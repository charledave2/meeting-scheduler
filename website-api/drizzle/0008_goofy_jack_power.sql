CREATE TABLE `assignment_history` (
	`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL,
	`schedule_id` integer NOT NULL,
	`member_id` integer NOT NULL,
	`meeting_date` text NOT NULL,
	`part_id` integer NOT NULL,
	`part_title` text NOT NULL,
	`assignment_role` text NOT NULL,
	`hall` text NOT NULL,
	`created_at` text DEFAULT CURRENT_TIMESTAMP NOT NULL,
	FOREIGN KEY (`schedule_id`) REFERENCES `schedules`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`member_id`) REFERENCES `members`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `assignment_history_schedule_idx` ON `assignment_history` (`schedule_id`);--> statement-breakpoint
CREATE INDEX `assignment_history_member_date_idx` ON `assignment_history` (`member_id`,`meeting_date`);