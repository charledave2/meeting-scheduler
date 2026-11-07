import { eq } from "drizzle-orm";
import { getDb, getRuntimeEnv } from "../db";
import { appSettings } from "../db/schema";

export type Role = "admin" | "scheduler" | "viewer";
export type ToolAccess = "hidden" | "view" | "edit";
export const TOOL_ACCESS_KEYS = ["home", "workbook", "assignments", "preview", "participants", "history"] as const;
export type ToolAccessKey = (typeof TOOL_ACCESS_KEYS)[number];

type MinimalUser = { role: Role; accountId?: number };

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

async function ensureToolAccessTable() {
  const database = getRuntimeEnv().DB;
  if (!database) throw new Error("Cloudflare D1 binding DB is unavailable.");
  await database.prepare("CREATE TABLE IF NOT EXISTS account_tool_access (account_id INTEGER PRIMARY KEY NOT NULL, access_json TEXT NOT NULL DEFAULT '{}', updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)").run();
  return database;
}

function defaultsFor(role: Role): Record<ToolAccessKey, ToolAccess> {
  if (role === "admin") return { home: "edit", workbook: "edit", assignments: "edit", preview: "edit", participants: "edit", history: "edit" };
  if (role === "scheduler") return { home: "view", workbook: "edit", assignments: "edit", preview: "edit", participants: "hidden", history: "view" };
  return { home: "view", workbook: "hidden", assignments: "hidden", preview: "hidden", participants: "hidden", history: "hidden" };
}

function validMode(value: unknown): value is ToolAccess {
  return value === "hidden" || value === "view" || value === "edit";
}

export async function getToolAccessMap(user: MinimalUser | null | undefined): Promise<Record<ToolAccessKey, ToolAccess>> {
  if (!user) return defaultsFor("viewer");
  const defaults = defaultsFor(user.role);
  if (user.role === "admin" || !user.accountId) return defaults;

  const database = await ensureToolAccessTable();
  const row = await database.prepare("SELECT access_json FROM account_tool_access WHERE account_id = ?").bind(user.accountId).first<{ access_json: string }>();
  let overrides: Record<string, unknown> = {};
  try { overrides = row?.access_json ? JSON.parse(row.access_json) as Record<string, unknown> : {}; } catch { overrides = {}; }

  // Preserve the existing congregation-wide Scheduler participant edit switch
  // unless this particular account has an explicit Participants override.
  if (user.role === "scheduler" && !validMode(overrides.participants)) {
    try { if ((await getAppSettings()).schedulerCanEditParticipants) defaults.participants = "edit"; } catch {}
  }

  const result = { ...defaults };
  for (const key of TOOL_ACCESS_KEYS) {
    if (validMode(overrides[key])) result[key] = overrides[key];
  }
  // Viewer accounts can be granted visibility, but their role remains read-only.
  if (user.role === "viewer") for (const key of TOOL_ACCESS_KEYS) if (result[key] === "edit") result[key] = "view";
  return result;
}

export async function saveToolAccessMap(accountId: number, input: unknown) {
  if (!input || typeof input !== "object" || Array.isArray(input)) throw new Error("Invalid tool access settings.");
  const source = input as Record<string, unknown>;
  const clean: Partial<Record<ToolAccessKey, ToolAccess>> = {};
  for (const key of TOOL_ACCESS_KEYS) {
    if (!validMode(source[key])) throw new Error("Choose Hidden, View only, or Edit for each tool.");
    clean[key] = source[key];
  }
  const database = await ensureToolAccessTable();
  await database.prepare("INSERT INTO account_tool_access (account_id, access_json, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT(account_id) DO UPDATE SET access_json = excluded.access_json, updated_at = CURRENT_TIMESTAMP")
    .bind(accountId, JSON.stringify(clean)).run();
  return clean as Record<ToolAccessKey, ToolAccess>;
}

export async function canAccessTool(user: MinimalUser | null | undefined, tool: ToolAccessKey, required: "view" | "edit" = "view") {
  if (!user) return false;
  if (user.role === "admin") return true;
  const mode = (await getToolAccessMap(user))[tool];
  return required === "view" ? mode !== "hidden" : mode === "edit";
}

export async function canAccessAnyTool(user: MinimalUser | null | undefined, tools: ToolAccessKey[], required: "view" | "edit" = "view") {
  for (const tool of tools) if (await canAccessTool(user, tool, required)) return true;
  return false;
}

// Participant mutations follow the per-account permission set. Older API
// callers without an account id retain the previous global setting behavior.
export async function canManageParticipants(user: MinimalUser | null | undefined): Promise<boolean> {
  if (!user) return false;
  if (user.role === "admin") return true;
  if (user.role !== "scheduler") return false;
  if (user.accountId) return canAccessTool(user, "participants", "edit");
  try { return (await getAppSettings()).schedulerCanEditParticipants; }
  catch (error) { console.error("Unable to verify Scheduler participant permission", error); return false; }
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
