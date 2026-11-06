import { count } from "drizzle-orm";import { redirect } from "next/navigation";import { getDb } from "../../db";import { authAccounts } from "../../db/schema";import { LoginForm } from "../AuthForms";import { getChatGPTUser } from "../chatgpt-auth";
export const dynamic="force-dynamic";
export default async function Login({searchParams}:{searchParams:Promise<{returnTo?:string}>}){if(await getChatGPTUser())redirect("/");const [{total}]=await getDb().select({total:count()}).from(authAccounts);if(total===0)redirect("/setup");return <LoginForm returnTo={(await searchParams).returnTo||"/"}/>;}
