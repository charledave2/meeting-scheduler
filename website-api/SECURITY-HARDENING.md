# Backend role-security hardening

Updated: 2026-08-22

## Implemented

- Viewer remains read-only for schedules and participants.
- Viewer is blocked from workbook import and administrative settings APIs.
- Scheduler participant management fails closed when the permission setting cannot be read.
- All mutating `/api` requests reject explicit cross-site browser requests while remaining compatible with the native Expo client.
- Account actions are runtime allow-listed; unknown actions can no longer fall through to account enablement.
- Role changes revoke all sessions for the affected account so the new role takes effect at the next sign-in.
- Password changes revoke old sessions and issue one fresh session to the current device.
- Native `POST /api/auth/logout` now deletes the server session; legacy web GET logout remains supported.
- Restore requests have a 2 MB payload limit, per-table row limits, strict JSON handling, and preserve Scheduler roles without allowing an imported non-owner Admin.

## Deployment note

The Weekend Meeting module adds D1 migration `0011_weekend_meeting_scheduler.sql`. Apply it before deploying the updated Worker.

## Weekend Meeting module

- Monthly Schedule Cards
- Quarry A mapped to Sundays
- Quarry B mapped to Saturdays
- Separate Chairman and Watchtower Reader assignments per Quarry
- One required Host Group plus an optional second Host Group
- Draft/published visibility with Viewer accounts limited to published schedules
- Admin-managed eligibility per Quarry and duty

## Recommended Cloudflare follow-up

Configure Cloudflare rate limiting for login and account-request endpoints. Keep this at the edge so limits can use trusted client IP and cannot add unbounded rows to D1.
