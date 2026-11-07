# Android release signing

GitHub release builds are signed with one long-lived Android release keystore. Keep a secure offline backup of the keystore and its passwords. Never commit the keystore or passwords to this repository. If the keystore is lost, Android will reject an update signed with a replacement key.

## Create the keystore on Windows

Run from a private folder on a computer with Java installed. Keep the .jks file outside the repository:

```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\meeting-scheduler-release.jks" -keyalg RSA -keysize 2048 -validity 10000 -alias meeting-scheduler
```

Choose a strong keystore password and key password, and store them safely. Do not send them in chat. The alias in this command is meeting-scheduler.

## Add GitHub Actions repository secrets

In the GitHub repository, open **Settings → Secrets and variables → Actions → New repository secret**. Add:

- ANDROID_KEYSTORE_BASE64: in PowerShell, run this command to copy the keystore as one Base64 string, then paste it into the secret:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\meeting-scheduler-release.jks")) | Set-Clipboard
  ```
- ANDROID_KEYSTORE_PASSWORD: keystore password
- ANDROID_KEY_ALIAS: meeting-scheduler
- ANDROID_KEY_PASSWORD: key password

The workflow decodes the keystore only during a release build, signs app-release.apk, then removes the temporary file.

## Publish a release

Increment both versionCode and versionName in android/app/build.gradle.kts, update CHANGELOG.md, and commit the release with a message beginning Release:. The workflow builds and attaches the signed release APK and Android source ZIP to the matching GitHub Release. Ordinary builds continue to produce a debug APK artifact for testing.

Use the same release keystore for every update to this package. A debug-signed installation and the new release-signed APK use different keys, so Android may require uninstalling the old debug app once before installing the first signed release. Uninstalling can clear device-local app data; cloud data remains on the server.