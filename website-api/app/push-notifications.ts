import { eq } from "drizzle-orm";
import { getDb } from "../db";
import { pushTokens } from "../db/schema";

// Sends push notifications via Expo's push service (https://exp.host). Works
// for both iOS and Android without needing separate FCM/APNs setup, since the
// mobile app is built with Expo and registers an Expo push token.
const EXPO_PUSH_ENDPOINT = "https://exp.host/--/api/v2/push/send";

export async function sendPushToAccounts(
  accountIds: number[],
  notification: { title: string; body: string; data?: Record<string, unknown> }
) {
  if (!accountIds.length) return { sent: 0 };
  const db = getDb();
  const tokens: { expoPushToken: string }[] = [];
  for (const accountId of accountIds) {
    const rows = await db
      .select({ expoPushToken: pushTokens.expoPushToken })
      .from(pushTokens)
      .where(eq(pushTokens.accountId, accountId));
    tokens.push(...rows);
  }
  if (!tokens.length) return { sent: 0 };

  const messages = tokens.map((t) => ({
    to: t.expoPushToken,
    title: notification.title,
    body: notification.body,
    data: notification.data || {},
    sound: "default",
  }));

  // Expo's API accepts batches of up to 100 messages per request.
  const chunkSize = 100;
  for (let i = 0; i < messages.length; i += chunkSize) {
    const chunk = messages.slice(i, i + chunkSize);
    try {
      await fetch(EXPO_PUSH_ENDPOINT, {
        method: "POST",
        headers: { "content-type": "application/json", accept: "application/json" },
        body: JSON.stringify(chunk),
      });
    } catch (error) {
      console.error("Expo push send failed", error);
    }
  }
  return { sent: messages.length };
}

// Convenience: notify every active account (used for "new schedule published").
export async function sendPushToAllActiveAccounts(notification: { title: string; body: string; data?: Record<string, unknown> }) {
  const db = getDb();
  const { authAccounts } = await import("../db/schema");
  const active = await db.select({ id: authAccounts.id }).from(authAccounts).where(eq(authAccounts.status, "active"));
  return sendPushToAccounts(active.map((a) => a.id), notification);
}
