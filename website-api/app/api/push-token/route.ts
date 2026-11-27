import { and, eq } from "drizzle-orm";
import { getDb } from "../../../db";
import { pushTokens } from "../../../db/schema";
import { assertSameOrigin, getChatGPTUser } from "../../chatgpt-auth";
import { ensurePushTokenProviderStorage } from "../../push-token-storage";

const validProvider = (value: unknown): value is "expo" | "fcm" =>
  value === "expo" || value === "fcm";

export async function POST(request: Request) {
  if (!assertSameOrigin(request)) return Response.json({ error: "Invalid request." }, { status: 403 });
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });

  const body = (await request.json()) as {
    token?: string;
    deliveryBinding?: string;
    provider?: "expo" | "fcm";
    platform?: "ios" | "android";
  };
  const token = body.token?.trim();
  const provider = body.provider ?? "expo";
  const platform = body.platform === "ios" ? "ios" : "android";
  if (!token || token.length > 4096 || /\s/.test(token)) {
    return Response.json({ error: "A valid push token is required." }, { status: 400 });
  }
  if (!validProvider(provider)) {
    return Response.json({ error: "Unsupported push provider." }, { status: 400 });
  }

  const deliveryBinding = provider === "fcm" && typeof body.deliveryBinding === "string" &&
    /^[a-f0-9]{64}$/.test(body.deliveryBinding) ? body.deliveryBinding : null;
  await ensurePushTokenProviderStorage();
  await getDb()
    .insert(pushTokens)
    .values({ accountId: user.accountId, expoPushToken: token, deliveryBinding, provider, platform, updatedAt: new Date().toISOString() })
    .onConflictDoUpdate({
      target: pushTokens.expoPushToken,
      set: { accountId: user.accountId, deliveryBinding, provider, platform, updatedAt: new Date().toISOString() },
    });

  return Response.json({ registered: true }, { headers: { "cache-control": "no-store" } });
}

// Called before sign-out / notification opt-out. Removal is scoped to the
// authenticated account so one account cannot revoke another account's token.
export async function DELETE(request: Request) {
  if (!assertSameOrigin(request)) return Response.json({ error: "Invalid request." }, { status: 403 });
  // A signed-out device retains only the removal capability for its old registration.
  if (request.headers.get("content-type")?.includes("application/json")) {
    const body = await request.json() as { removalKey?: string };
    if (typeof body.removalKey !== "string" || !/^[a-f0-9]{64}$/.test(body.removalKey)) {
      return Response.json({ error: "Invalid removal key." }, { status: 400 });
    }
    const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(body.removalKey));
    const binding = Array.from(new Uint8Array(digest), byte => byte.toString(16).padStart(2, "0")).join("");
    await ensurePushTokenProviderStorage();
    await getDb().delete(pushTokens).where(eq(pushTokens.deliveryBinding, binding));
    return Response.json({ removed: true });
  }
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });

  const url = new URL(request.url);
  const token = url.searchParams.get("token")?.trim();
  const provider = url.searchParams.get("provider");
  if (!token) return Response.json({ error: "A push token is required." }, { status: 400 });
  if (provider && !validProvider(provider)) {
    return Response.json({ error: "Unsupported push provider." }, { status: 400 });
  }

  await ensurePushTokenProviderStorage();
  const filters = [eq(pushTokens.expoPushToken, token), eq(pushTokens.accountId, user.accountId)];
  if (validProvider(provider)) filters.push(eq(pushTokens.provider, provider));
  await getDb().delete(pushTokens).where(and(...filters));
  return Response.json({ removed: true }, { headers: { "cache-control": "no-store" } });
}
