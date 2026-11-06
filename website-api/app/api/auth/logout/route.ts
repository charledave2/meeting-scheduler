import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { authSessions } from "../../../../db/schema";
import { assertSameOrigin, currentSessionToken, expiredSessionCookie } from "../../../chatgpt-auth";

const hex = (bytes: Uint8Array) => Array.from(bytes, byte => byte.toString(16).padStart(2, "0")).join("");
async function revokeCurrentSession() { const token = await currentSessionToken(); if (!token) return; const hash = hex(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token)))); await getDb().delete(authSessions).where(eq(authSessions.tokenHash, hash)); }
export async function GET(request: Request) { await revokeCurrentSession(); return new Response(null, { status: 302, headers: { location: new URL("/login", request.url).toString(), "set-cookie": expiredSessionCookie(), "cache-control": "no-store" } }); }
export async function POST(request: Request) { if (!assertSameOrigin(request)) return Response.json({ error: "Invalid request." }, { status: 403 }); await revokeCurrentSession(); return Response.json({ ok: true }, { headers: { "set-cookie": expiredSessionCookie(), "cache-control": "no-store" } }); }
