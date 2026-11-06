import { and, eq } from "drizzle-orm";
import { getDb, getRuntimeEnv } from "../../../db";
import { assignmentHistory, members, participantImportAudits, schedules, users } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";

type Backup = {
  format?: string;
  version?: number;
  data?: {
    users?: Array<Record<string, unknown>>;
    members?: Array<Record<string, unknown>>;
    schedules?: Array<Record<string, unknown>>;
    assignmentHistory?: Array<Record<string, unknown>>;
    participantImportAudits?: Array<Record<string, unknown>>;
  };
};

async function requireAdmin() {
  const auth = await getChatGPTUser();
  if (!auth) return null;
  const db = getDb();
  let [user] = await db.select().from(users).where(eq(users.email, auth.email)).limit(1);
  const isAdmin = auth.email.toLowerCase() === (getRuntimeEnv().ADMIN_EMAIL || "").toLowerCase();
  if (!user) [user] = await db.insert(users).values({ email: auth.email, displayName: auth.displayName, role: isAdmin ? "admin" : "viewer" }).returning();
  else if (isAdmin && user.role !== "admin") [user] = await db.update(users).set({ role: "admin" }).where(eq(users.id, user.id)).returning();
  return user.role === "admin" ? user : null;
}

const value = (row: Record<string, unknown>, camel: string, snake: string) => row[camel] ?? row[snake];
const stringValue = (row: Record<string, unknown>, camel: string, snake: string, fallback = "") => {
  const result = value(row, camel, snake);
  return typeof result === "string" ? result : fallback;
};
const numberValue = (row: Record<string, unknown>, key: string) => typeof row[key] === "number" ? row[key] as number : Number(row[key]) || 0;

