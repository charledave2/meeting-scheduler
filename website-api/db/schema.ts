import { sql } from "drizzle-orm";
import { index, integer, sqliteTable, text, uniqueIndex } from "drizzle-orm/sqlite-core";

export const users = sqliteTable("users", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  email: text("email").notNull(),
  displayName: text("display_name").notNull().default(""),
  // Three-tier role system: "admin" (fixed, full access), "scheduler"
  // (create/edit/assign schedules + export, but cannot save to cloud, and
  // cannot manage accounts/backup; participant edit access is a configurable
  // permission — see appSettings), "viewer" (read-only everywhere).
  // "member" (the old single non-admin role) is migrated to "viewer".
  role: text("role", { enum: ["admin", "scheduler", "viewer"] }).notNull().default("viewer"),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
}, (t) => [uniqueIndex("users_email_unique").on(t.email)]);

export const members = sqliteTable("members", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  name: text("name").notNull(),
  appointment: text("appointment", { enum: ["", "elder", "ministerial", "publisher"] }).notNull().default("publisher"),
  serviceStatus: text("service_status", { enum: ["", "regular_pioneer", "none"] }).notNull().default("none"),
  gender: text("gender", { enum: ["brother", "sister", "unspecified"] }).notNull().default("unspecified"),
  active: integer("active", { mode: "boolean" }).notNull().default(true),
  unbaptized: integer("unbaptized", { mode: "boolean" }).notNull().default(false),
  createdBy: integer("created_by").references(() => users.id),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

export const schedules = sqliteTable("schedules", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  title: text("title").notNull(),
  weekLabel: text("week_label").notNull(),
  bibleReading: text("bible_reading").notNull(),
  sourceUrl: text("source_url").notNull().default(""),
  detailsJson: text("details_json").notNull(),
  assignmentsJson: text("assignments_json").notNull().default("{}"),
  ownerId: integer("owner_id").notNull().references(() => users.id),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  deletedAt: text("deleted_at"),
  deletedBy: integer("deleted_by").references(() => users.id),
  deleteReason: text("delete_reason").notNull().default(""),
});

export const assignmentHistory = sqliteTable("assignment_history", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  scheduleId: integer("schedule_id").notNull().references(() => schedules.id, { onDelete: "cascade" }),
  memberId: integer("member_id").notNull().references(() => members.id, { onDelete: "cascade" }),
  meetingDate: text("meeting_date").notNull(),
  partId: integer("part_id").notNull(),
  partTitle: text("part_title").notNull(),
  assignmentRole: text("assignment_role", { enum: ["main", "assistant"] }).notNull(),
  hall: text("hall", { enum: ["main", "second"] }).notNull(),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  voidedAt: text("voided_at"),
  voidedBy: integer("voided_by").references(() => users.id),
  voidReason: text("void_reason").notNull().default(""),
}, (t) => [index("assignment_history_schedule_idx").on(t.scheduleId), index("assignment_history_member_date_idx").on(t.memberId, t.meetingDate)]);

export const participantImportAudits = sqliteTable("participant_import_audits", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  sourceUrl: text("source_url").notNull(),
  sheetTitle: text("sheet_title").notNull().default(""),
  tabName: text("tab_name").notNull().default(""),
  addedCount: integer("added_count").notNull().default(0),
  updatedCount: integer("updated_count").notNull().default(0),
  unchangedCount: integer("unchanged_count").notNull().default(0),
  skippedCount: integer("skipped_count").notNull().default(0),
  detailsJson: text("details_json").notNull().default("{}"),
  importedBy: integer("imported_by").notNull().references(() => users.id),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

export const authAccounts = sqliteTable("auth_accounts", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  userId: integer("user_id").references(() => users.id),
  username: text("username").notNull(),
  displayName: text("display_name").notNull(),
  passwordHash: text("password_hash"),
  passwordSalt: text("password_salt"),
  passwordIterations: integer("password_iterations").notNull().default(210000),
  status: text("status", { enum: ["requested", "active", "disabled"] }).notNull().default("requested"),
  mustChangePassword: integer("must_change_password", { mode: "boolean" }).notNull().default(false),
  failedAttempts: integer("failed_attempts").notNull().default(0),
  lockedUntil: text("locked_until"),
  requestNote: text("request_note").notNull().default(""),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  approvedAt: text("approved_at"),
}, (t) => [
  uniqueIndex("auth_accounts_username_unique").on(t.username),
  uniqueIndex("auth_accounts_user_id_unique").on(t.userId),
  index("auth_accounts_status_idx").on(t.status),
]);

export const authSessions = sqliteTable("auth_sessions", {
  tokenHash: text("token_hash").primaryKey(),
  accountId: integer("account_id").notNull().references(() => authAccounts.id),
  expiresAt: text("expires_at").notNull(),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
}, (t) => [index("auth_sessions_account_idx").on(t.accountId)]);

// Expo push tokens registered by the native mobile app, one row per
// account+device. Used to send real push notifications (e.g. "new schedule
// published") via Expo's push service.
export const pushTokens = sqliteTable("push_tokens", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  accountId: integer("account_id").notNull().references(() => authAccounts.id, { onDelete: "cascade" }),
  expoPushToken: text("expo_push_token").notNull(),
  platform: text("platform", { enum: ["ios", "android"] }).notNull().default("android"),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
}, (t) => [
  uniqueIndex("push_tokens_token_unique").on(t.expoPushToken),
  index("push_tokens_account_idx").on(t.accountId),
]);

// Single-row app-wide settings, currently used for configurable role
// permissions (admin can change these anytime without an app update).
export const appSettings = sqliteTable("app_settings", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  // When true, accounts with role "scheduler" may add/edit/delete
  // participants. Defaults to false (view-only) per product decision.
  schedulerCanEditParticipants: integer("scheduler_can_edit_participants", { mode: "boolean" }).notNull().default(false),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
});

export const weekendSchedules = sqliteTable("weekend_schedules", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  monthKey: text("month_key").notNull(),
  monthLabel: text("month_label").notNull(),
  status: text("status", { enum: ["draft", "published"] }).notNull().default("draft"),
  assignmentsJson: text("assignments_json").notNull().default("[]"),
  ownerId: integer("owner_id").notNull().references(() => users.id),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  updatedAt: text("updated_at").notNull().default(sql`CURRENT_TIMESTAMP`),
  deletedAt: text("deleted_at"),
  deletedBy: integer("deleted_by").references(() => users.id),
  deleteReason: text("delete_reason").notNull().default(""),
}, (t) => [uniqueIndex("weekend_schedules_month_unique").on(t.monthKey)]);

export const weekendEligibility = sqliteTable("weekend_eligibility", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  memberId: integer("member_id").notNull().references(() => members.id, { onDelete: "cascade" }),
  quarry: text("quarry", { enum: ["A", "B"] }).notNull(),
  duty: text("duty", { enum: ["chairman", "reader"] }).notNull(),
}, (t) => [uniqueIndex("weekend_eligibility_unique").on(t.memberId, t.quarry, t.duty)]);

export const hostGroups = sqliteTable("host_groups", {
  id: integer("id").primaryKey({ autoIncrement: true }),
  name: text("name").notNull(),
  active: integer("active", { mode: "boolean" }).notNull().default(true),
  createdAt: text("created_at").notNull().default(sql`CURRENT_TIMESTAMP`),
}, (t) => [uniqueIndex("host_groups_name_unique").on(t.name)]);
