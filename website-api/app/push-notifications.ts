import { and, eq, inArray } from "drizzle-orm";
import { getDb, getRuntimeEnv } from "../db";
import { authAccounts, members, pushTokens } from "../db/schema";
import { ensurePushTokenProviderStorage } from "./push-token-storage";
import { ensureAccountMemberStorage } from "./account-member-storage";

const EXPO_PUSH_ENDPOINT = "https://exp.host/--/api/v2/push/send";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const FCM_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
const encoder = new TextEncoder();
let cachedGoogleToken: { value: string; expiresAt: number } | null = null;

function base64Url(bytes: Uint8Array) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}
function encodeJson(value: unknown) { return base64Url(encoder.encode(JSON.stringify(value))); }

async function firebaseAccessToken() {
  if (cachedGoogleToken && cachedGoogleToken.expiresAt > Date.now() + 60_000) return cachedGoogleToken.value;
  const env = getRuntimeEnv();
  const email = env.FIREBASE_SERVICE_ACCOUNT_EMAIL;
  const privateKey = env.FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY;
  if (!email || !privateKey) return null;
  const now = Math.floor(Date.now() / 1000);
  const header = encodeJson({ alg: "RS256", typ: "JWT" });
  const claims = encodeJson({ iss: email, scope: FCM_SCOPE, aud: FCM_TOKEN_ENDPOINT, iat: now, exp: now + 3600 });
  const unsigned = `${header}.${claims}`;
  const pem = privateKey.replace(/\\n/g, "\n").replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const keyBytes = Uint8Array.from(atob(pem), character => character.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", keyBytes, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const signature = new Uint8Array(await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, encoder.encode(unsigned)));
  const assertion = `${unsigned}.${base64Url(signature)}`;
  const response = await fetch(FCM_TOKEN_ENDPOINT, { method: "POST", headers: { "content-type": "application/x-www-form-urlencoded" }, body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }) });
  if (!response.ok) { console.error("Firebase OAuth token request failed", response.status); return null; }
  const result = await response.json() as { access_token?: string; expires_in?: number };
  if (!result.access_token) return null;
  cachedGoogleToken = { value: result.access_token, expiresAt: Date.now() + (result.expires_in || 3600) * 1000 };
  return result.access_token;
}

async function sendFcm(token: string, binding: string | null, notification: { title: string; body: string; data?: Record<string, unknown> }) {
  const projectId = getRuntimeEnv().FIREBASE_PROJECT_ID || "jwmeeting-scheduler";
  const accessToken = await firebaseAccessToken();
  if (!accessToken) return false;
  const data = Object.fromEntries(Object.entries(notification.data || {}).map(([key, value]) => [key, String(value)]));
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`, {
    method: "POST",
    headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
    body: JSON.stringify({ message: { token, ...(binding ? { data: { ...data, title: notification.title, body: notification.body, binding }, android: { priority: "high" } } : { notification: { title: notification.title, body: notification.body }, data, android: { priority: "high", notification: { channel_id: "meeting_assignments" } } }) } }),
  });
  if (response.ok) return true;
  const error = await response.json().catch(() => ({})) as { error?: { status?: string } };
  if (error.error?.status === "UNREGISTERED" || response.status === 404) await getDb().delete(pushTokens).where(eq(pushTokens.expoPushToken, token));
  console.error("Firebase push send failed", response.status, error.error?.status || "unknown");
  return false;
}

export async function sendPushToAccounts(accountIds: number[], notification: { title: string; body: string; data?: Record<string, unknown> }) {
  if (!accountIds.length) return { sent: 0 };
  await ensurePushTokenProviderStorage();
  const rows = await getDb().select({ token: pushTokens.expoPushToken, provider: pushTokens.provider, binding: pushTokens.deliveryBinding }).from(pushTokens).where(inArray(pushTokens.accountId, accountIds));
  const expoTokens = rows.filter(row => row.provider === "expo");
  const fcmTokens = rows.filter(row => row.provider === "fcm");
  let sent = 0;
  const messages = expoTokens.map(row => ({ to: row.token, title: notification.title, body: notification.body, data: notification.data || {}, sound: "default" }));
  for (let i = 0; i < messages.length; i += 100) {
    const chunk = messages.slice(i, i + 100);
    try {
      const response = await fetch(EXPO_PUSH_ENDPOINT, { method: "POST", headers: { "content-type": "application/json", accept: "application/json" }, body: JSON.stringify(chunk) });
      if (response.ok) sent += chunk.length; else console.error("Expo push send failed", response.status);
    } catch (error) { console.error("Expo push send failed", error); }
  }
  if (fcmTokens.length && !getRuntimeEnv().FIREBASE_SERVICE_ACCOUNT_EMAIL) console.warn("Firebase push skipped: service account credentials are not configured.");
  else {
    const results = await Promise.all(fcmTokens.map(row => sendFcm(row.token, row.binding, notification).catch(error => { console.error("Firebase push send failed", error); return false; })));
    sent += results.filter(Boolean).length;
  }
  return { sent, recipients: accountIds.length };
}

export async function sendPushToAssignedMembers(memberIds: number[], notification: { title: string; body: string; data?: Record<string, unknown> }) {
  const uniqueMemberIds = [...new Set(memberIds.filter(id => Number.isInteger(id) && id > 0))];
  if (!uniqueMemberIds.length) return { sent: 0, recipients: 0 };
  await ensureAccountMemberStorage();
  const linked = await getDb().select({ id: authAccounts.id }).from(authAccounts)
    .innerJoin(members, eq(authAccounts.memberId, members.id))
    .where(and(eq(authAccounts.status, "active"), eq(members.active, true), inArray(authAccounts.memberId, uniqueMemberIds)));
  const accountIds = [...new Set(linked.map(account => account.id))];
  return sendPushToAccounts(accountIds, notification);
}
