import { getRuntimeEnv } from "../../../../db";
import { assertSameOrigin, getChatGPTUser } from "../../../chatgpt-auth";

async function ensureProfilePhotosTable() {
  const database = getRuntimeEnv().DB;
  if (!database) throw new Error("Cloudflare D1 binding DB is unavailable.");
  await database.prepare("CREATE TABLE IF NOT EXISTS account_profile_photos (account_id INTEGER PRIMARY KEY NOT NULL, photo_data_url TEXT NOT NULL, updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)").run();
  return database;
}

// Mobile clients use this route to fetch the signed-in user's cloud profile photo.
export async function GET() {
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const database = await ensureProfilePhotosTable();
  const row = await database.prepare("SELECT photo_data_url FROM account_profile_photos WHERE account_id = ?")
    .bind(user.accountId).first<{ photo_data_url: string }>();
  return Response.json({
    user: {
      displayName: user.displayName,
      username: user.username,
      role: user.role,
      mustChangePassword: user.mustChangePassword,
      photoDataUrl: row?.photo_data_url ?? "",
    },
  });
}

export async function PATCH(request: Request) {
  if (!assertSameOrigin(request)) return Response.json({ error: "Invalid request." }, { status: 403 });
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const body = await request.json() as { photoDataUrl?: string };
  const photoDataUrl = body.photoDataUrl || "";
  if (photoDataUrl.length > 600_000 || !/^data:image\/jpeg;base64,[A-Za-z0-9+/]+={0,2}$/.test(photoDataUrl)) {
    return Response.json({ error: "Choose a valid JPEG image under the size limit." }, { status: 400 });
  }
  const database = await ensureProfilePhotosTable();
  await database.prepare("INSERT INTO account_profile_photos (account_id, photo_data_url, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP) ON CONFLICT(account_id) DO UPDATE SET photo_data_url = excluded.photo_data_url, updated_at = CURRENT_TIMESTAMP")
    .bind(user.accountId, photoDataUrl).run();
  return Response.json({ ok: true, message: "Profile photo saved to cloud." });
}
