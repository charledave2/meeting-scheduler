import { asc, desc, eq } from "drizzle-orm";
import { getDb, getRuntimeEnv } from "../../../db";
import { authAccounts, authSessions, members, users } from "../../../db/schema";
import { ensureAccountMemberStorage } from "../../account-member-storage";
import { assertSameOrigin, getChatGPTUser, hashPassword, validPassword } from "../../chatgpt-auth";
import { getToolAccessMap, saveToolAccessMap } from "../../permissions";

async function admin() {
  const user = await getChatGPTUser();
  return user?.role === "admin" ? user : null;
}

const assignableRoles = ["scheduler", "viewer"] as const;
type AssignableRole = (typeof assignableRoles)[number];

async function ensureAccountPhotosTable() {
  const database = getRuntimeEnv().DB;
  if (!database) throw new Error("Cloudflare D1 binding DB is unavailable.");
  await database.prepare("CREATE TABLE IF NOT EXISTS account_profile_photos (account_id INTEGER PRIMARY KEY NOT NULL, photo_data_url TEXT NOT NULL, updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)").run();
  return database;
}

export async function GET() {
  if (!(await admin())) return Response.json({ error: "Admin only" }, { status: 403 });
  await ensureAccountMemberStorage();
  const accounts = await getDb()
    .select({
      id: authAccounts.id,
      displayName: authAccounts.displayName,
      username: authAccounts.username,
      status: authAccounts.status,
      createdAt: authAccounts.createdAt,
      requestNote: authAccounts.requestNote,
      role: users.role,
      memberId: authAccounts.memberId,
      memberName: members.name,
    })
    .from(authAccounts)
    .leftJoin(users, eq(authAccounts.userId, users.id))
    .leftJoin(members, eq(authAccounts.memberId, members.id))
    .orderBy(desc(authAccounts.createdAt));
  const photoDb = await ensureAccountPhotosTable();
  const photoRows = await photoDb.prepare("SELECT account_id, photo_data_url FROM account_profile_photos").all<{ account_id: number; photo_data_url: string }>();
  const photos = new Map((photoRows.results ?? []).map((row) => [row.account_id, row.photo_data_url]));
  const enriched = await Promise.all(accounts.map(async (account) => ({
    ...account,
    photoDataUrl: photos.get(account.id) ?? "",
    toolAccess: await getToolAccessMap({ accountId: account.id, role: account.role === "admin" ? "admin" : account.role === "scheduler" ? "scheduler" : "viewer" }),
  })));
  const activeMembers = await getDb().select({ id: members.id, name: members.name }).from(members).where(eq(members.active, true)).orderBy(asc(members.name));
  return Response.json({ accounts: enriched, activeMembers });
}

