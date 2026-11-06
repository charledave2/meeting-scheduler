import { and, asc, desc, eq, isNull } from "drizzle-orm";
import { getDb } from "../../../db";
import { weekendSchedules } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";
import { ensureScheduleSoftDeleteStorage } from "../../schedule-soft-delete-storage";

type Quarry = "A" | "B";
type WeekendMeeting = { date: string; quarry: Quarry; chairmanId: number | null; readerId: number | null; hostGroupIds: number[]; note?: string; cancelled?: boolean };
const monthPattern = /^\d{4}-(0[1-9]|1[0-2])$/;

function validMeetings(value: unknown): value is WeekendMeeting[] {
  return Array.isArray(value) && value.length <= 12 && value.every((row) => {
    if (!row || typeof row !== "object") return false;
    const item = row as WeekendMeeting;
    return /^\d{4}-\d{2}-\d{2}$/.test(item.date) && ["A", "B"].includes(item.quarry) &&
      (item.chairmanId === null || Number.isInteger(item.chairmanId)) &&
      (item.readerId === null || Number.isInteger(item.readerId)) &&
      Array.isArray(item.hostGroupIds) && item.hostGroupIds.length <= 2 && new Set(item.hostGroupIds).size === item.hostGroupIds.length && item.hostGroupIds.every(Number.isInteger);
  });
}

export async function GET(request: Request) {
  await ensureScheduleSoftDeleteStorage();
  const user = await getChatGPTUser(); if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const id = Number(new URL(request.url).searchParams.get("id"));
  if (id) {
    const [row] = await getDb().select().from(weekendSchedules).where(and(eq(weekendSchedules.id, id), isNull(weekendSchedules.deletedAt))).limit(1);
    if (!row || (user.role === "viewer" && row.status !== "published")) return Response.json({ error: "Schedule not found." }, { status: 404 });
    return Response.json({ schedule: { ...row, meetings: JSON.parse(row.assignmentsJson) } }, { headers: { "cache-control": "no-store" } });
  }
  const rows = await getDb().select({ id: weekendSchedules.id, monthKey: weekendSchedules.monthKey, monthLabel: weekendSchedules.monthLabel, status: weekendSchedules.status, updatedAt: weekendSchedules.updatedAt, assignmentsJson: weekendSchedules.assignmentsJson }).from(weekendSchedules).where(isNull(weekendSchedules.deletedAt)).orderBy(desc(weekendSchedules.monthKey), asc(weekendSchedules.id));
  return Response.json({ schedules: rows.filter(row => user.role !== "viewer" || row.status === "published").map(row => ({ ...row, meetings: JSON.parse(row.assignmentsJson), assignmentsJson: undefined })) }, { headers: { "cache-control": "no-store" } });
}

export async function POST(request: Request) {
  await ensureScheduleSoftDeleteStorage();
  const user = await getChatGPTUser(); if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  if (user.role === "viewer") return Response.json({ error: "Viewer accounts cannot edit weekend schedules." }, { status: 403 });
  const body = await request.json() as { id?: number; monthKey?: string; monthLabel?: string; status?: "draft" | "published"; meetings?: unknown };
  if (!body.monthKey || !monthPattern.test(body.monthKey) || !body.monthLabel?.trim() || !validMeetings(body.meetings)) return Response.json({ error: "Invalid monthly schedule." }, { status: 400 });
  const status: "draft" | "published" = body.status === "published" ? "published" : "draft";
  const values = { monthKey: body.monthKey, monthLabel: body.monthLabel.trim().slice(0, 40), status, assignmentsJson: JSON.stringify(body.meetings), ownerId: user.userId, updatedAt: new Date().toISOString() };
  const db = getDb(); let id = Number(body.id) || 0;
  if (!id) { const [existing] = await db.select({ id: weekendSchedules.id }).from(weekendSchedules).where(and(eq(weekendSchedules.monthKey, body.monthKey), isNull(weekendSchedules.deletedAt))).limit(1); id = existing?.id || 0; }
  if (id) await db.update(weekendSchedules).set(values).where(eq(weekendSchedules.id, id));
  else { const [created] = await db.insert(weekendSchedules).values(values).returning({ id: weekendSchedules.id }); id = created.id; }
  return Response.json({ schedule: { id, status } }, { status: body.id ? 200 : 201 });
}

export async function DELETE(request: Request) {
  await ensureScheduleSoftDeleteStorage();
  const user = await getChatGPTUser(); if (user?.role !== "admin") return Response.json({ error: "Admin only" }, { status: 403 });
  const url = new URL(request.url);
  const id = Number(url.searchParams.get("id")); if (!id) return Response.json({ error: "Invalid schedule." }, { status: 400 });
  const reason = (url.searchParams.get("reason") || "Deleted by admin").trim().slice(0, 180);
  const deletedAt = new Date().toISOString();
  const [row] = await getDb().select({ id: weekendSchedules.id }).from(weekendSchedules).where(and(eq(weekendSchedules.id, id), isNull(weekendSchedules.deletedAt))).limit(1);
  if (!row) return Response.json({ error: "Schedule not found." }, { status: 404 });
  await getDb().update(weekendSchedules).set({ deletedAt, deletedBy: user.userId, deleteReason: reason, updatedAt: deletedAt }).where(eq(weekendSchedules.id, id));
  return Response.json({ deleted: true, archived: true });
}
