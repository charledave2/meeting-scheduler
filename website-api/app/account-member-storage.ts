import { getRuntimeEnv } from "../db";

type D1Like = {
  prepare: (query: string) => { all: <T>() => Promise<{ results?: T[] }> };
  exec: (query: string) => Promise<unknown>;
};

let ready: Promise<void> | null = null;

export function ensureAccountMemberStorage() {
  if (ready) return ready;
  ready = (async () => {
    const database = getRuntimeEnv().DB as D1Like | undefined;
    if (!database) throw new Error("Database binding is unavailable.");
    const result = await database.prepare("PRAGMA table_info(auth_accounts)").all<{ name: string }>();
    const columns = new Set((result.results || []).map((column) => column.name));
    if (!columns.has("member_id")) {
      await database.exec("ALTER TABLE auth_accounts ADD COLUMN member_id INTEGER REFERENCES members(id) ON DELETE SET NULL;");
    }
    if (!columns.has("last_login_at")) {
      await database.exec("ALTER TABLE auth_accounts ADD COLUMN last_login_at TEXT;");
    }
    await database.exec("CREATE UNIQUE INDEX IF NOT EXISTS auth_accounts_member_id_unique ON auth_accounts(member_id);");
  })().catch((error) => { ready = null; throw error; });
  return ready;
}
