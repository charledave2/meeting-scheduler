Warning: truncated output (original token count: 4962)
Total output lines: 372

# Changelog

## v0.45.28 — 2026-11-24

- Add a review dialog showing new, changed, and removed assignments before cloud save.
- Compare edits with the saved schedule and let the user return to assignments before confirming.

## v0.45.27 — 2026-11-24

- Show assignment completion progress and the exact required slots still missing.
- Keep Group optional and prevent cloud saves until required assignments are complete.

## v0.45.26 — 2026-11-24

- Update the launcher icon and Android 12+ launch screen to match the selected palette.
- Use palette-aware colors and contrast for the animated splash and workbook capture log.
- Add bottom breathing room so the Workbook action remains reachable above navigation.
- Use consistent English copy for workbook import success.

## v0.45.25 — 2026-11-24

- Recolor the in-app brand icon from the selected palette in the header, sign-in, splash, and app-lock screens.

## v0.45.24 — 2026-11-24

- Add four selectable color palettes: Tonal Spot, Neutral, Vibrant, and Expressive.
- Save the chosen palette on the device and apply it across light and dark themes.

## v0.45.23 — 2026-11-23

- Apply Google Sans to the Workbook capture log.
- Allow the centered Home preview to dismiss when tapping outside the card.

## v0.45.22 — 2026-11-23

- Use bundled Google Sans in the app, schedule previews, PNG exports, and assignment slips.
- Keep the Home schedule preview centered and fit the full page to the screen.
- Allow active brothers to be picked for Bible Reading, including those marked unbaptized. Show why unavailable matches cannot be selected.
- Keep Android sign-in sessions across app restarts when Keystore is unavailable.
- Clarify participant options and notification test results, and use consistent English copy in sign-in and biometric screens.

## v0.45.21 — 2026-11-23

- Recover from unusable Android Keystore keys during sign-in. If secure storage is unavailable, keep the session in memory until Android closes the app process.
- Improve typography, navigation, and schedule previews on phones and tablets.

## v0.45.20 — 2026-11-21

- Fix Schedule History sorting for weeks that cross into January, including Dec 28 – Jan 3, 2027.
- Prepare Monday and Wednesday assignment FCM reminders at 08:00 Philippine time; the Cloudflare cron trigger must be enabled before automatic sends start.

## v0.45.19 — 2026-11-21

- Show a small colored dot followed by the part number and period in the midweek schedule preview and PNG, matching the reference.
- Remove assignment slip PDF export and keep individual A4 PNG slips.

## v0.45.18 — 2026-11-20

- Export each assignment slip as its own full-size portrait A4 PNG, sharing multiple image files when needed.
- Remove pale green field fills and dashed underlines from assignment slips; use a clean thin solid line.
- Show a balanced colored part number without a circular badge in the schedule preview and PNG.

## v0.45.17 — 2026-11-20

- Show each part's number, title, and duration on portrait assignment slips in PDF and PNG; keep long labels readable across two lines.
- Put the part number inside the colored circle in the midweek schedule preview and PNG export.

## v0.45.16 — 2026-11-20

- Add portrait PNG image export for assignment slips, four slips per A4 page; use the same layout for PDF.
- Include Chairman, Opening Prayer, Second Hall Adviser, and every assigned main or second-hall part, without a separate Closing Prayer slip.
- Label Parts 1, 2, and 8 by their meeting section or title; leave Assistant blank without a partner.

## v0.45.15 — 2026-11-20

- Add printable assignment slip PDF export to Preview & Export, with two slips per A4 page.
- Fill each slip from its assigned participant, optional assistant, Thursday meeting date, part number, and Main Hall or Second Hall.
- Leave the assistant value blank when no partner is assigned.

## v0.45.14 — 2026-11-16

- Open the exact saved schedule preview when an assignment notification is tapped, including when the app starts from the notification.
- Keep test notifications opening the app normally.

## v0.45.13 — 2026-11-16

- Add a test FCM notification button to Account & Settings for every signed-in account.
- Use the available A4 page width in schedule preview and PNG export on tablets.

