import { getRuntimeEnv } from "../db";

type D1Like = {
  prepare: (query: string) => { all: <T>() => Promise<{ results?: T[] }> };
  exec: (query: string) => Promise<unknown>;
};

let ready: Promise<void> | null = null;

// Older deployments already have push_tokens without a provider column.
// Add it lazily so existing Expo registrations keep working during rollout.
export function ensurePushTokenProviderStorage() {
  if (ready) return ready;
  ready = (async () => {
    const database = getRuntimeEnv().DB as D1Like | undefined;
    if (!database) throw new Error("Database binding is unavailable.");
    const result = await database.prepare("PRAGMA table_info(push_tokens)").all<{ name: string }>();
    const columns = new Set((result.results || []).map((column) => column.name));
    if (!columns.has("delivery_binding")) {
      await database.exec("ALTER TABLE push_tokens ADD COLUMN delivery_binding TEXT;");
    }
    if (!columns.has("provider")) {
      await database.exec("ALTER TABLE push_tokens ADD COLUMN provider TEXT DEFAULT 'expo' NOT NULL;");
    }
  })().catch((error) => {
    ready = null;
    throw error;
  });
  return ready;
}
