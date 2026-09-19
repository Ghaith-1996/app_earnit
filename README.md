# Earn it!

Native Android workout planner and rest timer, built with Kotlin + Jetpack Compose.
Public name: **Earn it!**; `applicationId`: `com.fitness.restlock`.

Create/edit/reorder saved routines, configure sets and reps, then start a workout.
Use the phone normally during rest. At 0:00, non-allowed apps redirect to a decision
screen: **Exercise done**, **+30s rest**, or **Finish**. The final planned set says
**Finish final set** and completes without another countdown. Sessions can end early.
Saved workouts produce a summary and local history; Home shows the latest three
results. Quick start has no structured log. Deleting a routine preserves logs.

## Run

Open the project in Android Studio (Meerkat Feature Drop 2024.3.2 or newer), let Gradle sync, then
run the `app` configuration on an Android 8.0+ device or emulator.

Debug builds need no production signing credentials and use Google's test AdMob
configuration. Both variants use `com.fitness.restlock` with no debug suffix;
Android will not install differently signed variants over each other. Preserve
local data before uninstalling a variant to switch signing identities.

Build configuration: min SDK **26**, compile/target SDK **36**, AGP **8.10.0**,
Gradle **8.11.1**, Kotlin and Compose compiler plugin **2.2.0**. The active plugin
versions are in the root `build.gradle.kts`; this project does not currently use
the plugin aliases in `gradle/libs.versions.toml`.

