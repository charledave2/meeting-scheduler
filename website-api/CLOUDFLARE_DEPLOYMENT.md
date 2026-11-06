# Deploy Meeting Scheduler to your Cloudflare account

This project can run as a Cloudflare Worker with its own D1 database and native username/password accounts. Cloudflare Access is used only once to verify the first admin setup.

## Upgrade an existing deployment to native accounts

Keep the Worker URL **Restricted** while doing the upgrade. The configured `ADMIN_EMAIL` must match the email you use through Cloudflare Access.

```cmd
npm ci
npx vinext build
npx wrangler d1 migrations apply meeting-scheduler-db --remote --config wrangler.cloudflare.jsonc
npx wrangler deploy --config wrangler.cloudflare.jsonc
```

Visit `/setup` on your Worker URL while signed in through Cloudflare Access, then create your admin username and password. Test the native login in a private/incognito window. Only after it works, change the Worker URL from **Restricted** to **Public**. The app will then protect itself with its own login page.

New users choose **Request access**. The admin opens **Accounts**, approves the request, and gives the user a temporary password. The user must change it on first sign-in. The admin also handles password resets from **Accounts**.

## 1. Sign in and create the database

```bash
npm ci
npx wrangler login
npx wrangler d1 create meeting-scheduler-db --location apac
```

Copy `wrangler.cloudflare.example.jsonc` to `wrangler.cloudflare.jsonc`. Put the database ID returned by Wrangler in `database_id`, and set `ADMIN_EMAIL` to the email that should have admin controls.

## 2. Build, initialize D1, and deploy

```bash
npm run build
npx wrangler d1 migrations apply meeting-scheduler-db --remote --config wrangler.cloudflare.jsonc
npx wrangler deploy --config wrangler.cloudflare.jsonc
```

Wrangler will print the new `workers.dev` URL.

## 3. Require login with Cloudflare Access

In Cloudflare Dashboard:

1. Open **Workers & Pages**, select **meeting-scheduler**, then open **Settings → Domains & Routes**.
2. On the `workers.dev` route, choose **Enable Cloudflare Access**.
3. In the Access policy, allow the email addresses that may use the scheduler. One-time PIN is the simplest login method.
4. Copy the Access application **AUD tag** and your team domain into `CF_ACCESS_AUD` and `CF_ACCESS_TEAM_DOMAIN` in `wrangler.cloudflare.jsonc`.
5. Run `npx wrangler deploy --config wrangler.cloudflare.jsonc` again.

The app verifies the signed Access JWT before trusting the user's email. The email matching `ADMIN_EMAIL` becomes the admin; other allowed emails become regular members.

## 4. Copy existing data

From the current app, use **Data → Export complete backup** before switching. Keep the JSON file safe. After the new Worker and login are verified, sign in as the admin and use **Data → Restore backup**. Restore merges participants, users, schedules, and import history into the new D1 database and skips matching schedule/history records on repeat runs.
