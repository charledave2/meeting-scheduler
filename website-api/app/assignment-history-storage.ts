import { getRuntimeEnv } from "../db";

let ready: Promise<void> | null = null;

export function ensureAssignmentHistoryStorage() {
  if (ready) return ready;
  ready = (async () => {
    const database = getRuntimeEnv().DB;
    if (!database) throw new Error("Database binding is unavailable.");
    await database.exec(`
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
      );
      CREATE INDEX IF NOT EXISTS assignment_history_schedule_idx ON assignment_history(schedule_id);
      CREATE INDEX IF NOT EXISTS assignment_history_member_date_idx ON assignment_history(member_id, meeting_date);
    `);
    const columns = await database.prepare(`PRAGMA table_info(assignment_history)`).all<{ name: string }>();
    const names = new Set((columns.results || []).map((column: { name: string }) => column.name));
    if (!names.has("voided_at")) await database.exec(`ALTER TABLE assignment_history ADD COLUMN voided_at TEXT;`);
    if (!names.has("voided_by")) await database.exec(`ALTER TABLE assignment_history ADD COLUMN voided_by INTEGER;`);
    if (!names.has("void_reason")) await database.exec(`ALTER TABLE assignment_history ADD COLUMN void_reason TEXT DEFAULT '' NOT NULL;`);
  })().catch(error => { ready = null; throw error; });
  return ready;
}
