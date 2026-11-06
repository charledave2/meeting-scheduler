import { redirect } from "next/navigation";import { AccountsManager } from "../AuthForms";import { requireChatGPTUser } from "../chatgpt-auth";
export const dynamic="force-dynamic";export default async function Accounts(){const user=await requireChatGPTUser("/accounts");if(user.role!=="admin")redirect("/");return <AccountsManager/>;}
