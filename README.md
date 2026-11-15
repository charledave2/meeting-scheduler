# Meeting Scheduler

Private monorepo for the native Android app and its Cloudflare Workers website/API.

## Projects

- `android/` — native Kotlin Android app using Material 3.
- `website-api/` — Vinext app and Cloudflare Worker API backed by D1.

## Deploy the API to Cloudflare

The GitHub Actions workflow deploys `website-api/` when changes are pushed to `main` (and can also be started manually from Actions). Before the first deploy, add these GitHub Actions repository secrets:

- `CLOUDFLARE_API_TOKEN` — a token with permission to deploy Workers and access the configured D1 database.
- `CLOUDFLARE_ACCOUNT_ID` — the Cloudflare account ID.

Then push to `main` or run **Deploy Cloudflare Worker** from the Actions tab. The workflow runs `npm ci`, builds the Worker, then deploys with `wrangler.cloudflare.jsonc`.

The Wrangler config contains the existing D1 database binding and Access application settings. The workflow does not run database migrations automatically. Apply any pending SQL migrations deliberately before deploying a schema-dependent change.

### Firebase Cloud Messaging

For server-side assignment notifications, set these as **Worker secrets** in Cloudflare (never commit the private key):

- `FIREBASE_SERVICE_ACCOUNT_EMAIL` — the service account email for the Firebase project.
- `FIREBASE_SERVICE_ACCOUNT_PRIVATE_KEY` — its PEM private key, stored as a secret. The Worker accepts either real newlines or escaped `\\n` sequences.

The Worker sends through the FCM HTTP v1 API using the `jwmeeting-scheduler` project by default. The service account must be allowed to send Firebase Cloud Messaging messages. Until both secrets are configured, schedule saves still succeed but FCM delivery is skipped. Android Firebase configuration is included for both build IDs: the release app uses `android/app/google-services.json`, and the preview app uses `android/app/src/debug/google-services.json`.

## Android build

Open `android/` in Android Studio, or build with JDK 17+, Android SDK Platform 35, and Gradle 8.10.2+:

```sh
cd android
gradle :app:assembleDebug
```

The app uses the existing production API URL configured in `android/app/src/main/java/com/example/meetingscheduler/SchedulerApi.kt`.
