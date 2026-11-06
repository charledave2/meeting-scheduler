-- Add application-wide permissions without rebuilding the users table.
-- Rebuilding users breaks existing foreign-key references on Cloudflare D1.
CREATE TABLE IF NOT EXISTS `app_settings` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`scheduler_can_edit_participants` integer DEFAULT false NOT NULL,
	`updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
-- Data migration: the old two-role system used "admin" | "member". The new
-- three-tier system is "admin" | "scheduler" | "viewer". Existing "member"
-- accounts become "viewer" (read-only), preserving their current behavior
-- exactly (the old "member" role was already view-only everywhere).
UPDATE `users` SET `role` = 'viewer' WHERE `role` = 'member';--> statement-breakpoint
-- Seed the single app_settings row if it doesn't exist yet, so the app can
-- always assume exactly one row is present.
INSERT INTO `app_settings` (`scheduler_can_edit_participants`)
SELECT false WHERE NOT EXISTS (SELECT 1 FROM `app_settings`);
