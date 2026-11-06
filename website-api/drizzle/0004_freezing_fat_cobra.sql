ALTER TABLE `members` ADD `gender` text DEFAULT 'unspecified' NOT NULL;
--> statement-breakpoint
UPDATE `members` SET `gender` = 'brother' WHERE `appointment` IN ('elder', 'ministerial');
