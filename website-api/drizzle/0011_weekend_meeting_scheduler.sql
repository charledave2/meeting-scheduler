CREATE TABLE IF NOT EXISTS `weekend_schedules` (
  `id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
  `month_key` text NOT NULL,
  `month_label` text NOT NULL,
  `status` text DEFAULT 'draft' NOT NULL,
  `assignments_json` text DEFAULT '[]' NOT NULL,
  `owner_id` integer NOT NULL,
  `created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
  `updated_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
  FOREIGN KEY (`owner_id`) REFERENCES `users`(`id`)
);
--> statement-breakpoint
CREATE UNIQUE INDEX IF NOT EXISTS `weekend_schedules_month_unique` ON `weekend_schedules` (`month_key`);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS `weekend_eligibility` (
  `id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
  `member_id` integer NOT NULL,
  `quarry` text NOT NULL,
  `duty` text NOT NULL,
  FOREIGN KEY (`member_id`) REFERENCES `members`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE UNIQUE INDEX IF NOT EXISTS `weekend_eligibility_unique` ON `weekend_eligibility` (`member_id`,`quarry`,`duty`);
--> statement-breakpoint
CREATE TABLE IF NOT EXISTS `host_groups` (
  `id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
  `name` text NOT NULL,
  `active` integer DEFAULT true NOT NULL,
  `created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL
);
--> statement-breakpoint
CREATE UNIQUE INDEX IF NOT EXISTS `host_groups_name_unique` ON `host_groups` (`name`);
--> statement-breakpoint
INSERT OR IGNORE INTO `host_groups` (`name`) VALUES ('Group 1'),('Group 2'),('Group 3'),('Group 4'),('Group 5'),('Group 6'),('Group 7'),('Group 8'),('Group 9');