Install Android SDK Platform 36. For terminal builds, set `JAVA_HOME` to a
compatible JDK (17 or newer supported by Gradle). On this Windows checkout:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
```

`gradle.properties` currently selects the Android Studio JDK at that same Windows
path; on another machine, override `org.gradle.java.home` with your JDK location.
If Windows `clean` reports locked lint-cache JARs, run `.\gradlew.bat --stop`
and retry the build with `--no-daemon` to release the cached file handles.


## Architecture

`RestLockApp` wires real backend adapters, not development fakes. UI and contracts
live in `app/src/main/kotlin/com/restlock/`; native backend code lives in
`app/src/main/java/com/fitness/restlock/backend/`.

- `SessionEngine` / `BackendSessionEngine`: serialized workout commands, set progression,
  final-set completion, and recovery of pending results through `WorkoutController`.
- `SettingsRepository`: DataStore-backed rest duration and **allowed** package names.
- `FitnessRepository` / `BackendFitnessRepository`: profile age/sex/weight/height,
  saved routines with ordered exercise IDs/sets/reps, active metadata, logs, and summary.
- `InstalledAppsProvider`: launchable apps queried through PackageManager for the allowlist.
- `PermissionGateway`: native permission status/settings access.

Two DataStore preference stores hold session/settings and fitness data; no Room database.
Capacity is 30 routines and 30 logs. Logs include timestamps/duration and set counts
when known, exercises reached, and estimated calories. There is no individual log-deletion UI.
Fakes are used by tests. No account or remote Earn it! backend exists.

## Android behavior

| Phase | Blocking |
| --- | --- |
| Idle | None |
| Resting | None |
| AwaitingDecision | Enforce allowlist after expiry |

Accessibility observes foreground package changes throughout an active workout,
including `Resting`, so it knows which app is already open at expiry. It reads no
screen text/content or node trees (`canRetrieveWindowContent=false`). It sends
blocked apps through Home to `BlockerActivity` only in `AwaitingDecision`.
Self, launchers, settings, dialer/emergency and other essential packages are exempt;
an empty allowlist is strict mode. Both package logging and Home's diagnostic card
are guarded by `FLAG_DEBUGGABLE`. First enabling Accessibility requires in-app disclosure/consent.

The timer uses coroutine ticking and `AlarmManager.setAndAllowWhileIdle`; no exact-alarm
permission or workout countdown foreground service/notification. Background alarms
are inexact. No `QUERY_ALL_PACKAGES` permission; launcher/home queries restrict visibility.

AdMob initializes and preloads at startup; display is optional after the workout
has ended. Saved-workout summaries have an optional support action. The app has no
integrated UMP/CMP flow. Workout/profile/settings/session data are not uploaded to an
Earn it! backend, but AdMob is a network SDK. Android backup is enabled, with Android
12+ cloud exclusions but no legacy exclusions; avoid unconditional offline/retention claims.
See [privacy policy](PRIVACY_POLICY.md) and [Play declarations](GOOGLE_PLAY_DECLARATIONS.md)
for the consent, backup, contact, and hosted-policy release checklist.

## UI copy and tests

Default application copy is in `app/src/main/res/values/strings.xml`, with formatting
and plurals for counts. The language is English; no second-language translation is
added. Exercise catalog names/sections/equipment and user-entered or stored routine
names remain content, separate from this localization foundation.

Run `testDebugUnitTest`, `lintDebug`, and `assembleDebug`. Tests cover the fake contract,
session reducer/controller/adapters, persistence/recovery/mutations, ViewModel routines,
and blocker coordination/lifecycle. Device smoke tests cover onboarding, picker,
countdown/expiry, final completion, history, deletion, and optional support.
See [BACKEND_HANDOFF.md](BACKEND_HANDOFF.md) for technical detail.

## Release build

The release signing config is wired in `app/build.gradle.kts` and reads the
keystore from `key/key` (a PKCS#12 keystore) plus passwords from a local,
git-ignored `key/keystore.properties` file:

```
storePassword=...
keyAlias=...
keyPassword=...
```

A template lives at `key/keystore.properties.example`. Once the real
properties file is in place:

```
./gradlew :app:bundleRelease     # produces app/build/outputs/bundle/release/app-release.aab
./gradlew :app:assembleRelease   # produces app/build/outputs/apk/release/app-release.apk
```

Both `bundleRelease` and `assembleRelease` require the existing production
keystore and all three nonblank signing properties. `validateProductionSigning`
fails clearly if any are missing, including when release tasks are reached via
an aggregate task. There is no debug signing fallback. Invalid credentials also
fail Android's signing validation. Use `assembleDebug` for local builds without
production credentials. Never regenerate or replace the production keystore.
Both `key/key` and `key/keystore.properties` are Git-ignored; only the empty
template belongs in Git. Do not include credential values in logs or commits.

Debug uses Google's sample AdMob application and rewarded unit IDs; release uses
the existing production IDs. Verify the merged manifest and generated resources
for each variant without copying production IDs into reports.

### Versioning and production verification

Version metadata is explicit in `app/build.gradle.kts`: `releaseVersionCode = 1`
and `releaseVersionName = "1.0"`. The first-release values are preserved because
the repository does not establish a previous Play upload. The production
application ID remains `com.fitness.restlock`.

**Every Play upload requires an integer versionCode greater than the highest
previously uploaded versionCode**, including testing tracks. Check Play Console,
update `releaseVersionCode`, and pass that previous value to enforce the comparison:

```text
./gradlew bundleRelease -PlastUploadedVersionCode=<highest-code-from-Play-Console>
```

Use `0` only when there have been no uploads. A missing comparison property is
allowed for local builds; Gradle cannot discover your Play upload history.
Malformed, negative, equal, or greater previous codes fail the build. Keep the
user-facing `versionName` aligned with the intended release; there is no automatic
timestamp or version generation.

Before uploading an AAB:

1. Run `clean`, `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.
2. Run `bundleRelease` with production signing configured and the previous upload
   code supplied as above. Confirm `app/build/outputs/bundle/release/app-release.aab`
   exists and record its size.
3. Use `jarsigner -verify` and `keytool -printcert -jarfile` on the AAB to verify
   its signature and confirm its signer is the expected production certificate,
   not the Android debug certificate. Never share private key material.
4. Use `bundletool dump manifest --bundle=<aab> --module=base` to verify the
   package, min SDK 26, target SDK 36, and expected version code/name in the
   artifact. Verify release AdMob resources as well.
5. Smoke-test on Android 16: system-bar insets/back navigation, rest expiration,
   process restoration, the blocker decision screen, saved workout completion
   and history, and the rewarded ad flow. Blocking must remain exclusive to
   `AwaitingDecision`, never `Idle` or `Resting`.

If production signing is unavailable, a failed `bundleRelease` with the explicit
signing error is the expected safety result; no release artifact is ready for
upload. Do not substitute a debug-signed artifact.

Compatibility references: [Android SDK / AGP / Gradle requirements](https://developer.android.com/build/releases/about-agp)
and [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16).
