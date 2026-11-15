import { assertSameOrigin, getChatGPTUser } from "../../../chatgpt-auth";
import { sendPushToAccounts } from "../../../push-notifications";

export async function POST(request: Request) {
  if (!assertSameOrigin(request)) return Response.json({ error: "Invalid request." }, { status: 403 });
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });

  const result = await sendPushToAccounts([user.accountId], {
    title: "JW Meeting Scheduler test",
    body: "Gumagana ang push notification para sa account na ito.",
    data: { type: "notification_test" },
  });
  if (!result.sent) {
    return Response.json({ error: "Walang naipadalang notification. I-check ang notification permission, FCM registration, at Firebase Worker credentials." }, { status: 503 });
  }
  return Response.json({ sent: result.sent }, { headers: { "cache-control": "no-store" } });
}
