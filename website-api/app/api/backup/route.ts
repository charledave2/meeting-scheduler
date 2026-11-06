import { eq } from "drizzle-orm";
import { getDb, getRuntimeEnv } from "../../../db";
import { assignmentHistory, members, participantImportAudits, schedules, users } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";

async function requireAdmin() {
  const auth = await getChatGPTUser();
  if (!auth) return null;

  const db = getDb();
  let [user] = await db.select().from(users).where(eq(users.email, auth.email)).limit(1);
  const adminEmail = (getRuntimeEnv().ADMIN_EMAIL || "").toLowerCase();
  const isAdmin = auth.email.toLowerCase() === adminEmail;

  if (!user) {
    [user] = await db.insert(users).values({
      email: auth.email,
      displayName: auth.displayName,
      role: isAdmin ? "admin" : "viewer",
    }).returning();
  } else if (isAdmin && user.role !== "admin") {
    [user] = await db.update(users).set({ role: "admin" }).where(eq(users.id, user.id)).returning();
  }

  return user.role === "admin" ? user : null;
}

export async function GET() {
  if (!await requireAdmin()) {
    return Response.json({ error: "Admin only" }, { status: 403 });
  }

  const db = getDb();
  const [userRows, memberRows, scheduleRows, assignmentRows, importAuditRows] = await Promise.all([
    db.select().from(users),
    db.select().from(members),
    db.select().from(schedules),
    db.select().from(assignmentHistory),
    db.select().from(participantImportAudits),
  ]);
  const exportedAt = new Date().toISOString();
  const filename = `meeting-scheduler-backup-${exportedAt.slice(0, 10)}.json`;

  return new Response(JSON.stringify({
    format: "meeting-scheduler-backup",
    version: 1,
    exportedAt,
    data: { users: userRows, members: memberRows, schedules: scheduleRows, assignmentHistory: assignmentRows, participantImportAudits: importAuditRows },
  }, null, 2), {
    headers: {
      "content-type": "application/json; charset=utf-8",
      "content-disposition": `attachment; filename="${filename}"`,
      "cache-control": "no-store",
    },
  });
}
