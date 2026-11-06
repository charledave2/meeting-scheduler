import { eq } from "drizzle-orm";
import { getDb, getRuntimeEnv } from "../../../db";
import { members, participantImportAudits, users } from "../../../db/schema";
import { getChatGPTUser } from "../../chatgpt-auth";
import { canManageParticipants } from "../../permissions";

type Appointment = "elder" | "ministerial" | "publisher";
type ServiceStatus = "regular_pioneer" | "none";
type Gender = "brother" | "sister";
type SheetRow = { row: number; name: string; appointment: Appointment; serviceStatus: ServiceStatus; gender: Gender };

async function requireAdmin() {
  const auth = await getChatGPTUser();
  if (!auth) return null;
  const db = getDb();
  let [user] = await db.select().from(users).where(eq(users.email, auth.email)).limit(1);
  const isAdmin = auth.email.toLowerCase() === (getRuntimeEnv().ADMIN_EMAIL || "").toLowerCase();
  if (!user) [user] = await db.insert(users).values({ email: auth.email, displayName: auth.displayName, role: isAdmin ? "admin" : "viewer" }).returning();
  else if (isAdmin && user.role !== "admin") [user] = await db.update(users).set({ role: "admin" }).where(eq(users.id, user.id)).returning();
  // Bulk participant import counts as participant management, so it follows
  // the same configurable permission as add/edit/delete (admin always
  // allowed; scheduler only if the admin has enabled it).
  return (await canManageParticipants(user)) ? user : null;
}

const base64Url = (value: string | Uint8Array) => {
  const bytes = typeof value === "string" ? new TextEncoder().encode(value) : value;
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
};

async function googleAccessToken() {
  const { GOOGLE_SERVICE_ACCOUNT_EMAIL: email, GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY: rawKey } = getRuntimeEnv();
  if (!email || !rawKey) throw new Error("The private Google Sheets connection is not configured yet.");
  const now = Math.floor(Date.now() / 1000);
  const encodedHeader = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const encodedClaim = base64Url(JSON.stringify({ iss: email, scope: "https://www.googleapis.com/auth/spreadsheets.readonly", aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600 }));
  const unsigned = `${encodedHeader}.${encodedClaim}`;
  const pem = rawKey.replace(/\\n/g, "\n").replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const keyBytes = Uint8Array.from(atob(pem), (char) => char.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", keyBytes, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const signature = new Uint8Array(await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned)));
  const response = await fetch("https://oauth2.googleapis.com/token", { method: "POST", headers: { "content-type": "application/x-www-form-urlencoded" }, body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: `${unsigned}.${base64Url(signature)}` }) });
  const data = await response.json() as { access_token?: string; error_description?: string };
  if (!response.ok || !data.access_token) throw new Error(data.error_description || "Unable to connect to Google Sheets.");
  return data.access_token;
}

function parseCsv(csv: string) {
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = "";
  let quoted = false;
  for (let index = 0; index < csv.length; index++) {
    const char = csv[index];
    if (quoted) {
      if (char === '"' && csv[index + 1] === '"') { cell += '"'; index++; }
      else if (char === '"') quoted = false;
      else cell += char;
    } else if (char === '"') quoted = true;
    else if (char === ",") { row.push(cell); cell = ""; }
    else if (char === "\n") { row.push(cell.replace(/\r$/, "")); rows.push(row); row = []; cell = ""; }
    else cell += char;
  }
  if (cell || row.length) { row.push(cell.replace(/\r$/, "")); rows.push(row); }
  if (rows[0]?.[0]) rows[0][0] = rows[0][0].replace(/^\uFEFF/, "");
  return rows;
}

function sheetNameFromDownload(header: string | null) {
  if (!header) return null;
  const encoded = header.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  const plain = header.match(/filename="?([^";]+)"?/i)?.[1];
  const filename = encoded ? decodeURIComponent(encoded) : plain;
  return filename?.replace(/\.csv$/i, "") || null;
}

