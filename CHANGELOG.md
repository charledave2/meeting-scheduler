# Changelog

## v0.45.38 — 2026-11-25

- Add an About this App page with app version, changelog, contributor and developer links, repository link, privacy notes, and accurate license information.
- Remove the launcher mark from Android's launch splash so the app opens into its existing animated intro without a duplicate logo screen.

## v0.45.37 — 2026-11-25

- Keep theme and palette changes immediate while deferring launcher icon alias updates until the app is in the background; batch alias changes atomically on Android 13 and newer.

## v0.45.36 — 2026-11-25

- Add System, Light, and Dark appearance options; System follows Android's current theme while saved Light or Dark choices remain selected.
- Record successful sign-ins and show the last login time for other accounts in the admin account details.
- Default Workbook to the current Monday-based meeting week and migrate the old fixed default when no saved history depends on it.
- Add a “This Week” shortcut after choosing another Workbook week.
- Make the required assignment slots still missing easier to spot.

## v0.45.35 — 2026-11-24

- Replace the bottom Settings tab with a Profile shortcut and move account settings into its profile menu.
- Move profile photo and display-name editing into Account & Settings.
- Keep Profile available in the rightmost navigation position for accounts with different tool access.

## v0.45.34 — 2026-11-24

- Match the system splash, status bar, and navigation bar to the saved app theme and selected palette.
- Correct Tonal Spot launch colors and keep each splash color aligned with its app background.
- Update the launcher theme when either the palette or dark-mode setting changes.

## v0.45.33 — 2026-11-24

- Show participant rotation status in the assignment picker while preserving eligibility and rotation rules.
- Improve participant row dividers and make the picker surface follow the selected palette.
- Match the Android startup splash and animated splash background to the selected palette in light and dark appearances.

## v0.45.32 — 2026-11-24

- Replace the overlapping participant dropdown with a responsive searchable selection dialog.
- Simplify participant rows to names only and keep rotation checks as a brief confirmation when needed.

## v0.45.31 — 2026-11-24

- Paginate Schedule History from the cloud and load older schedules on demand.
- Show Participants in batches of 50 with the existing search and filters applied.
- Defer downloading participant data until Workbook, Assignments, or Participants is opened.

## v0.45.30 — 2026-11-24

- Detect cloud schedule edits or deletions made by another admin before saving.
- Prevent stale edits from overwriting newer cloud data and offer to reload the latest version.

## v0.45.29 — 2026-11-24

- Add Schedule History search across meeting week, Bible reading, and assigned participant names.
- Add year filters and sort schedules by their explicit meeting-week date, newest first.

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

## v0.45.5 — 2026-11-14

- Show visible progress and success or error feedback while saving schedules, importing workbooks, and preparing exports.
- Disable repeat actions while a save or export is in progress.

## v0.45.4 — 2026-11-13

- Use the bundled Google Sans font in both schedule preview and PNG export.
- Remove the “Nabuo sa JW Meeting Scheduler” footer from schedule exports.
- Keep PNG output at a lossless 2480 × 3508 pixels with 300 DPI metadata.
- Add a subtle forest-green abstract design to the Home Page greeting card.

## v0.45.3 — 2026-11-12

- Fixed shared PNG content scaling so the schedule fills the fixed A4 canvas across device densities.
- Kept the export font, page layout, dimensions, and preview rendering unchanged.

## v0.45.2 — 2026-11-12

- Fixed PNG export rendering by attaching the temporary high-resolution WebView to the Activity window before capture, then removing it after rendering.
- Kept the fixed A4/300 DPI PNG output and chooser success popup behavior.

## v0.45.1 — 2026-11-12

- Fixed PNG sharing so the generated image is granted correctly to apps in the Android Sharesheet.
- Routed PNG and Export Text through a tracked chooser result and show a success popup after the user returns from a selected share target.
- Kept the success popup hidden when the Sharesheet is cancelled without choosing an app.

## v0.45.0 — 2026-11-12

- Standardized schedule PNG exports to a fixed A4 portrait canvas at 300 DPI (2480 × 3508) on phones and tablets.
- Rendered exports with a pinned, SHA-256-verified Google Sans Flex font so typography no longer falls back differently by device and works offline.
- Kept export rendering separate from the on-screen preview.


## v0.44.1 — 2026-11-12

- Removed the theme switch from the profile popover; theme selection remains in Account & Settings.
- Placed Save and Sign out side by side at the bottom as compact, label-free icons.
- Removed the sign-out icon’s solid background and kept its icon in the theme’s error color.

## v0.44.0 — 2026-11-12

- Replaced the centered profile dialog with a compact popover anchored beside the profile shortcut.
- Reduced profile name field and control sizes; save and sign-out are icon-only, with sign-out highlighted in the theme’s error color.
- Applied the forest-green Material You palette to profile controls and kept the theme switch compact.
- Added a custom animated forest-green splash and press animations for the navigation icons.

