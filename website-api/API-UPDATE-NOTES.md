# Android participant API update

The Android v0.17.0 participant screen uses these API additions:

- `members.unbaptized` boolean, added by `drizzle/0013_member_unbaptized.sql`
- Admin `GET /api/members?includeInactive=true` includes inactive rows and returns `isAdmin`
- `PATCH /api/members` accepts `unbaptized` and admin-only `active`
- `DELETE /api/members?id=ID` permanently removes a member (and cascades their assignment history); admin only

The Android app and website API source need to be updated together. The current live Worker has not been deployed from this workspace.
