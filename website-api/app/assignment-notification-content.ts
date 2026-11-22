import { getDb } from "../db";
import { members } from "../db/schema";
import { meetingDateFromLabel } from "./meeting-date";

type Part = {
  title?: string;
  assignee?: string;
  assistant?: string;
  secondaryAssignee?: string;
  secondaryAssistant?: string;
};

export type AssignmentNotice = {
  name: string;
  lines: string[];
};

export type ReminderKind = "assigned" | "updated" | "monday" | "wednesday";

function meetingDateLabel(meta: Record<string, string>) {
  const weekStart = /^\d{4}-\d{2}-\d{2}$/.test(meta.weekStart || "") ? meta.weekStart : null;
  const parsedDate = weekStart ? null : meetingDateFromLabel(meta.date || "");
  const date = weekStart || parsedDate;
  if (!date) return meta.date || "ang susunod na pulong";

  const [year, month, day] = date.split("-").map(Number);
  const meetingDate = new Date(Date.UTC(year, month - 1, day + (weekStart ? 3 : 0)));
  return new Intl.DateTimeFormat("en-US", {
    month: "long",
    day: "numeric",
    year: "numeric",
    timeZone: "UTC",
  }).format(meetingDate);
}

export function assignmentNoticeText(
  name: string,
  meta: Record<string, string>,
  lines: string[],
  kind: ReminderKind = "assigned",
) {
  const date = meetingDateLabel(meta);
  const title = kind === "updated"
    ? `Hello ${name}, may update sa bahagi mo — ${date}`
    : kind === "wednesday"
      ? `Hello ${name}, bukas na ang bahagi mo — ${date}`
      : `Hello ${name}, May bahagi ka sa ${date}`;
  return { title, body: lines.join("\n") };
}

export async function assignmentNoticeDetails(
  meta: Record<string, string>,
  parts: Part[],
  memberRows?: Array<{ id: number; name: string }>,
): Promise<Map<number, AssignmentNotice>> {
  const rows = memberRows || await getDb().select({ id: members.id, name: members.name }).from(members);
  const byName = new Map(rows.map((member) => [member.name.trim().toLocaleLowerCase("tl"), member]));
  const notices = new Map<number, AssignmentNotice>();

  function add(name: string | undefined, label: string, location: string) {
    const member = byName.get((name || "").trim().toLocaleLowerCase("tl"));
    if (!member) return;
    const notice = notices.get(member.id) || { name: member.name.trim(), lines: [] };
    const line = `${label} : ${location}`;
    if (!notice.lines.includes(line)) notice.lines.push(line);
    notices.set(member.id, notice);
  }

  add(meta.chairman, "Chairman", "Main Hall");
  add(meta.openingPrayer, "Panalangin sa simula", "Main Hall");
  add(meta.adviser, "Second Hall Adviser", "Second Hall");

  for (const part of parts) {
    const title = (part.title || "Bahagi sa pulong")
      .replace(/^\s*\d+\.\s*/, "")
      .replace(/\s*\(\d+\s*min(?:ute)?s?\.?\)\s*$/i, "")
      .trim();
    const isBibleStudy = /pag-aaral ng kongregasyon sa bibliya/i.test(title);
    const mainLabel = isBibleStudy ? `${title} — Conductor` : title;
    const assistantLabel = isBibleStudy ? `${title} — Reader` : `${title} — Assistant`;

    add(part.assignee, mainLabel, "Main Hall");
    add(part.assistant, assistantLabel, "Main Hall");
    add(part.secondaryAssignee, title, "Second Hall");
    add(part.secondaryAssistant, `${title} — Assistant`, "Second Hall");
  }

  return notices;
}