export async function PATCH(request: Request) {
  const current = await admin();
  if (!current) return Response.json({ error: "Admin only" }, { status: 403 });
  if (!assertSameOrigin(request)) return Response.json({ error: "Invalid request." }, { status: 403 });

  await ensureAccountMemberStorage();
  const body = (await request.json()) as {
    id?: number;
    memberId?: number | null;
    action?: string;
    temporaryPassword?: string;
    role?: AssignableRole;
    photoDataUrl?: string;
    toolAccess?: Record<string, unknown>;
  };
  const allowedActions = ["approve", "reset", "disable", "enable", "decline", "change_role", "upload_photo", "update_tool_access", "link_member"];
  if (!body.id || !body.action || !allowedActions.includes(body.action)) {
    return Response.json({ error: "Invalid account action." }, { status: 400 });
  }

  const db = getDb();
  const [account] = await db.select().from(authAccounts).where(eq(authAccounts.id, body.id)).limit(1);
  if (!account) return Response.json({ error: "Account not found." }, { status: 404 });
  if (body.action === "link_member") {
    if (!account.userId || account.status === "requested") return Response.json({ error: "Approve this account before linking a participant." }, { status: 400 });
    if (body.memberId == null) {
      await db.update(authAccounts).set({ memberId: null }).where(eq(authAccounts.id, account.id));
      return Response.json({ message: "Participant link removed." });
    }
    if (!Number.isInteger(body.memberId) || body.memberId < 1) return Response.json({ error: "Choose a valid participant." }, { status: 400 });
    const [member] = await db.select({ id: members.id, active: members.active }).from(members).where(eq(members.id, body.memberId)).limit(1);
    if (!member || !member.active) return Response.json({ error: "Choose an active participant." }, { status: 400 });
    try {
      await db.update(authAccounts).set({ memberId: member.id }).where(eq(authAccounts.id, account.id));
    } catch (error) {
      const detail = error instanceof Error ? error.message : "";
      if (/unique|constraint/i.test(detail)) return Response.json({ error: "This participant is already linked to another account." }, { status: 409 });
      throw error;
    }
    return Response.json({ message: "Participant linked to account." });
  }
  if (body.action === "update_tool_access") {
    if (!account.userId) return Response.json({ error: "Only approved accounts can have tool access customized." }, { status: 400 });
    const [target] = await db.select({ role: users.role }).from(users).where(eq(users.id, account.userId)).limit(1);
    if (target?.role === "admin") return Response.json({ error: "Administrator access is fixed." }, { status: 400 });
    try {
      const toolAccess = await saveToolAccessMap(account.id, body.toolAccess);
      return Response.json({ message: "Tool access updated.", toolAccess });
    } catch (error) {
      return Response.json({ error: error instanceof Error ? error.message : "Invalid tool access settings." }, { status: 400 });
    }
  }
  if (body.action === "upload_photo") {
    const photoDataUrl = body.photoDataUrl || "";
    if (photoDataUrl.length > 600_000 || !/^data:image\/jpeg;base64,[A-Za-z0-9+/]+={0,2}$/.test(photoDataUrl)) {
      return Response.json({ error: "Choose a valid JPEG image under the size limit." }, { status: 400 });
    }
    const photoDb = await ensureAccountPhotosTable();
    await photoDb.prepare("INSERT INTO account_profile_photos (account_id, photo_data_url, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT(account_id) DO UPDATE SET photo_data_url = excluded.photo_data_url, updated_at = CURRENT_TIMESTAMP")
      .bind(account.id, photoDataUrl).run();
    return Response.json({ message: "Account photo saved to cloud." });
  }

  if ((body.action === "approve" || body.action === "reset") && !validPassword(body.temporaryPassword || "")) {
    return Response.json({ error: "Temporary password must have at least 10 characters." }, { status: 400 });
  }

  if (body.action === "approve") {
    if (account.status !== "requested") return Response.json({ error: "This request is no longer pending." }, { status: 409 });
    const role: AssignableRole = assignableRoles.includes(body.role as AssignableRole) ? (body.role as AssignableRole) : "viewer";
    const credentials = await hashPassword(body.temporaryPassword || "");
    const email = `${account.username}@local.invalid`;
    const [existingUser] = await db.select().from(users).where(eq(users.email, email)).limit(1);
    const [user] = existingUser
      ? await db.update(users).set({ displayName: account.displayName, role }).where(eq(users.id, existingUser.id)).returning()
      : await db.insert(users).values({ email, displayName: account.displayName, role }).returning();
    await db.update(authAccounts).set({
      userId: user.id,
      passwordHash: credentials.hash,
      passwordSalt: credentials.salt,
      passwordIterations: credentials.iterations,
      status: "active",
      mustChangePassword: true,
      approvedAt: new Date().toISOString(),
    }).where(eq(authAccounts.id, account.id));
    return Response.json({ message: `${account.displayName} approved as ${role}. Give them the temporary password securely.` });
  }

  if (body.action === "decline") {
    if (account.status !== "requested") return Response.json({ error: "This request is no longer pending." }, { status: 409 });
    const photoDb = await ensureAccountPhotosTable();
    await photoDb.prepare("DELETE FROM account_profile_photos WHERE account_id = ?").bind(account.id).run();
    await db.delete(authAccounts).where(eq(authAccounts.id, account.id));
    return Response.json({ message: `${account.displayName}'s request was declined and removed.`, removed: true });
  }

  if (body.action === "change_role") {
    if (!account.userId) return Response.json({ error: "This account has no linked role yet." }, { status: 400 });
    const [targetUser] = await db.select().from(users).where(eq(users.id, account.userId)).limit(1);
    if (!targetUser) return Response.json({ error: "Linked user not found." }, { status: 404 });
    if (targetUser.role === "admin") return Response.json({ error: "The admin role cannot be changed." }, { status: 400 });
    if (!assignableRoles.includes(body.role as AssignableRole)) return Response.json({ error: "Choose Scheduler or Viewer." }, { status: 400 });
    await db.update(users).set({ role: body.role as AssignableRole }).where(eq(users.id, targetUser.id));
    await db.delete(authSessions).where(eq(authSessions.accountId, account.id));
    return Response.json({ message: `${account.displayName}'s role changed to ${body.role}. Sign in again to continue.` });
  }

  if (body.action === "reset") {
    const credentials = await hashPassword(body.temporaryPassword || "");
    await db.delete(authSessions).where(eq(authSessions.accountId, account.id));
    await db.update(authAccounts).set({
      passwordHash: credentials.hash,
      passwordSalt: credentials.salt,
      passwordIterations: credentials.iterations,
      mustChangePassword: true,
      failedAttempts: 0,
      lockedUntil: null,
    }).where(eq(authAccounts.id, account.id));
    return Response.json({ message: `Temporary password set for ${account.displayName}.` });
  }

  if (body.action === "disable") {
    if (account.id === current.accountId) return Response.json({ error: "You cannot disable your own admin account." }, { status: 400 });
    await db.delete(authSessions).where(eq(authSessions.accountId, account.id));
    await db.update(authAccounts).set({ status: "disabled" }).where(eq(authAccounts.id, account.id));
    return Response.json({ message: `${account.displayName} disabled.` });
  }

  if (body.action === "enable") {
    await db.update(authAccounts).set({ status: "active" }).where(eq(authAccounts.id, account.id));
    return Response.json({ message: `${account.displayName} enabled.` });
  }

  return Response.json({ error: "Invalid account action." }, { status: 400 });
}

