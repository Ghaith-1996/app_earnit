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

Open the project in Android Studio (Hedgehog or newer), let Gradle sync, then
run the `app` configuration on an Android 8.0+ device or emulator.

There is no API key, no network, no signing — the debug build installs as
`com.restlock.debug`.

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

If `key/key` or `key/keystore.properties` are absent the release build will
still compile but will be **unsigned** — Gradle simply omits the signing
config.

Version metadata lives in `app/build.gradle.kts` (`versionCode` /
`versionName`). Bump `versionCode` for every store upload.

## Notes

- First release is a private APK, not a Play submission.
- This blocks only the selected apps; whole-device kiosk is out of scope.
- Package visibility is restricted to launchable apps (no
  `QUERY_ALL_PACKAGES`) per Play policy.