export async function POST(request: Request) {
  const admin = await requireAdmin();
  if (!admin) return Response.json({ error: "Admin only" }, { status: 403 });

  const declaredLength = Number(request.headers.get("content-length") || 0);
  if (declaredLength > 2_000_000) return Response.json({ error: "Backup file is too large." }, { status: 413 });
  const raw = await request.text();
  if (new TextEncoder().encode(raw).byteLength > 2_000_000) return Response.json({ error: "Backup file is too large." }, { status: 413 });
  let backup: Backup;
  try { backup = JSON.parse(raw) as Backup; }
  catch { return Response.json({ error: "Backup file is not valid JSON." }, { status: 400 }); }
  if (backup.format !== "meeting-scheduler-backup" || backup.version !== 1 || !backup.data) {
    return Response.json({ error: "This is not a valid Meeting Scheduler backup file." }, { status: 400 });
  }
  const rowLimits = { users: 500, members: 5000, schedules: 1000, assignmentHistory: 20000, participantImportAudits: 5000 } as const;
  for (const [key, limit] of Object.entries(rowLimits)) {
    const rows = backup.data[key as keyof typeof rowLimits];
    if (rows && (!Array.isArray(rows) || rows.length > limit)) return Response.json({ error: `Backup contains too many ${key} rows.` }, { status: 400 });
  }

  const db = getDb();
  const userIdMap = new Map<number, number>();
  const memberIdMap = new Map<number, number>();
  const scheduleIdMap = new Map<number, number>();
  let restoredUsers = 0;
  let restoredMembers = 0;
  let restoredSchedules = 0;
  let restoredAudits = 0;
  const adminEmail = (getRuntimeEnv().ADMIN_EMAIL || "").toLowerCase();

  for (const row of backup.data.users ?? []) {
    const email = stringValue(row, "email", "email").trim().toLowerCase();
    if (!email) continue;
    let [target] = await db.select().from(users).where(eq(users.email, email)).limit(1);
    const backedUpRole = stringValue(row, "role", "role");
    const role = email === adminEmail ? "admin" : backedUpRole === "scheduler" ? "scheduler" : "viewer";
    const displayName = stringValue(row, "displayName", "display_name", email);
    if (!target) {
      [target] = await db.insert(users).values({ email, displayName, role }).returning();
      restoredUsers++;
    } else if (target.displayName !== displayName || target.role !== role) {
      [target] = await db.update(users).set({ displayName, role }).where(eq(users.id, target.id)).returning();
      restoredUsers++;
    }
    const oldId = numberValue(row, "id");
    if (oldId) userIdMap.set(oldId, target.id);
  }

  for (const row of backup.data.members ?? []) {
    const name = stringValue(row, "name", "name").trim().replace(/\s+/g, " ");
    if (!name) continue;
    const allMembers = await db.select().from(members);
    const target = allMembers.find((member) => member.name.toLocaleLowerCase() === name.toLocaleLowerCase());
    const appointment = ["elder", "ministerial", "publisher"].includes(stringValue(row, "appointment", "appointment")) ? stringValue(row, "appointment", "appointment") as "elder" | "ministerial" | "publisher" : "publisher";
    const serviceStatus = stringValue(row, "serviceStatus", "service_status") === "regular_pioneer" ? "regular_pioneer" as const : "none" as const;
    const rawGender = stringValue(row, "gender", "gender");
    const gender = ["brother", "sister", "unspecified"].includes(rawGender) ? rawGender as "brother" | "sister" | "unspecified" : "unspecified";
    const unbaptized = value(row, "unbaptized", "unbaptized") === true || value(row, "unbaptized", "unbaptized") === 1;
    const createdBy = userIdMap.get(numberValue(row, "createdBy") || numberValue(row, "created_by")) ?? admin.id;
    const active = value(row, "active", "active") !== false && value(row, "active", "active") !== 0;
    let targetId:number;
    if (target) { await db.update(members).set({ name, appointment, serviceStatus, gender, active, unbaptized }).where(eq(members.id, target.id)); targetId=target.id; }
    else { const [created]=await db.insert(members).values({ name, appointment, serviceStatus, gender, active, unbaptized, createdBy }).returning({id:members.id}); targetId=created.id; }
    const oldId=numberValue(row,"id");if(oldId)memberIdMap.set(oldId,targetId);
    restoredMembers++;
  }

  for (const row of backup.data.schedules ?? []) {
    const title = stringValue(row, "title", "title");
    const updatedAt = stringValue(row, "updatedAt", "updated_at");
    if (!title) continue;
    const [existing] = await db.select({ id: schedules.id }).from(schedules).where(and(eq(schedules.title, title), eq(schedules.updatedAt, updatedAt))).limit(1);
    if (existing) { const oldId=numberValue(row,"id");if(oldId)scheduleIdMap.set(oldId,existing.id);continue; }
    const ownerId = userIdMap.get(numberValue(row, "ownerId") || numberValue(row, "owner_id")) ?? admin.id;
    const [createdSchedule]=await db.insert(schedules).values({
      title,
      weekLabel: stringValue(row, "weekLabel", "week_label"),
      bibleReading: stringValue(row, "bibleReading", "bible_reading"),
      sourceUrl: stringValue(row, "sourceUrl", "source_url"),
      detailsJson: stringValue(row, "detailsJson", "details_json", "{}"),
      assignmentsJson: stringValue(row, "assignmentsJson", "assignments_json", "{}"),
      ownerId,
      createdAt: stringValue(row, "createdAt", "created_at", new Date().toISOString()),
      updatedAt: updatedAt || new Date().toISOString(),
    }).returning({id:schedules.id});
    const oldId=numberValue(row,"id");if(oldId)scheduleIdMap.set(oldId,createdSchedule.id);
    restoredSchedules++;
  }

  for(const row of backup.data.assignmentHistory??[]){const scheduleId=scheduleIdMap.get(numberValue(row,"scheduleId")||numberValue(row,"schedule_id"));const memberId=memberIdMap.get(numberValue(row,"memberId")||numberValue(row,"member_id"));if(!scheduleId||!memberId)continue;const partId=numberValue(row,"partId")||numberValue(row,"part_id");const assignmentRole=stringValue(row,"assignmentRole","assignment_role")==="assistant"?"assistant" as const:"main" as const;const hall=stringValue(row,"hall","hall")==="second"?"second" as const:"main" as const;const [existing]=await db.select({id:assignmentHistory.id}).from(assignmentHistory).where(and(eq(assignmentHistory.scheduleId,scheduleId),eq(assignmentHistory.memberId,memberId),eq(assignmentHistory.partId,partId),eq(assignmentHistory.assignmentRole,assignmentRole),eq(assignmentHistory.hall,hall))).limit(1);if(!existing)await db.insert(assignmentHistory).values({scheduleId,memberId,meetingDate:stringValue(row,"meetingDate","meeting_date"),partId,partTitle:stringValue(row,"partTitle","part_title","Meeting part"),assignmentRole,hall});}

  for (const row of backup.data.participantImportAudits ?? []) {
    const sourceUrl = stringValue(row, "sourceUrl", "source_url");
    const createdAt = stringValue(row, "createdAt", "created_at");
    const [existing] = await db.select({ id: participantImportAudits.id }).from(participantImportAudits).where(and(eq(participantImportAudits.sourceUrl, sourceUrl), eq(participantImportAudits.createdAt, createdAt))).limit(1);
    if (existing) continue;
    await db.insert(participantImportAudits).values({
      sourceUrl,
      sheetTitle: stringValue(row, "sheetTitle", "sheet_title"),
      tabName: stringValue(row, "tabName", "tab_name"),
      addedCount: numberValue(row, "addedCount") || numberValue(row, "added_count"),
      updatedCount: numberValue(row, "updatedCount") || numberValue(row, "updated_count"),
      unchangedCount: numberValue(row, "unchangedCount") || numberValue(row, "unchanged_count"),
      skippedCount: numberValue(row, "skippedCount") || numberValue(row, "skipped_count"),
      detailsJson: stringValue(row, "detailsJson", "details_json", "{}"),
      importedBy: userIdMap.get(numberValue(row, "importedBy") || numberValue(row, "imported_by")) ?? admin.id,
      createdAt: createdAt || new Date().toISOString(),
    });
    restoredAudits++;
  }

  return Response.json({ summary: { users: restoredUsers, members: restoredMembers, schedules: restoredSchedules, audits: restoredAudits } });
}
