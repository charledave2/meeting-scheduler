import { ChangePasswordForm } from "../AuthForms";import { requireChatGPTUser } from "../chatgpt-auth";
export const dynamic="force-dynamic";export default async function ChangePassword(){await requireChatGPTUser("/change-password");return <ChangePasswordForm/>;}
