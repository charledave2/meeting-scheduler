# Meeting Scheduler Android

Native Android Material 3 starter app for the Meeting Scheduler workspace.

## Build

With Android SDK Platform 35, Build Tools 35.0.0, JDK 17+, and Gradle 8.10.2+:

```sh
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Current scope

Version 0.15.0 adds two-tone Material You icon containers in navigation and connects schedule publishing/history and participant retrieval to the live website APIs. Schedule saves are kept locally and posted to `/api/schedules`; history reads `/api/schedules`, and Participants loads `/api/members`. Website session cookies are stored encrypted using Android Keystore. Cloud write permissions remain enforced by the Worker API; the signed-in account needs the website's scheduler/admin permission to publish schedules.


Version 0.16.0 makes the Participants page denser, adds name search and filters/sorting for gender, appointment, and service status, and opens a Material You edit dialog. Participant edits are PATCHed to the website API. The current API schema does not include an unbaptized status, so that option is identified as unsupported instead of being stored inaccurately.

Version 0.17.0 adds a baptism status control and filter, participant active/inactive controls, and admin-only permanent deletion with confirmation. The Android client now uses `PATCH /api/members` for `unbaptized` and `active`, `GET /api/members?includeInactive=true` for admin status views, and `DELETE /api/members?id=...` for permanent deletion. These API additions must be deployed with the website's included `0013_member_unbaptized.sql` migration before they can persist against the currently live Worker.

Version 0.18.0 fills Material 3 secondary/tertiary container roles from the app's sage/forest palettes to prevent default lavender surfaces, lazy-renders the Participants list, caches cloud lists between page visits, and shortens page transition timings. Accounts & access loads `/api/accounts` and supports admin approval/decline, role changes, temporary password resets, and account enable/disable actions.

Version 0.19.0 makes Accounts & access compact, removes account search, and adds status filter chips with status-first ordering. Schedule history now shows only database schedules from `/api/schedules`, with View for signed-in users and admin-only Edit/Delete actions. New schedules are saved locally as drafts; the explicit Save to cloud button appears only for admin accounts. Schedule editing and deletion use the existing admin-only schedule API endpoints.


Version 0.20.0 sorts cloud history by schedule date newest first, restores full saved cloud records before showing the preview, and checks for an existing cloud schedule after the selected meeting date changes. The Assignments page waits until the cloud lookup or workbook import finishes so an already-saved schedule is populated before editing.

Version 0.21.0 parses English and Tagalog meeting week labels for date-based history sorting and cloud schedule matching. It prevents a background workbook lookup from replacing a cloud schedule being viewed, clears stale local meeting-role and assignment values when starting a week without a saved schedule, normalizes lowercase API participant eligibility values, and loads the full cloud participant list before enabling Assignments.

Version 0.22.0 removes the duplicate centered title from the app bar, pins the admin account to the top of Accounts & access, and adds Material role badges for admin and scheduler accounts. Admins can permanently remove non-admin login accounts after confirmation; the website worker must first receive the included DELETE `/api/accounts` route patch. The sign-in screen now uses a compact Material 3 card with password visibility and clearer demo access.
