# Changelog

## v0.35.0 — 2026-11-07

- Show each account's cloud-stored photo in the profile image at the top of the burger menu.
- Let each signed-in user upload or replace their own profile photo to the shared Cloudflare D1 photo store.
- Added an authenticated `/api/auth/me` photo read and upload endpoint.
- Published the APK and Android source archive as GitHub Release assets.

## v0.34.0 — 2026-11-07

- Removed circular fills from the Participants Add and Sort icons while preserving matching 40 dp touch targets.
- Reworked Export Text to follow the official schedule order with section headings, times, durations, details, roles, hall assignments, and closing program.
- Export Text now creates a UTF-8 .txt attachment and opens Android's share sheet for direct sharing to other apps.
- Published the APK and Android source archive as GitHub Release assets.

## v0.33.0 — 2026-11-07

- Fixed the sign-in failure on devices where Android Keystore cannot generate an AES session key.
- Added app-private AES-GCM fallback key storage for affected devices while retaining Android Keystore as the primary option.
- Included clearer diagnostics for login API and session verification failures.
- Published the APK and Android source archive as GitHub Release assets.

## v0.32.0 — 2026-11-07

- Added clear sign-in diagnostics to distinguish login API failures from session verification failures; Android network permissions were confirmed in the manifest.
- Diagnostic messages show the exception type and safe server/network details without exposing passwords or session tokens.
- Published the APK and Android source archive as GitHub Release assets.

## v0.31.0 — 2026-11-07

- Updated Participants Add, Filter & Sort, and Edit controls to matching-size Google Material You Two Tone icons.
- Replaced the Filter & Sort text label with its Sort icon.
- Removed status filter chips and account labels from the Accounts & Access photo grid.
- Added an Administrator badge to the admin account photo.
- Added blank values for Appointment and Service Status so publisher-only participants can keep these details empty.
- Published the APK and Android source archive as GitHub Release assets.

## v0.29.0 — 2026-11-07

- Added role-based access restrictions for Admin, Scheduler, and Viewer accounts.
- Added in-app temporary-password change flow.
- Refined Accounts & Access with compact account details, request notification dots, and menu-based actions.
- Updated Participants refresh control to a Google Material Two Tone icon.
- Added blank “Select” choices for participant appointment and service status, with API support for clearing either value.
- Packaged the Android source ZIP and debug APK as release assets.

## v0.30.0 — 2026-11-07

- Replaced account summary rows with a photo grid and account details modal.
- Added cloud stored account photos and profile actions inside the details modal.
- Added Two Tone icons to Participants add, filter and edit controls with consistent sizing.
- Styled account deletion actions in red and account disabling actions in orange.
- Packaged the APK and Android source archive as GitHub Release assets.
