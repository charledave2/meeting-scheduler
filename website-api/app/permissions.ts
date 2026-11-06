import { eq } from "drizzle-orm";
import { getDb } from "../db";
import { appSettings } from "../db/schema";

export type Role = "admin" | "scheduler" | "viewer";

type MinimalUser = { role: Role };

export function isAdmin(user: MinimalUser | null | undefined) {
  return user?.role === "admin";
}

// Cached per-request-ish (module scope; Workers isolate is short-lived per
// request anyway) fetch of the single app_settings row, creating it with
// defaults if it doesn't exist yet.
export async function getAppSettings() {
  const db = getDb();
  const [row] = await db.select().from(appSettings).limit(1);
  if (row) return row;
  const [created] = await db.insert(appSettings).values({}).returning();
  return created;
}

// Admin can always manage participants. Scheduler can only if the admin has
// turned on the "scheduler can edit participants" setting (off by default).
// Viewer never can.
export async function canManageParticipants(user: MinimalUser | null | undefined): Promise<boolean> {
  if (!user) return false;
  if (user.role === "admin") return true;
  if (user.role !== "scheduler") return false;
  try {
    const settings = await getAppSettings();
    return settings.schedulerCanEditParticipants;
  } catch (error) {
    console.error("Unable to verify Scheduler participant permission", error);
    return false;
  }
}

export async function updateAppSettings(patch: Partial<{ schedulerCanEditParticipants: boolean }>) {
  const db = getDb();
  const current = await getAppSettings();
  const [updated] = await db
    .update(appSettings)
    .set({ ...patch, updatedAt: new Date().toISOString() })
    .where(eq(appSettings.id, current.id))
    .returning();
  return updated;
}
