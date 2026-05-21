# Earn it! Backend And Frontend Handoff

## App Identity

App name: Earn it!

Android package/application ID: `com.fitness.restlock`

The app is a local Android workout rest timer. It lets the user start a workout, choose a rest duration, and decide which apps are allowed after the rest timer ends. Blocking only starts when the rest timer reaches `0:00`.

## Backend Contract

The native backend is exposed through `RestLockBackend.controller(context)` as a `WorkoutController`.

`WorkoutController.state` is a `StateFlow<WorkoutState>` with the full session state. New native UI can call the controller directly, but the existing `com.restlock` frontend intentionally goes through adapters in `com.restlock.domain.backend`.

- `setRestDuration(seconds)`
- `setAllowedApps(packages)`
- `startWorkout()`
- `addThirtySecondsRest()`
- `exerciseDone()`
- `finishWorkout()`

`WorkoutState` includes:

- mode: `Idle`, `Resting`, or `AwaitingDecision`
- selected rest duration
- remaining seconds
- allowed apps
- permission status
- completed sets for the current session

`RestLockApp` provides real `SessionEngine`, `SettingsRepository`, `InstalledAppsProvider`, and `PermissionGateway` implementations backed by the native backend.

## Timer Behavior

The app supports preset rest durations and a custom rest duration input. Custom rest duration is entered from the setup sheet with minute and second fields and is clamped by backend validation to the supported rest range.

State flow:

- `Idle`: no active workout, no app blocking.
- `Resting`: timer is counting down, no app blocking.
- `AwaitingDecision`: rest hit `0:00`, app blocking is active.

Decision actions:

- `exerciseDone()`: increments the completed set count and restarts the original selected rest duration.
- `addThirtySecondsRest()`: starts a 30-second extra rest.
- `finishWorkout()`: resets the active session to idle.

## Allowed Apps Model

The app uses an allowlist, not a blocklist.

The picker stores allowed apps. When the rest timer reaches `0:00`, all normal launchable apps are blocked except:

- the user's allowed apps
- this app
- launchers/home screen
- known home/Google/Samsung home surface packages
- Android Settings
- System UI
- permission controller
- package installer
- dialer/phone/emergency-related core packages

Known home false-positive packages currently include Google Search/Home surface (`com.google.android.googlequicksearchbox`) and Samsung Finder (`com.samsung.android.app.galaxyfinder`) so returning to the home screen does not reopen the blocker.

## Blocking Service

`com.restlock.BlockerActivity` is launched by the accessibility blocker when a non-allowed app is opened during `WorkoutMode.AwaitingDecision`. The service sends the user home first, then opens the blocker screen. If the frontend renames that activity, update `AppBlockerAccessibilityService.blockerIntent`.

`AppBlockerAccessibilityService` receives window change events, reads the foreground package name, checks `AppBlockPolicy.shouldBlock(...)`, calls `GLOBAL_ACTION_HOME`, then opens `BlockerActivity`.

Important rule: never block in `Idle` or `Resting`. Blocking only happens in `AwaitingDecision`.

## Permissions And Onboarding

The app includes a permission onboarding/disclosure screen before opening Android Accessibility settings.

The disclosure explains:

- Accessibility is used only after the rest timer reaches `0:00`.
- The service checks the foreground app package name.
- The service returns blocked apps to the workout decision screen.
- The app does not read messages, passwords, form text, notifications, or screen content for ads or analytics.
- Settings and session data stay local on the device.

`MainActivity` starts on the permission route if Accessibility is off. The home screen banner links to the permission onboarding screen instead of opening system settings directly.

The app no longer requests `SCHEDULE_EXACT_ALARM`. The timer uses in-process coroutine ticking while alive and an `AlarmManager` fallback without asking the user for exact alarm permission.

## Installed Apps

Installed launcher apps are available from `RestLockBackend.installedApps(context).loadInstalledApps()`. Permission status and settings intents are available from `RestLockBackend.permissions(context)` and surfaced on the home screen through `HomeViewModel`.

The installed-app picker should be labeled as an allowed-app picker, not a blocklist picker. The current UX copy is "Apps allowed during lock", with a strict mode when zero apps are allowed.

## Diagnostics

Blocking diagnostics are exposed through `BlockingDiagnostics.state` and shown on the home screen: lock active, last detected package, and last blocked package.

These diagnostics are useful on real devices because Accessibility events can report OEM-specific home/search packages.

## Support Creator Dialog

When the user chooses `Finish workout`, the frontend now shows a local support dialog before ending the session.

Dialog copy:

- Title: `Support the creator?`
- Body: `Could you watch a short ad to support the creator of Earn it!?`
- Primary CTA: `Yes, support the creator`
- Secondary CTA: `No thanks`

The primary CTA uses the mint/green visual treatment so it is more noticeable. The `onWatchAd` branch is connected to Google AdMob rewarded ads through `CreatorSupportRewardedAd`.

Debug builds use Google's test rewarded ad IDs:

- app id: `ca-app-pub-3940256099942544~3347511713`
- ad unit: `ca-app-pub-3940256099942544/5224354917`

Release builds use the creator's production AdMob IDs:

- app id: `ca-app-pub-2584072112522734~9482454567`
- ad unit: `ca-app-pub-2584072112522734/3207856102`

If the rewarded ad fails to load or show, the app continues and finishes the workout so the user is not blocked.

Files:

- `SupportCreatorDialog.kt`
- `HomeScreen.kt`
- `BlockerActivity.kt`
- `CreatorSupportRewardedAd.kt`

Important Play Store note: Google Play's Ads section must now be set to yes, and Data Safety/privacy policy must include Google AdMob advertising-related data.

## Compliance Files

The repo includes Play Store helper documents:

- `PRIVACY_POLICY.md`
- `GOOGLE_PLAY_DECLARATIONS.md`
- `APP_BRIEF_FOR_AI.md`

Before production release, host the privacy policy online and replace the placeholder developer contact email.

## Build Verification

Recent verification commands:

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`

Debug APK output:

- `app/build/outputs/apk/debug/app-debug.apk`
