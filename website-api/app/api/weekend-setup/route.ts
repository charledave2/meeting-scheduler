import { and, asc, eq } from "drizzle-orm";
import { getDb } from "../../../db";
import { hostGroups, members, weekendEligibility } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";

export async function GET() {
  const user = await getChatGPTUser(); if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const db = getDb();
  const [memberRows, eligibility, groups] = await Promise.all([
    db.select({ id: members.id, name: members.name, appointment: members.appointment }).from(members).where(eq(members.active, true)).orderBy(asc(members.name)),
    db.select().from(weekendEligibility),
    db.select().from(hostGroups).where(eq(hostGroups.active, true)).orderBy(asc(hostGroups.name)),
  ]);
  return Response.json({ members: memberRows, eligibility, groups }, { headers: { "cache-control": "no-store" } });
}

export async function PATCH(request: Request) {
  const user = await getChatGPTUser(); if (user?.role !== "admin") return Response.json({ error: "Admin only" }, { status: 403 });
  const body = await request.json() as { memberId?: number; quarry?: "A" | "B"; duty?: "chairman" | "reader"; eligible?: boolean };
  if (!Number.isInteger(body.memberId) || !["A", "B"].includes(body.quarry || "") || !["chairman", "reader"].includes(body.duty || "") || typeof body.eligible !== "boolean") return Response.json({ error: "Invalid eligibility change." }, { status: 400 });
  const db = getDb();
  const conditions = [eq(weekendEligibility.memberId, body.memberId!), eq(weekendEligibility.quarry, body.quarry!), eq(weekendEligibility.duty, body.duty!)];
  if (body.eligible) await db.insert(weekendEligibility).values({ memberId: body.memberId!, quarry: body.quarry!, duty: body.duty! }).onConflictDoNothing();
  else await db.delete(weekendEligibility).where(and(...conditions));
  return Response.json({ ok: true });
}

export async function POST(request: Request) {
  const user = await getChatGPTUser(); if (user?.role !== "admin") return Response.json({ error: "Admin only" }, { status: 403 });
  const name = ((await request.json()) as { name?: string }).name?.trim().replace(/\s+/g, " ");
  if (!name || name.length > 40) return Response.json({ error: "Enter a valid Group name." }, { status: 400 });
  const [group] = await getDb().insert(hostGroups).values({ name }).onConflictDoNothing().returning();
  return Response.json({ group }, { status: 201 });
}