## v0.45.12 — 2026-11-15

- Fix Account & Settings loading being cancelled during screen transitions.
- Load accounts and active participants together so the participant link picker receives the list reliably.

## v0.45.11 — 2026-11-15

- Add participant linking to the Android admin account screen for FCM assignment notifications.
- Improve Gradle dependency resolution by using the official Google and Maven Central repositories first.

## v0.45.9 — 2026-11-14

- Use Thursday as this congregation's midweek meeting date when choosing the next Home meeting.
- Keep the schedule preview and PNG export on the bundled Google Sans Flex font.
## v0.45.8 — 2026-11-14

- Choose the next Home meeting by its Wednesday meeting date, so a week whose midweek meeting has passed is no longer shown as upcoming.
- Show the actual midweek meeting date on upcoming schedule cards.
## v0.45.7 — 2026-11-13

- Keep saved Home, participant, and history data visible while refreshing, with clear loading, retry, and empty states.
- Highlight the next meeting on Home and add accessible preview actions.
- Cache participant sorting and search results between changes, and bring key controls to 48 dp touch targets.
- Fit the complete A4 preview in the Home preview modal in both orientations; keep PNG dimensions and quality unchanged.
## v0.45.6 — 2026-11-14

- Fit the entire schedule into a consistent A4 page in Preview & Export and shared PNGs, preventing long schedules from being clipped.
- Keep portrait and landscape previews proportional to the fixed 2480 × 3508 PNG canvas.
- Refine the screen transition animation with a smoother, more visible motion.

## v0.45.5 — 2026-…1962 tokens truncated…itHub releases when the app opens.
- Added an in-app update prompt that downloads the signed APK through Android Downloads and hands installation to Android.

## v0.37.5 — 2026-11-09

- Applied the first forest-green calendar icon concept to the Android launcher, splash animation, and sign-in page.

## v0.37.4 — 2026-11-09

- Limited the independently scrolling preview viewport to landscape mode.
- Restored the original portrait preview height, preserving PNG export dimensions and image quality.

## v0.37.3 — 2026-11-09

- Kept the preview header and export controls outside the scrolling region.
- Let the schedule WebView use the remaining screen height and handle its own vertical scrolling in portrait and landscape.

## v0.37.2 — 2026-11-08

- Fixed the clipped schedule preview by giving it a screen-sized viewport with its own working vertical scroll, in portrait and landscape.
- Kept the export controls visible above the independently scrolling schedule preview.
- Published the signed APK and Android source archive as GitHub Release assets.

## v0.37.1 — 2026-11-08

- Fixed the schedule preview in landscape: the embedded page now reloads and recalculates its height when the available width changes.
- Corrected Compose font scale initialization for the current Android build setup.
- Applied safe drawing insets so app content and the navigation drawer stay clear of display cutouts, status bars, navigation bars, and curved screen edges.
- Renamed Accounts & Access to Account & Settings and added a persistent app-wide font size setting.
- Published the signed APK and Android source archive as GitHub Release assets.

## v0.36.1 — 2026-11-08

- Kept the navigation drawer usable in landscape by making its tool list scroll independently from the fixed theme and sign-out controls.
- Prevented activity recreation during orientation and screen-size changes, preserving the current page and signed-in session instead of returning to the splash screen.
- Published the signed APK and Android source archive as GitHub Release assets.


## v0.36.0 — 2026-11-07

- Added per-account tool visibility and permission controls for Scheduler and Viewer accounts.
- Administrators can set each tool to Hidden, View only, or Edit; Viewers are always limited to read-only access.
- Hidden tools are removed from the navigation menu, and view-only screens disable editing and export actions.
- Applied the per-account access rules to workbook import, participant viewing/editing, and schedule API access.
- Published the signed APK and Android source archive as GitHub Release assets.


## v0.35.1 — 2026-11-07

- Build GitHub releases as APKs signed with the stable Android release keystore.
- Keep debug APKs as separate test artifacts.
- Document release keystore creation and GitHub Actions secret setup.

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


