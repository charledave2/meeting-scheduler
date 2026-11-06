# Soft Delete Published Schedules

This update adds a safer delete flow for published schedules.

## Behavior

- Admin can delete/archive a published Midweek schedule.
- Deleted schedules are hidden from normal saved schedule lists.
- Deleted schedules can no longer be opened by normal `GET /api/schedules?id=...`.
- Assignment history rows are not physically removed. They are marked voided.
- Rotation monitor excludes deleted schedules, so each affected member's counter is recalculated automatically.
- Weekend monthly schedules now use the same archive behavior instead of hard delete.

## Data Columns Added

Schedules:

- `deleted_at`
- `deleted_by`
- `delete_reason`

Assignment history:

- `voided_at`
- `voided_by`
- `void_reason`

Weekend schedules:

- `deleted_at`
- `deleted_by`
- `delete_reason`

## Migration

Added:

```text
drizzle/0012_soft_delete_published_schedules.sql
```

The app also includes a runtime storage guard:

```text
app/schedule-soft-delete-storage.ts
```

This helps existing D1 databases add missing columns safely when the route is used.

## Rotation Logic

The rotation API now only reads active schedules:

```text
schedules.deleted_at IS NULL
```

So when a published schedule is deleted:

```text
schedule archived
assignment rows voided
rotation calculation ignores that schedule
```

This means the participant's monthly count, last-assigned date, and priority/recommended status are recalculated from active schedules only.

## Validation

Passed:

```bat
npm run build
```

Note:

```bat
npx tsc --noEmit
```

still reports pre-existing Cloudflare ambient type issues unrelated to this change:

- `D1Database`
- `Fetcher`
- `JsonWebKey.kid`
- `env.IMAGES`
