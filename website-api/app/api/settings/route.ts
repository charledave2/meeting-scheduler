import { getChatGPTUser } from "../../chatgpt-auth";
import { getAppSettings, isAdmin, updateAppSettings } from "../../permissions";

// Any authenticated user can read the settings (needed so the mobile/web app
// knows whether Scheduler accounts currently have participant-edit access),
// but only admins can change them.
export async function GET() {
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  if (user.role === "viewer") return Response.json({ error: "Viewer accounts cannot access administrative settings." }, { status: 403 });
  const settings = await getAppSettings();
  return Response.json({ settings: { schedulerCanEditParticipants: settings.schedulerCanEditParticipants } });
}

export async function PATCH(request: Request) {
  const user = await getChatGPTUser();
  if (!isAdmin(user)) return Response.json({ error: "Admin only" }, { status: 403 });
  const body = (await request.json()) as { schedulerCanEditParticipants?: boolean };
  if (typeof body.schedulerCanEditParticipants !== "boolean") {
    return Response.json({ error: "schedulerCanEditParticipants must be a boolean." }, { status: 400 });
  }
  const updated = await updateAppSettings({ schedulerCanEditParticipants: body.schedulerCanEditParticipants });
  return Response.json({ settings: { schedulerCanEditParticipants: updated.schedulerCanEditParticipants } });
}
