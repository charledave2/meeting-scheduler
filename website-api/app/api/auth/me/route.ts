import { getChatGPTUser } from "../../../chatgpt-auth";

// Lightweight JSON endpoint so native/mobile clients (which can't use Next.js
// server components) can check the current session and get user info.
export async function GET() {
  const user = await getChatGPTUser();
  if (!user) return Response.json({ error: "Unauthorized" }, { status: 401 });
  return Response.json({
    user: {
      displayName: user.displayName,
      username: user.username,
      role: user.role,
      mustChangePassword: user.mustChangePassword,
    },
  });
}
