# Play Store release runbook

Package name: **`com.waystar.myLedger`** (permanent and case-sensitive once uploaded). Debug builds install as `com.waystar.myLedger.debug`.

## 1. Signing (one-time)

Create an **upload key** and keep it out of git (`keystore.properties`, `*.jks` and `*.keystore` are git-ignored). Back up the `.jks` file and its passwords somewhere safe.

```bash
keytool -genkeypair -v -keystore myledger-upload.jks -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

`keystore.properties` at the repo root:

```properties
storeFile=myledger-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

CI can use the environment variables `MYLEDGER_KEYSTORE`, `MYLEDGER_KEYSTORE_PASSWORD`, `MYLEDGER_KEY_ALIAS` and `MYLEDGER_KEY_PASSWORD` instead. Without either, release builds come out unsigned.

In Play Console, enrol in **Play App Signing**. Google holds the app signing key and you upload with the upload key.

## 2. Build

```bash
./gradlew testDebugUnitTest lintRelease bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

Release builds use R8 (minify and resource shrinking). Before each upload, install a release build on a real phone and check the main flows: permission grant/deny, full import, analytics pages.

Increase `versionCode` in `app/build.gradle.kts` by 1 for every upload, on any track.

## 3. Play Console forms

### Permissions Declaration Form (SMS): required, reviewed manually

Choose the exception **"SMS-based money management"** (READ_SMS, RECEIVE_SMS). Google grants this at its discretion. Suggested answers:

- **Core functionality:** "myLedger is a personal budgeting app. It reads M-PESA transaction confirmation SMS to automatically record the user's balance, payments, income and transaction fees, and shows spending analytics. Without SMS access the app has no data and cannot function."
- **Why there is no alternative:** "M-PESA does not provide an API that gives a user's own transaction history to third-party apps automatically. The confirmation SMS is the only real-time, on-device record of each transaction."
- **Data handling:** "Only messages from the M-PESA sender ID are queried (`content://sms/inbox` filtered by sender). Live messages from other senders are discarded on receipt. All data is stored on the device only. There is no network transmission of SMS data, and cloud backup and device transfer are disabled."
- Attach a short screen recording of the disclosure screen, the system prompt and the resulting dashboard. The form may not require it, but it helps the review.

The store description must make SMS-based tracking prominent. The policy requires the core functionality to be "prominently documented and promoted in the app's description".

### Privacy policy (GitHub Pages)

The policy lives in `site/privacy/index.html`, and `.github/workflows/pages.yml` deploys `site/` to **https://victorgpt0.github.io/myLedger/privacy/**. The app links to it from the SMS disclosure screen (`ui/common/Links.kt`).

One-time setup: in the repo's Settings › Pages, set the source to **GitHub Actions**. After that, every push to `main` that changes `site/` redeploys. Enter the same URL in Play Console › App content › Privacy policy.

Before publishing, fill in `[DEVELOPER NAME]` and `[CONTACT EMAIL]` in the policy.

### Data safety

Following the privacy policy (nothing leaves the device):

- Data collected: **No**. Data processed only on the device doesn't count as collected.
- Data shared: **No**.
- Encrypted in transit: not applicable (nothing is transmitted).
- Users can request deletion: uninstalling deletes all data.

Revisit these answers if you ever add analytics, crash reporting or a backend.

### Other App content items

Privacy policy URL (see above), app access (no login needed), ads (none), content rating questionnaire, target audience, and the **financial features declaration** (personal finance/budgeting, no loans or payments).

### Testing requirement

Personal developer accounts created after 13 Nov 2023 must run a **closed test with at least 12 testers for 14 continuous days** before applying for production access. Start this early.

## 4. Over-the-air updates (Play In-App Updates)

`ui/update/InAppUpdateController` checks Play on launch. `domain/update/UpdatePolicy` decides the update mode:

| Release `inAppUpdatePriority` | Behaviour |
|---|---|
| 4–5 | **Immediate**: blocking full-screen update. Use for critical fixes, e.g. a parser bug corrupting totals. |
| 2–3 | **Flexible**: downloads in the background, then a "Restart" snackbar. Shown on the next launch. |
| 0–1 (default) | Flexible, but only once the update has been available on the device for 3 or more days. |

Priority can only be set through the **Play Developer Publishing API** (`edits.tracks` → `releases[].inAppUpdatePriority`) when creating the release. The Play Console web UI can't set it, so releases uploaded through the Console get priority 0.

**Testing updates:** in-app updates only work for builds installed from Play. Upload two versions (different `versionCode`) through **internal app sharing**, install the lower one from its link, then open the higher one's link without installing. On the next launch the app sees the update. Sideloaded and debug builds never prompt.

## 5. Still to do before production

- [x] App icon: generated by `tools/icon/generate_app_icon.py`. Upload `app/src/main/ic_launcher-playstore.png` as the Play Store icon (512×512).
- [ ] Final app name and store listing: short and long descriptions, screenshots, feature graphic.
- [ ] Fill in the privacy policy placeholders, enable Pages (source: GitHub Actions), and merge `site/` to `main`.
- [ ] Parser coverage: collect real samples of M-PESA message types not yet handled (Fuliza, reversals, M-Shwari/ZIIDI confirmations, Pochi, deposits) and add patterns.
- [ ] Crash and ANR monitoring: Play Console vitals covers basics. A third-party SDK would change the Data safety answers above.
