import { desc, eq } from "drizzle-orm";
import { getDb } from "../../../db";
import { authAccounts, authSessions, users } from "../../../db/schema";
import { assertSameOrigin, getChatGPTUser, hashPassword, validPassword } from "../../chatgpt-auth";

async function admin() {
  const user = await getChatGPTUser();
  return user?.role === "admin" ? user : null;
}

const assignableRoles = ["scheduler", "viewer"] as const;
type AssignableRole = (typeof assignableRoles)[number];

export async function GET() {
  if (!(await admin())) return Response.json({ error: "Admin only" }, { status: 403 });
  const accounts = await getDb()
    .select({
      id: authAccounts.id,
      displayName: authAccounts.displayName,
      username: authAccounts.username,
      status: authAccounts.status,
      createdAt: authAccounts.createdAt,
      requestNote: authAccounts.requestNote,
      role: users.role,
    })
    .from(authAccounts)
    .leftJoin(users, eq(authAccounts.userId, users.id))
    .orderBy(desc(authAccounts.createdAt));
  return Response.json({ accounts });
}

export async function PATCH(request: Request) {
  const current = await admin();
  if (!current) return Response.json({ error: "Admin only" }, { status: 403 });
  if (!assertSameOrigin(request)) return Response.json({ error: "Invalid request." }, { status: 403 });

  const body = (await request.json()) as {
    id?: number;
    action?: string;
    temporaryPassword?: string;
    role?: AssignableRole;
  };
  const allowedActions = ["approve", "reset", "disable", "enable", "decline", "change_role"];
  if (!body.id || !body.action || !allowedActions.includes(body.action)) {
    return Response.json({ error: "Invalid account action." }, { status: 400 });
  }

  const db = getDb();
  const [account] = await db.select().from(authAccounts).where(eq(authAccounts.id, body.id)).limit(1);
  if (!account) return Response.json({ error: "Account not found." }, { status: 404 });
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

  await db.delete(authSessions).where(eq(authSessions.accountId, account.id));
  await db.delete(authAccounts).where(eq(authAccounts.id, account.id));
  // Keep the linked user row so saved schedules retain their owner/audit identity.
  // Approving a future request with the same username reuses this user row.
  return Response.json({ deleted: true, message: `${account.displayName}'s login was permanently removed. Saved records were retained.` });
}
