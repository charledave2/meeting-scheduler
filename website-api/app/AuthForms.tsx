"use client";

import { FormEvent, useEffect, useState } from "react";
import Link from "next/link";

function Brand() {
  return <div className="authBrand"><span className="brandmark">M</span><div><strong>Meeting Scheduler</strong><small>Midweek meeting workspace</small></div></div>;
}

export function LoginForm({ returnTo = "/" }: { returnTo?: string }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError("");
    const form = new FormData(event.currentTarget);
    const response = await fetch("/api/auth/login", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ username: form.get("username"), password: form.get("password"), returnTo }) });
    const data = await response.json() as { error?: string; redirectTo?: string };
    if (response.ok) window.location.assign(data.redirectTo || "/"); else { setError(data.error || "Unable to sign in."); setBusy(false); }
  }
  return <AuthShell eyebrow="ACCOUNT ACCESS" title="Welcome back" subtitle="Sign in with your Meeting Scheduler account.">
    <form onSubmit={submit} className="authForm"><label>Username<input name="username" autoComplete="username" required autoFocus /></label><label>Password<input name="password" type="password" autoComplete="current-password" required /></label>{error&&<p className="authError">{error}</p>}<button disabled={busy}>{busy?"Signing in…":"Sign in"}</button></form>
    <p className="authFoot">No account yet? <a href="/request-access">Request access</a></p>
  </AuthShell>;
}

export function RequestAccessForm() {
  const [busy, setBusy] = useState(false); const [message, setMessage] = useState(""); const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(""); setMessage("");
    const form = new FormData(event.currentTarget);
    const response = await fetch("/api/auth/request", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ displayName: form.get("displayName"), username: form.get("username"), requestNote: form.get("requestNote") }) });
    const data = await response.json() as { error?: string; message?: string };
    if (response.ok) { setMessage(data.message || "Request sent."); event.currentTarget.reset(); } else setError(data.error || "Unable to send request."); setBusy(false);
  }
  return <AuthShell eyebrow="REQUEST ACCESS" title="Ask to join" subtitle="The admin will review your request and give you a temporary password.">
    <form onSubmit={submit} className="authForm"><label>Full name<input name="displayName" required autoFocus /></label><label>Preferred username<input name="username" required minLength={3} maxLength={32} placeholder="e.g. juan.delacruz" /></label><label>Note <small>Optional</small><textarea name="requestNote" rows={3} placeholder="How the admin will recognize you" /></label>{message&&<p className="authSuccess">{message}</p>}{error&&<p className="authError">{error}</p>}<button disabled={busy}>{busy?"Sending…":"Send request"}</button></form><p className="authFoot"><a href="/login">Back to sign in</a></p>
  </AuthShell>;
}

export function SetupForm() {
  const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(""); const form = new FormData(event.currentTarget);
    const password = String(form.get("password") || ""); if (password !== form.get("confirmPassword")) { setError("Passwords do not match."); setBusy(false); return; }
    try {
      const response = await fetch("/api/auth/setup", { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ displayName: form.get("displayName"), username: form.get("username"), password }) });
      const text = await response.text(); let data: { error?: string; redirectTo?: string } = {};
      try { data = text ? JSON.parse(text) : {}; } catch { data = {}; }
      if (response.ok) window.location.assign(data.redirectTo || "/"); else setError(data.error || `Unable to create the admin account (error ${response.status}).`);
    } catch { setError("The server did not respond. Check your connection and try again."); }
    finally { setBusy(false); }
  }
  return <AuthShell eyebrow="ONE-TIME SETUP" title="Create the admin account" subtitle="This page closes permanently after the first admin account is created.">
    <form onSubmit={submit} className="authForm"><label>Display name<input name="displayName" required autoFocus /></label><label>Admin username<input name="username" required minLength={3} maxLength={32} /></label><label>Password<input name="password" type="password" required minLength={10} autoComplete="new-password" /><small>At least 10 characters</small></label><label>Confirm password<input name="confirmPassword" type="password" required minLength={10} autoComplete="new-password" /></label>{error&&<p className="authError">{error}</p>}<button disabled={busy}>{busy?"Creating…":"Create admin account"}</button></form>
  </AuthShell>;
}

