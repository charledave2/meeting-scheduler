import { getRuntimeEnv } from "../db";

type D1Like = {
  prepare: (query: string) => { all: <T>() => Promise<{ results?: T[] }> };
  exec: (query: string) => Promise<unknown>;
};

let ready: Promise<void> | null = null;

async function ensureColumns(database: D1Like, table: string, columns: Record<string, string>) {
  const result = await database.prepare(`PRAGMA table_info(${table})`).all<{ name: string }>();
  const names = new Set((result.results || []).map((column: { name: string }) => column.name));
  for (const [name, definition] of Object.entries(columns)) {
    if (!names.has(name)) await database.exec(`ALTER TABLE ${table} ADD COLUMN ${definition};`);
  }
}

export function ensureScheduleSoftDeleteStorage() {
  if (ready) return ready;
  ready = (async () => {
    const database = getRuntimeEnv().DB as D1Like | undefined;
    if (!database) throw new Error("Database binding is unavailable.");
    await ensureColumns(database, "schedules", {
      deleted_at: "deleted_at TEXT",
      deleted_by: "deleted_by INTEGER",
      delete_reason: "delete_reason TEXT DEFAULT '' NOT NULL",
    });
    await ensureColumns(database, "weekend_schedules", {
      deleted_at: "deleted_at TEXT",
      deleted_by: "deleted_by INTEGER",
      delete_reason: "delete_reason TEXT DEFAULT '' NOT NULL",
    });
  })().catch(error => { ready = null; throw error; });
  return ready;
}
