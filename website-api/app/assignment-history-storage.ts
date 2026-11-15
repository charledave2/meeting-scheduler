import { getRuntimeEnv } from "../db";

let ready: Promise<void> | null = null;

export function ensureAssignmentHistoryStorage() {
  if (ready) return ready;
  ready = (async () => {
    const database = getRuntimeEnv().DB;
    if (!database) throw new Error("Database binding is unavailable.");
    await database.prepare(`
      CREATE TABLE IF NOT EXISTS assignment_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        schedule_id INTEGER NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
        member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
        meeting_date TEXT NOT NULL,
        part_id INTEGER NOT NULL,
        part_title TEXT NOT NULL,
        assignment_role TEXT NOT NULL,
        hall TEXT NOT NULL,
        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
      )
    `).run();
    const columns = await database.prepare("PRAGMA table_info(assignment_history)").all<{ name: string }>();
    const names = new Set((columns.results || []).map(column => column.name));
    const missing: Record<string,string> = {
      voided_at: "voided_at TEXT",
      voided_by: "voided_by INTEGER",
      void_reason: "void_reason TEXT DEFAULT '' NOT NULL",
    };
    for (const [name, definition] of Object.entries(missing)) {
      if (!names.has(name)) await database.prepare(`ALTER TABLE assignment_history ADD COLUMN ${definition}`).run();
    }
    await database.prepare("CREATE INDEX IF NOT EXISTS assignment_history_schedule_idx ON assignment_history(schedule_id)").run();
    await database.prepare("CREATE INDEX IF NOT EXISTS assignment_history_member_date_idx ON assignment_history(member_id, meeting_date)").run();
  })().catch(error => { ready = null; throw error; });
  return ready;
}