export function ChangePasswordForm() {
  const [busy, setBusy] = useState(false); const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); setBusy(true); setError(""); const form=new FormData(event.currentTarget); const password=String(form.get("newPassword")||""); if(password!==form.get("confirmPassword")){setError("Passwords do not match.");setBusy(false);return;} const response=await fetch("/api/auth/change-password",{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify({currentPassword:form.get("currentPassword"),newPassword:password})});const data=await response.json() as {error?:string};if(response.ok)window.location.assign("/");else{setError(data.error||"Unable to update password.");setBusy(false);}}
  return <AuthShell eyebrow="ACCOUNT SECURITY" title="Set a new password" subtitle="Change the temporary password before continuing."><form onSubmit={submit} className="authForm"><label>Current password<input name="currentPassword" type="password" required autoFocus /></label><label>New password<input name="newPassword" type="password" required minLength={10} /></label><label>Confirm new password<input name="confirmPassword" type="password" required minLength={10} /></label>{error&&<p className="authError">{error}</p>}<button disabled={busy}>{busy?"Saving…":"Save new password"}</button></form></AuthShell>;
}

type Account = { id:number; displayName:string; username:string; status:"requested"|"active"|"disabled"; role:"admin"|"member"|null; createdAt:string; requestNote:string };
export function AccountsManager() {
  const [accounts,setAccounts]=useState<Account[]>([]);const [busy,setBusy]=useState("");const [notice,setNotice]=useState("");
  async function refresh(){const response=await fetch("/api/accounts");if(response.ok)setAccounts((await response.json()).accounts);}
  useEffect(()=>{let active=true;fetch("/api/accounts").then(response=>response.ok?response.json():null).then(data=>{if(active&&data)setAccounts(data.accounts);});return()=>{active=false;};},[]);
  async function action(account:Account,type:"approve"|"reset"|"disable"|"enable"){let temporaryPassword="";if(type==="approve"||type==="reset"){temporaryPassword=window.prompt(`Enter a temporary password for ${account.displayName} (at least 10 characters):`)||"";if(!temporaryPassword)return;}if(type==="disable"&&!window.confirm(`Disable ${account.displayName}'s account?`))return;setBusy(`${type}-${account.id}`);const response=await fetch("/api/accounts",{method:"PATCH",headers:{"content-type":"application/json"},body:JSON.stringify({id:account.id,action:type,temporaryPassword})});const data=await response.json();setNotice(response.ok?data.message:data.error||"Action failed.");await refresh();setBusy("");}
  return <div className="accountsPage"><header className="accountHeader"><Link className="accountHeaderAction accountBack" href="/" aria-label="Back to Scheduler"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m15 18-6-6 6-6"/></svg><span>Scheduler</span></Link><div className="accountHeaderTitle"><strong>Accounts &amp; access</strong><small>Admin management</small></div><a className="accountHeaderAction accountSignOut" href="/api/auth/logout" aria-label="Sign out"><span>Sign out</span><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M10 17l5-5-5-5M15 12H3M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/></svg></a></header><main><div className="accountsTitle"><div><small>ADMIN</small><h1>Accounts & approvals</h1><p>Approve requests, issue temporary passwords, and control access.</p></div><span>{accounts.filter(a=>a.status==="requested").length} pending</span></div>{notice&&<p className="accountNotice">{notice}</p>}<section className="accountList">{accounts.map(account=><article key={account.id}><div><strong>{account.displayName}</strong><span>@{account.username}</span>{account.requestNote&&<p>{account.requestNote}</p>}</div><span className={`accountStatus ${account.status}`}>{account.status}</span><div className="accountActions">{account.status==="requested"&&<button onClick={()=>action(account,"approve")} disabled={!!busy}>Approve</button>}{account.status==="active"&&account.role!=="admin"&&<><button className="secondary" onClick={()=>action(account,"reset")} disabled={!!busy}>Reset password</button><button className="danger" onClick={()=>action(account,"disable")} disabled={!!busy}>Disable</button></>}{account.status==="disabled"&&<button onClick={()=>action(account,"enable")} disabled={!!busy}>Enable</button>}</div></article>)}{!accounts.length&&<p>No account requests yet.</p>}</section></main></div>;
}

function AuthShell({ eyebrow,title,subtitle,children }:{eyebrow:string;title:string;subtitle:string;children:React.ReactNode}) { return <main className="authPage"><Brand/><section className="authCard"><small>{eyebrow}</small><h1>{title}</h1><p>{subtitle}</p>{children}</section></main>; }
