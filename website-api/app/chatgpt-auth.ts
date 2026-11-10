import { and, eq, gt } from "drizzle-orm";
import { cookies, headers } from "next/headers";
import { redirect } from "next/navigation";
import { getDb, getRuntimeEnv } from "../db";
import { authAccounts, authSessions, users } from "../db/schema";

const COOKIE_NAME = "meeting_session";
const SESSION_DAYS = 30;
// Cloudflare Workers Web Crypto accepts at most 100,000 PBKDF2 iterations.
export const PASSWORD_ITERATIONS = 100_000;

export type ChatGPTUser = {
  accountId: number; userId: number; displayName: string; email: string;
  fullName: string; username: string; role: "admin" | "scheduler" | "viewer";
  mustChangePassword: boolean; provider: "native";
};

export const normalizeUsername = (value: string) => value.trim().toLocaleLowerCase().replace(/\s+/g, "");
export const validUsername = (value: string) => /^[a-z0-9][a-z0-9._-]{2,31}$/.test(value);
export const validPassword = (value: string) => value.length >= 10 && value.length <= 128;
const hex = (bytes: Uint8Array) => Array.from(bytes, byte => byte.toString(16).padStart(2, "0")).join("");
const bytesFromHex = (value: string) => Uint8Array.from(value.match(/.{2}/g) ?? [], byte => Number.parseInt(byte, 16));

export async function hashPassword(password: string, saltHex?: string, iterations = PASSWORD_ITERATIONS) {
  const salt = saltHex ? bytesFromHex(saltHex) : crypto.getRandomValues(new Uint8Array(16));
  const material = await crypto.subtle.importKey("raw", new TextEncoder().encode(password), "PBKDF2", false, ["deriveBits"]);
  const result = await crypto.subtle.deriveBits({ name: "PBKDF2", hash: "SHA-256", salt, iterations }, material, 256);
  return { hash: hex(new Uint8Array(result)), salt: hex(salt), iterations };
}

export async function verifyPassword(password: string, hash: string, salt: string, iterations: number) {
  const candidate = await hashPassword(password, salt, iterations);
  if (candidate.hash.length !== hash.length) return false;
  let difference = 0;
  for (let index = 0; index < hash.length; index++) difference |= candidate.hash.charCodeAt(index) ^ hash.charCodeAt(index);
  return difference === 0;
}

async function sha256(value: string) {
  return hex(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value))));
}

function randomToken() {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

export async function createSession(accountId: number) {
  const token = randomToken();
  const expiresAt = new Date(Date.now() + SESSION_DAYS * 86400_000);
  await getDb().insert(authSessions).values({ tokenHash: await sha256(token), accountId, expiresAt: expiresAt.toISOString() });
  return { token, expiresAt };
}

export const sessionCookie = (token: string, expiresAt: Date) => `${COOKIE_NAME}=${token}; Path=/; HttpOnly; Secure; SameSite=Lax; Expires=${expiresAt.toUTCString()}`;
export const expiredSessionCookie = () => `${COOKIE_NAME}=; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=0`;
export async function currentSessionToken() { return (await cookies()).get(COOKIE_NAME)?.value ?? null; }

export async function getChatGPTUser(options: { allowPasswordChange?: boolean } = {}): Promise<ChatGPTUser | null> {
  const token = await currentSessionToken();
  if (!token) return null;
  const [row] = await getDb().select({
    accountId: authAccounts.id, userId: users.id, displayName: users.displayName,
    email: users.email, username: authAccounts.username, role: users.role,
    mustChangePassword: authAccounts.mustChangePassword,
  }).from(authSessions)
    .innerJoin(authAccounts, eq(authSessions.accountId, authAccounts.id))
    .innerJoin(users, eq(authAccounts.userId, users.id))
    .where(and(eq(authSessions.tokenHash, await sha256(token)), gt(authSessions.expiresAt, new Date().toISOString()), eq(authAccounts.status, "active")))
    .limit(1);
  if (row?.mustChangePassword && !options.allowPasswordChange) return null;
  return row ? { ...row, fullName: row.displayName, provider: "native" } : null;
}

export async function requireChatGPTUser(returnTo: string): Promise<ChatGPTUser> {
  const user = await getChatGPTUser({ allowPasswordChange: true });
  if (!user) redirect(`/login?returnTo=${encodeURIComponent(safeRelativeReturnPath(returnTo))}`);
  if (user.mustChangePassword && returnTo !== "/change-password") redirect("/change-password");
  return user;
}

export function chatGPTSignOutPath() { return "/api/auth/logout"; }
export function assertSameOrigin(request: Request) {
  const origin = request.headers.get("origin");
  const fetchSite = request.headers.get("sec-fetch-site");
  return fetchSite !== "cross-site" && (!origin || origin === new URL(request.url).origin);
}

export async function getBootstrapIdentity() {
  const requestHeaders = await headers();
  const adminEmail = (getRuntimeEnv().ADMIN_EMAIL || "").trim().toLowerCase();
  const jwt = requestHeaders.get("cf-access-jwt-assertion");
  const env = getRuntimeEnv();
  if (!jwt || !env.CF_ACCESS_TEAM_DOMAIN || !env.CF_ACCESS_AUD) return null;
  try {
    const [encodedHeader, encodedPayload, encodedSignature] = jwt.split(".");
    const jwtHeader = JSON.parse(new TextDecoder().decode(base64UrlBytes(encodedHeader))) as { alg?: string; kid?: string };
    const payload = JSON.parse(new TextDecoder().decode(base64UrlBytes(encodedPayload))) as { email?: string; aud?: string|string[]; iss?: string; exp?: number };
    const domain=env.CF_ACCESS_TEAM_DOMAIN.replace(/^https?:\/\//,"").replace(/\/$/,"");const issuer=`https://${domain}`;const audiences=Array.isArray(payload.aud)?payload.aud:[payload.aud];
    if(jwtHeader.alg!=="RS256"||!jwtHeader.kid||payload.iss!==issuer||!audiences.includes(env.CF_ACCESS_AUD)||!payload.exp||payload.exp<=Date.now()/1000)return null;
    const response=await fetch(`${issuer}/cdn-cgi/access/certs`,{headers:{accept:"application/json"}});if(!response.ok)return null;const keys=(await response.json() as {keys?:JsonWebKey[]}).keys||[];const jwk=keys.find(key=>key.kid===jwtHeader.kid);if(!jwk)return null;
    const key=await crypto.subtle.importKey("jwk",jwk,{name:"RSASSA-PKCS1-v1_5",hash:"SHA-256"},false,["verify"]);const valid=await crypto.subtle.verify({name:"RSASSA-PKCS1-v1_5"},key,base64UrlBytes(encodedSignature),new TextEncoder().encode(`${encodedHeader}.${encodedPayload}`));
    return valid&&payload.email?.trim().toLowerCase()===adminEmail ? adminEmail : null;
  } catch { return null; }
}

function base64UrlBytes(value: string) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const binary = atob(normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "="));
  return Uint8Array.from(binary, character => character.charCodeAt(0));
}

function safeRelativeReturnPath(value: string) {
  if (!value.startsWith("/") || value.startsWith("//")) return "/";
  try { const url = new URL(value, "https://app.local"); return url.origin === "https://app.local" ? `${url.pathname}${url.search}` : "/"; }
  catch { return "/"; }
}