## v0.43.0 — 2026-11-12

- Replaced the side drawer with compact forest green bottom navigation for Home, Workbook, History and Account & Settings.
- Added a raised center Tools button with a springy tap animation that opens Assignments, Preview & Export and Participants.
- Shows only destinations each account can access; preserves the existing profile editor, theme and biometric lock settings.
- Kept the existing app identity and signing configuration so the update installs over the previous official version with its existing app data.


## v0.42.1 — 2026-11-11

- Moved the theme switcher and biometric lock from the profile dialog to Account & Settings for every signed-in role.
- Kept profile editing and sign-out in a compact Profile dialog.

## v0.42.0 — 2026-11-11

- Added optional device-local biometric lock in Profile & preferences, with Android fingerprint/face authentication and phone PIN, pattern or password fallback.
- Require successful authentication to enable or disable the lock. Re-lock on leaving the app and after a fresh launch; cancellation and failed attempts keep the workspace protected.
- Added a forest green lock screen, protected recent-app previews, and retained meeting drafts and update downloads across locking.
- Added security regression tests for restart, background, cancellation, stale callbacks and preference changes.

## v0.41.0 — 2026-11-10

- Renamed the Android app to JW Meeting Scheduler across launcher, drawer, splash, login, update notifications and export branding.
- Centered role labels beneath the profile photo using a shared content width while keeping the shortcut on the left.


## v0.40.2 — 2026-11-10

- Emphasized all drawer account roles with bold uppercase labels aligned beneath the profile photo.
- Styled the profile sign-out action in the theme’s red error color.
- Removed the Appearance heading and compacted profile controls, corners and spacing.


## v0.40.1 — 2026-11-10

- Aligned the drawer’s bottom profile shortcut to the left.
- Standardized profile actions with Material Two Tone icons and a shorter Save label.
- Removed the profile reminder and added equal-width footer actions with stacking for narrow screens or enlarged fonts.


## v0.40.0 — 2026-11-10

- Opened Ano ang Sasabihin Mo assignments to any active brother, including publishers.
- Matched the Android launch animation and in-app splash to the current app icon.
- Replaced the drawer header profile shortcut with the app icon.
- Added a bottom profile photo shortcut with the signed-in account role.
- Grouped photo changes, local display-name editing, appearance and sign-out in a scrollable profile dialog.


## v0.39.0 — 2026-11-10

- Completed forest-green Material3 surface colors for dialogs, inputs, menus and both themes.
- Moved avatar decoding off the UI thread with bounded thumbnail sizes and a shared memory cache.
- Made the navigation drawer fully scrollable and improved keyboard and small-screen dialog sizing.
- Restricted preview WebViews, APK download sources and shared export paths; disabled app backup.
- Revoked server sessions on sign-out and blocked authenticated API use until required password changes are complete.
- Removed unsigned admin bootstrap header trust and disabled caching of API responses.
- Added Android lint checks and backported date APIs for Android 7 compatibility.
- Corrected Android-version-specific theme resources and regenerated the backend dependency lockfile.
- Updated Next.js and its matching ESLint configuration to address dependency security advisories.


## v0.38.1 — 2026-11-10

- Refreshed assignment choices immediately after each selection so a chosen participant disappears from other slots.
- Rechecked for available app updates whenever the app starts or returns to the foreground; dismissing the prompt is temporary.


## v0.38.0 — 2026-11-10

- Prevented a participant from occupying multiple assignment slots in the same schedule.
- Removed legacy duplicate assignments when local and cloud schedules are loaded, then saved the cleaned assignments.


## v0.37.9 — 2026-11-10

- Styled the date picker’s Cancel and Select actions with the forest-green palette in both themes.
- Reduced the assignment search field height and visual weight.
- Hid brothers already assigned elsewhere in the schedule from other participant choices while keeping each field’s current selection visible.
- Added compact Active and Disabled icon badges to account photos, matching the Administrator badge style.


## v0.37.8 — 2026-11-10

- Added a compact, searchable participant picker anchored to each assignment field.
- Hid rotation counts and rotation warnings for elders and ministerial servants, who are exempt from rotation.
- Added Active and Disabled badges to non-administrator accounts.
- Restyled the in-app update dialog with a two-tone icon, download progress, and an in-app APK install handoff.
- Applied the app’s forest-green palette to the Workbook date picker in light and dark themes.


## v0.37.7 — 2026-11-10

- Matched the Home Page upcoming meeting preview height to the portrait Preview & Export schedule viewport.
- Redesigned account tool access controls as compact, clearly labeled permission cards with selected-state indicators.

## v0.37.6 — 2026-11-10

- Added automatic checks for newer public GitHub releases when the app opens.
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


