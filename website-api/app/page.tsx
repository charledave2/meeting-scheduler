import SchedulerApp from "./SchedulerApp";
import { chatGPTSignOutPath, requireChatGPTUser } from "./chatgpt-auth";

export const dynamic = "force-dynamic";

export default async function Home() {
  const user = await requireChatGPTUser("/");
  return <SchedulerApp user={{ name: user.displayName, email: user.email, isAdmin: user.role === "admin" }} signOutPath={chatGPTSignOutPath()} />;
}
