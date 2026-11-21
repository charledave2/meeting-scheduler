import { isNull } from "drizzle-orm";
import { getDb, getRuntimeEnv } from "../db";
import { schedules } from "../db/schema";
import { assignedMemberDetails } from "./api/schedules/route";
import { sendPushToAssignedMembers } from "./push-notifications";

const MANILA_OFFSET_MS = 8 * 60 * 60 * 1000;
const DAY_MS = 24 * 60 * 60 * 1000;

async function ensureReminderStorage() {
  const database = getRuntimeEnv().DB;
  if (!database) throw new Error("Database binding is unavailable.");
  await database.prepare(`
    CREATE TABLE IF NOT EXISTS assignment_reminder_deliveries (
      schedule_id INTEGER NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
      member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE CASCADE,
      reminder_date TEXT NOT NULL,
      sent_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (schedule_id, member_id, reminder_date)
    )
  `).run();
  return database;
}

export async function sendScheduledAssignmentReminders(scheduledTime: number) {
  // The cron runs at 00:00 UTC, which is 08:00 in the Philippines.
  const local = new Date(scheduledTime + MANILA_OFFSET_MS);
  const day = local.getUTCDay();
  if (day !== 1 && day !== 3) return;
  const reminderDate = local.toISOString().slice(0, 10);
  const weekStart = new Date(Date.UTC(local.getUTCFullYear(), local.getUTCMonth(), local.getUTCDate()) -
    (day === 1 ? 0 : 2) * DAY_MS).toISOString().slice(0, 10);
  const database = await ensureReminderStorage();
  const rows = await getDb().select({
    id: schedules.id, weekLabel: schedules.weekLabel,
    detailsJson: schedules.detailsJson, assignmentsJson: schedules.assignmentsJson, updatedAt: schedules.updatedAt,
  }).from(schedules).where(isNull(schedules.deletedAt));
  const seenWeeks = new Set<string>();
  for (const row of rows.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))) {
    try {
      const meta = JSON.parse(row.detailsJson) as Record<string, string>;
      if (meta.weekStart !== weekStart || seenWeeks.has(meta.weekStart)) continue;
      seenWeeks.add(meta.weekStart);
      const parts = JSON.parse(row.assignmentsJson);
      if (!Array.isArray(parts)) continue;
      const recipients = await assignedMemberDetails(meta, parts);
      for (const [memberId, details] of recipients) {
        const claim = await database.prepare(
          "INSERT OR IGNORE INTO assignment_reminder_deliveries (schedule_id, member_id, reminder_date) VALUES (?, ?, ?)"
        ).bind(row.id, memberId, reminderDate).run();
        if (!claim.meta.changes) continue;
        try {
          const result = await sendPushToAssignedMembers([memberId], {
            title: day === 1 ? "Paalala: May bahagi ka sa Huwebes" : "Paalala: Bukas na ang bahagi mo",
            body: `${row.weekLabel} · ${details.join(" • ")}`,
            data: { scheduleId: String(row.id) },
          });
          // Allow a later retry if this account has no active device token or the send failed.
          if (!result.sent) throw new Error("No push message was accepted.");
        } catch (error) {
          await database.prepare(
            "DELETE FROM assignment_reminder_deliveries WHERE schedule_id = ? AND member_id = ? AND reminder_date = ?"
          ).bind(row.id, memberId, reminderDate).run();
          console.error("Assignment reminder send failed", row.id, memberId, error);
        }
      }
    } catch (error) {
      console.error("Assignment reminder processing failed", row.id, error);
    }
  }
}