export async function DELETE(request: Request) {
  const current = await admin();
  if (!current) return Response.json({ error: "Admin only" }, { status: 403 });
  if (!assertSameOrigin(request)) return Response.json({ error: "Invalid request." }, { status: 403 });

  await ensureAccountMemberStorage();
  const id = Number(new URL(request.url).searchParams.get("id"));
  if (!Number.isInteger(id) || id < 1) return Response.json({ error: "Invalid account." }, { status: 400 });

  const db = getDb();
  const [account] = await db.select().from(authAccounts).where(eq(authAccounts.id, id)).limit(1);
  if (!account) return Response.json({ error: "Account not found." }, { status: 404 });
  if (account.id === current.accountId || account.userId === current.userId) {
    return Response.json({ error: "You cannot delete your own admin account." }, { status: 400 });
  }
  if (account.userId) {
    const [targetUser] = await db.select({ role: users.role }).from(users).where(eq(users.id, account.userId)).limit(1);
    if (targetUser?.role === "admin") return Response.json({ error: "Admin accounts cannot be deleted here." }, { status: 400 });
  }

  const photoDb = await ensureAccountPhotosTable();
  await photoDb.prepare("DELETE FROM account_profile_photos WHERE account_id = ?").bind(account.id).run();
  await db.delete(authSessions).where(eq(authSessions.accountId, account.id));
  await db.delete(authAccounts).where(eq(authAccounts.id, account.id));
  // Keep the linked user row so saved schedules retain their owner/audit identity.
  // Approving a future request with the same username reuses this user row.
  return Response.json({ deleted: true, message: `${account.displayName}'s login was permanently removed. Saved records were retained.` });
}
