ALTER TABLE `schedules` ADD COLUMN `deleted_at` text;
--> statement-breakpoint
ALTER TABLE `schedules` ADD COLUMN `deleted_by` integer;
--> statement-breakpoint
ALTER TABLE `schedules` ADD COLUMN `delete_reason` text DEFAULT '' NOT NULL;
--> statement-breakpoint
ALTER TABLE `assignment_history` ADD COLUMN `voided_at` text;
--> statement-breakpoint
ALTER TABLE `assignment_history` ADD COLUMN `voided_by` integer;
--> statement-breakpoint
ALTER TABLE `assignment_history` ADD COLUMN `void_reason` text DEFAULT '' NOT NULL;
--> statement-breakpoint
ALTER TABLE `weekend_schedules` ADD COLUMN `deleted_at` text;
--> statement-breakpoint
ALTER TABLE `weekend_schedules` ADD COLUMN `deleted_by` integer;
--> statement-breakpoint
ALTER TABLE `weekend_schedules` ADD COLUMN `delete_reason` text DEFAULT '' NOT NULL;
