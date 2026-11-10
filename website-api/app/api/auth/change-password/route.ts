import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { authAccounts, authSessions } from "../../../../db/schema";
import { assertSameOrigin, createSession, getChatGPTUser, hashPassword, sessionCookie, validPassword, verifyPassword } from "../../../chatgpt-auth";

export async function POST(request: Request) {
  if (!assertSameOrigin(request)) return Response.json({ error: "Invalid request." }, { status: 403 });
  const user = await getChatGPTUser({ allowPasswordChange: true }); if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const body = await request.json() as { currentPassword?: string; newPassword?: string };
  if (!validPassword(body.newPassword || "")) return Response.json({ error: "Use at least 10 characters." }, { status: 400 });
  const db = getDb(); const [account] = await db.select().from(authAccounts).where(eq(authAccounts.id, user.accountId)).limit(1);
  if (!account?.passwordHash || !account.passwordSalt || !await verifyPassword(body.currentPassword || "", account.passwordHash, account.passwordSalt, account.passwordIterations)) return Response.json({ error: "Current password is incorrect." }, { status: 400 });
  const credentials = await hashPassword(body.newPassword || "");
  await db.update(authAccounts).set({ passwordHash: credentials.hash, passwordSalt: credentials.salt, passwordIterations: credentials.iterations, mustChangePassword: false, failedAttempts: 0, lockedUntil: null }).where(eq(authAccounts.id, user.accountId));
  await db.delete(authSessions).where(eq(authSessions.accountId, user.accountId));
  const session = await createSession(user.accountId);
  return Response.json({ ok: true }, { headers: { "set-cookie": sessionCookie(session.token, session.expiresAt), "cache-control": "no-store" } });
}

