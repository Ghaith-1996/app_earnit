# earn it !

Native Android app for the Rest-Lock fitness MVP. Kotlin + Jetpack Compose,
dark-first, single-module.

> User-facing name: **earn it !** — internal `applicationId` is still
> `com.fitness.restlock` so the existing signing identity is preserved.

The UI is built against a small **StateFlow contract** that the backend agent
implements (DataStore, Room, AlarmManager, AccessibilityService). For local
development the contract is wired to in-memory fakes so the full UI is
runnable today.

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

## Project layout

```
app/src/main/kotlin/com/restlock/
├── RestLockApp.kt          # Application container, wires the contract to fakes
├── MainActivity.kt         # Hosts the Compose nav graph
├── BlockerActivity.kt      # Full-screen lock prompt (launched by the AccessibilityService)
├── domain/                 # The contract the backend MUST implement
│   ├── SessionState.kt
│   ├── SessionEngine.kt
│   ├── SettingsRepository.kt
│   ├── InstalledAppsProvider.kt
│   └── fake/               # In-memory implementations for dev
└── ui/
    ├── theme/              # Palette, typography, theme wrapper
    ├── components/         # TimerRing, GlassCard, PrimaryAction, StateChip
    ├── screens/            # HomeScreen, SetupSheet, AppPickerScreen, BlockerScreen
    ├── nav/                # Navigation graph
    ├── HomeViewModel.kt
    ├── AppPickerViewModel.kt
    ├── BlockerViewModel.kt
    └── RestLockViewModelFactory.kt
```

## The contract (what the backend agent implements)

The UI depends on **three interfaces only**. Swap the fakes in
`RestLockApp.onCreate()` for real implementations and the UI is unchanged.

### `SessionEngine`

The state machine the UI observes and commands.

```
Idle
  └─ startWorkout(rest)        ──▶ Resting (remaining = rest)

Resting
  ├─ timer reaches 0           ──▶ AwaitingDecision (blockerArmed = true)
  └─ finishWorkout()           ──▶ Idle

AwaitingDecision
  ├─ exerciseDone()            ──▶ Resting (remaining = chosenRest, setsCompleted +1)
  ├─ addThirtySeconds()        ──▶ Resting (remaining = 30s, extraRests +1, blockerArmed = false)
  └─ finishWorkout()           ──▶ Idle
```

Implementations must:

- Emit `state` updates on every UI-relevant change, including each visible
  countdown tick (the fake ticks every 250 ms).
- Survive process death — after restart, the first collector sees the correct
  phase (back this with DataStore on the real implementation).
- Use `AlarmManager` for the actual rest-end timing on the real impl, and
  gracefully fall back if `SCHEDULE_EXACT_ALARM` is denied.

See `FakeSessionEngineTest` for the executable spec.

### `SettingsRepository`

Persisted user preferences. Real impl is DataStore-backed.

- `chosenRest: Flow<Duration>` — current rest length
- `blockedPackages: Flow<Set<String>>` — package names the user picked
- `setChosenRest(Duration)` / `setBlockedPackages(Set<String>)`

### `InstalledAppsProvider`

Lists the launchable apps the user is allowed to block. Real impl queries
`PackageManager` for `ACTION_MAIN` + `CATEGORY_LAUNCHER` activities (no
`QUERY_ALL_PACKAGES`) and filters out the host app, the active launcher,
settings, dialer, emergency, and core system packages.

## Android integration the backend owns

The frontend assumes the backend wires:

- **AccessibilityService** detecting foreground app changes; when a blocked
  package is foregrounded during `AwaitingDecision`, it launches
  `BlockerActivity` (passing the human-readable label in
  `BlockerActivity.EXTRA_BLOCKED_APP_LABEL`).
- **AlarmManager** for the actual rest-end alarm.
- **Foreground service + notification** showing the live countdown.
- **Room** for workout history (post-MVP — current spec is "no history").

## UI states implemented

- **Idle** — large ring with "ready when you are", Start CTA + presets sheet.
- **Resting** — animated progress ring counting down, stats below, finish
  workout secondary action.
- **AwaitingDecision** — ring shows `0:00`, mint "Exercise done" primary CTA,
  `+30s rest` and `Finish` secondary actions.
- **Blocker overlay** — same three actions as a full-screen activity for when
  the user opens a blocked app.

## Testing

- `FakeSessionEngineTest` pins the state-machine contract.
- Add instrumentation tests under `app/src/androidTest/...` once the backend
  is wired (the spec test plan calls for Compose flow tests).

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

## Notes

- This blocks only the selected apps; whole-device kiosk is out of scope.
- Package visibility is restricted to launchable apps (no
  `QUERY_ALL_PACKAGES`) per Play policy.
