# Earn it! backend and frontend handoff

Public name: **Earn it!**; application ID: `com.fitness.restlock`. Native Android Kotlin/Compose, min SDK 26 and compile/target SDK 36.

## Wiring and persistence

`RestLockApp` wires real `BackendSessionEngine`, `BackendSettingsRepository`, `BackendFitnessRepository`, `BackendInstalledAppsProvider`, and native `PermissionGateway` implementations. `RestLockBackend.controller(context)` exposes `WorkoutController`; UI session commands go through `SessionEngine`.

`WorkoutPreferencesStore` (`workout_backend` DataStore) persists mode, rest duration, allowed packages, timer end, completed sets, extra rests, optional planned sets, active routine identity/start time, and pending completion information. `BackendFitnessRepository` (`fitness_repository` DataStore) persists profile age/sex/weight/height, up to 30 saved routines, up to 30 workout logs, active metadata, and pending summary. Routines hold IDs/names/creation time and ordered exercise IDs/sets/reps. Logs preserve timestamps/duration where known, set counts where known, exercises reached, and estimated calories. No Room database is used.

No remote Earn it! backend exists. These records are not uploaded by application code. AdMob is a separate network SDK. Android backup is enabled in the manifest; Android 12+ cloud exclusions exist, but older-version backup and device-transfer behavior still require review. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## Session and completion behavior

- `Idle`: no blocking.
- `Resting`: no blocking; coroutine ticking plus `AlarmManager.setAndAllowWhileIdle` wakeup. No exact-alarm permission or countdown foreground service/notification.
- `AwaitingDecision`: timer expired; allowlist enforcement.

`SessionEngine.startWorkout(rest, workout?)` starts quick start or a saved routine. The adapter reloads the current saved definition under the repository start/mutation lock. Commands are serialized, and backend start persists planned-set count and active identity together.

`exerciseDone()` is the internal API; the current UI says **Exercise done**. It marks one set and resumes the selected rest. The final planned set says **Finish final set** and transitions to idle without an extra countdown. `addThirtySeconds()`/controller `addThirtySecondsRest()` starts a 30-second rest without increasing completed sets. `finishWorkout()` can end early. Keep the final-set/reducer rules unchanged.

On completion, the controller ends blocking/cancels its alarm before fitness logging or optional support. The adapter atomically saves a result and pending summary while clearing fitness active metadata, then acknowledges the persisted backend completion. Pending completion recovery handles interrupted writes. Quick start has no structured fitness log. Older records retain unknown duration/completion rather than invented values.

Home shows the persisted summary and latest three logs. Dismissing a summary does not delete its log. Editing/deleting the active saved routine is rejected by repository guards. Deleting another saved routine removes its template, not logs. UI supports creating, configuring, reordering, editing, deleting, and starting saved routines; there is no individual log-deletion UI.

## Accessibility and allowlist

`AppBlockerAccessibilityService` observes `TYPE_WINDOW_STATE_CHANGED` package metadata while active (`Resting` or `AwaitingDecision`), ignoring idle events. `BlockerLaunchCoordinator` remembers the latest foreground package and checks current state before enforcing. This covers an app already open at expiry.

Blocking is **only** in `AwaitingDecision`, never `Idle` or `Resting`. Empty allowlist means strict mode except essential packages. `AppBlockPolicy`, `AndroidAppBlockPolicy`, and `KnownExemptPackages` cover self, launchers/home surfaces, settings, system UI, permissions/installers, dialer and emergency surfaces. The service goes Home and opens `com.restlock.BlockerActivity` for blocked apps. Activity lifecycle guards dismiss stale screens and route completed saved sessions to Home's summary.

Only package metadata is used: no screen text, messages, passwords, form/notification/browser contents, or node trees. Window-content retrieval and gesture capability are disabled. Observations stay local and are not sent to advertising/analytics. The service's `ApplicationInfo.FLAG_DEBUGGABLE` gate protects diagnostics and package logs; Home applies the same gate to the debug card. Preserve these existing guards.

`PermissionOnboardingScreen` provides scrollable disclosure and a checkbox before first opening Android Accessibility settings. It distinguishes active-workout observation from expiry-only blocking, and local fitness records from AdMob. Installed-app visibility uses launcher/home intent queries, not `QUERY_ALL_PACKAGES`.

## Optional creator support

`CreatorSupportRewardedAd.initialize()` runs in `RestLockApp.onCreate()` and preloads after initialization. It may load before the support prompt, on demand, and after ad dismissal/show failure. Google Mobile Ads SDK is 24.7.0; debug IDs are Google's samples, release IDs stay in build configuration. Never copy production IDs or signing secrets into docs.

Saved workouts first show `WorkoutSummaryDialog`, whose **Support the creator (optional)** action opens `SupportCreatorDialog`. Quick start can open support directly after ending. **Yes, support the creator** requests display; **No thanks**, dismissal, or load/show failure permits continuation. The session has already ended before support; never put completion behind ad success. No integrated UMP/CMP consent flow exists; resolving regional consent is separate release work.

## Verification and publication

Run `.\gradlew.bat testDebugUnitTest`, `.\gradlew.bat lintDebug`, and `.\gradlew.bat assembleDebug`. The APK is `app/build/outputs/apk/debug/app-debug.apk`. Core coverage includes reducer, controller, adapter, repository/mutation, ViewModel, blocker coordinator/lifecycle, and fake contract tests.

Default production copy is in `res/values/strings.xml`; exercise catalog content and existing stored/user-entered routine names are separate content. See README for release signing/versioning and [GOOGLE_PLAY_DECLARATIONS.md](GOOGLE_PLAY_DECLARATIONS.md) for policy verification, developer contact, hosted privacy URL/in-app access, consent/CMP, and submission tasks.