function spreadsheetId(url: string) {
  const parsed = new URL(url);
  const match = parsed.pathname.match(/\/spreadsheets\/d\/([a-zA-Z0-9-_]+)/);
  if (!match) throw new Error("The Google Sheets link is invalid.");
  return { id: match[1], gid: new URLSearchParams(parsed.hash.replace(/^#/, "")).get("gid") };
}

function normalizeAppointment(value: string): Appointment | null {
  const role = value.trim().toLowerCase().replace(/[._-]/g, " ").replace(/\s+/g, " ");
  if (["elder", "elders", "matanda"].includes(role)) return "elder";
  if (["ministerial servant", "ministerial", "ms", "ministeryal", "ministeryal na lingkod"].includes(role)) return "ministerial";
  if (["publisher", "publishers", "mamamahayag"].includes(role)) return "publisher";
  return null;
}

function normalizeServiceStatus(value: string): ServiceStatus | null {
  const status = value.trim().toLowerCase().replace(/[._-]/g, " ").replace(/\s+/g, " ");
  if (!status || ["none", "n/a", "no", "hindi", "publisher", "mamamahayag"].includes(status)) return "none";
  if (["regular pioneer", "regular pioneers", "rp", "regular na payunir", "regular payunir", "yes", "oo", "true", "x"].includes(status)) return "regular_pioneer";
  return null;
}

async function readSheet(url: string) {
  const { id, gid } = spreadsheetId(url);
  const publicResponse = await fetch(`https://docs.google.com/spreadsheets/d/${id}/export?format=csv&gid=${encodeURIComponent(gid || "0")}`, { redirect: "follow" });
  const publicText = await publicResponse.text();
  const looksLikeCsv = publicResponse.ok && !/<!doctype html|<html[\s>]/i.test(publicText.slice(0, 500));
  if (looksLikeCsv) {
    return {
      sheetTitle: sheetNameFromDownload(publicResponse.headers.get("content-disposition")) || "Public Google Sheet",
      tabName: gid ? `Sheet ${gid}` : "First sheet",
      values: parseCsv(publicText),
    };
  }

  const { GOOGLE_SERVICE_ACCOUNT_EMAIL: email, GOOGLE_SERVICE_ACCOUNT_PRIVATE_KEY: key } = getRuntimeEnv();
  if (!email || !key) throw new Error("Unable to read the Sheet. Under Share → General access, select Anyone with the link as Viewer, then try again.");
  const token = await googleAccessToken();
  const headers = { authorization: `Bearer ${token}` };
  const metadataResponse = await fetch(`https://sheets.googleapis.com/v4/spreadsheets/${id}?fields=properties.title,sheets.properties`, { headers });
  const metadata = await metadataResponse.json() as { properties?: { title?: string }; sheets?: Array<{ properties?: { sheetId?: number; title?: string } }>; error?: { message?: string } };
  if (!metadataResponse.ok) throw new Error(metadata.error?.message || "Unable to read the Sheet. Make sure the public link is set to Viewer or the Sheet is shared with the service account.");
  const tab = metadata.sheets?.find((sheet) => gid && String(sheet.properties?.sheetId) === gid) || metadata.sheets?.[0];
  const tabName = tab?.properties?.title;
  if (!tabName) throw new Error("No readable tab was found in the Sheet.");
  const range = `'${tabName.replace(/'/g, "''")}'!A:C`;
  const valuesResponse = await fetch(`https://sheets.googleapis.com/v4/spreadsheets/${id}/values/${encodeURIComponent(range)}?majorDimension=ROWS`, { headers });
  const valuesData = await valuesResponse.json() as { values?: string[][]; error?: { message?: string } };
  if (!valuesResponse.ok) throw new Error(valuesData.error?.message || "Unable to read the participant rows.");
  return { sheetTitle: metadata.properties?.title || "Google Sheet", tabName, values: valuesData.values || [] };
}

async function buildPreview(url: string, gender: Gender) {
  const sheet = await readSheet(url);
  const skipped: Array<{ row: number; reason: string }> = [];
  const validRows: SheetRow[] = [];
  const seen = new Set<string>();
  const values = sheet.values;
  const first = values[0]?.map((cell) => cell.trim().toLowerCase()) || [];
  const pioneerList = ["regular pioneer", "regular pioneers", "regular na payunir"].includes(first[0] || "") && !first[1];
  const serviceOnly = ["regular pioneer", "service status", "pioneer status"].includes(first[1] || "");
  const hasHeader = pioneerList || ["name", "pangalan", "participant"].includes(first[0] || "") || ["appointment", "role", "atas"].includes(first[1] || "");
  for (let index = hasHeader ? 1 : 0; index < values.length; index++) {
    const name = (values[index]?.[0] || "").trim().replace(/\s+/g, " ");
    const appointment = pioneerList || serviceOnly ? "publisher" : normalizeAppointment(values[index]?.[1] || "");
    const serviceStatus = pioneerList ? "regular_pioneer" : serviceOnly ? normalizeServiceStatus(values[index]?.[1] || "") : normalizeServiceStatus(values[index]?.[2] || "");
    const key = name.toLocaleLowerCase();
    if (!name) { skipped.push({ row: index + 1, reason: "Missing name" }); continue; }
    if (!appointment) { skipped.push({ row: index + 1, reason: `Invalid role para kay ${name}` }); continue; }
    if (!serviceStatus) { skipped.push({ row: index + 1, reason: `Invalid service status para kay ${name}` }); continue; }
    if (seen.has(key)) { skipped.push({ row: index + 1, reason: `Duplicate: ${name}` }); continue; }
    seen.add(key);
    validRows.push({ row: index + 1, name, appointment, serviceStatus, gender: appointment === "publisher" ? gender : "brother" });
  }
  const existing = await getDb().select().from(members);
  const byName = new Map(existing.map((member) => [member.name.trim().toLocaleLowerCase(), member]));
  const added: SheetRow[] = [];
  const updated: Array<SheetRow & { from: Appointment; fromServiceStatus: ServiceStatus; fromGender: "brother" | "sister" | "unspecified" }> = [];
  const unchanged: SheetRow[] = [];
  for (const row of validRows) {
    const member = byName.get(row.name.toLocaleLowerCase());
    if (!member) added.push(row);
    else { const appointment=pioneerList||serviceOnly?member.appointment:row.appointment;const resolved={...row,appointment,gender:appointment==="publisher"?row.gender:"brother" as const}; if (member.appointment !== resolved.appointment || member.serviceStatus !== resolved.serviceStatus || member.gender !== resolved.gender || !member.active) updated.push({ ...resolved, from: member.appointment, fromServiceStatus: member.serviceStatus, fromGender: member.gender }); else unchanged.push(resolved); }
  }
  return { sheetTitle: sheet.sheetTitle, tabName: sheet.tabName, added, updated, unchanged, skipped };
}

export async function POST(request: Request) {
  const admin = await requireAdmin();
  if (!admin) return Response.json({ error: "You do not have permission to import participants." }, { status: 403 });
  try {
    const body = await request.json() as { url?: string; gender?: Gender; action?: "review" | "approve"; approved?: boolean };
    if (!body.url) return Response.json({ error: "Enter the Google Sheets link." }, { status: 400 });
    if (!body.gender || !["brother", "sister"].includes(body.gender)) return Response.json({ error: "Piliin kung Brothers o Sisters ang listahan." }, { status: 400 });
    const preview = await buildPreview(body.url, body.gender);
    if (body.action !== "approve") return Response.json({ preview });
    if (body.approved !== true) return Response.json({ error: "You must accept the disclaimer first." }, { status: 400 });
    const db = getDb();
    for (const row of preview.added) await db.insert(members).values({ name: row.name, appointment: row.appointment, serviceStatus: row.serviceStatus, gender: row.gender, active: true, createdBy: admin.id });
    for (const row of preview.updated) {
      const [member] = await db.select().from(members).where(eq(members.name, row.name)).limit(1);
      if (member) await db.update(members).set({ appointment: row.appointment, serviceStatus: row.serviceStatus, gender: row.gender, active: true }).where(eq(members.id, member.id));
      else {
        const all = await db.select().from(members);
        const matched = all.find((item) => item.name.trim().toLocaleLowerCase() === row.name.toLocaleLowerCase());
        if (matched) await db.update(members).set({ appointment: row.appointment, serviceStatus: row.serviceStatus, gender: row.gender, active: true }).where(eq(members.id, matched.id));
      }
    }
    const result = { added: preview.added.length, updated: preview.updated.length, unchanged: preview.unchanged.length, skipped: preview.skipped.length };
    await db.insert(participantImportAudits).values({ sourceUrl: body.url, sheetTitle: preview.sheetTitle, tabName: preview.tabName, addedCount: result.added, updatedCount: result.updated, unchangedCount: result.unchanged, skippedCount: result.skipped, detailsJson: JSON.stringify(preview), importedBy: admin.id });
    return Response.json({ result });
  } catch (error) {
    return Response.json({ error: error instanceof Error ? error.message : "Unable to import participants." }, { status: 400 });
  }
}
