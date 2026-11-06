import { and, eq } from "drizzle-orm";
import { getDb } from "../../../db";
import { pushTokens } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";

// Called by the mobile app after login (and whenever the Expo push token
// changes) to register this device for push notifications.
export async function POST(request: Request) {
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });

  const body = (await request.json()) as { token?: string; platform?: "ios" | "android" };
  const token = body.token?.trim();
  if (!token) return Response.json({ error: "A push token is required." }, { status: 400 });
  const platform = body.platform === "ios" ? "ios" : "android";

  const db = getDb();
  const [existing] = await db.select().from(pushTokens).where(eq(pushTokens.expoPushToken, token)).limit(1);
  if (existing) {
    await db
      .update(pushTokens)
      .set({ accountId: user.accountId, platform, updatedAt: new Date().toISOString() })
      .where(eq(pushTokens.id, existing.id));
  } else {
    await db.insert(pushTokens).values({ accountId: user.accountId, expoPushToken: token, platform });
  }
  return Response.json({ registered: true });
}

// Called on logout / notification opt-out to stop sending push to this device.
export async function DELETE(request: Request) {
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const token = new URL(request.url).searchParams.get("token");
  if (!token) return Response.json({ error: "A push token is required." }, { status: 400 });
  await getDb()
    .delete(pushTokens)
    .where(and(eq(pushTokens.expoPushToken, token), eq(pushTokens.accountId, user.accountId)));
  return Response.json({ removed: true });
}
