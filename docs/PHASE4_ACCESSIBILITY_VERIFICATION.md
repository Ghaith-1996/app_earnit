# Phase 4: Accessibility blocking verification

Integration branch: `master` (confirmed with the owner; this repository has no `main`).
Implementation branch: `fix/accessibility-lock-hardening`. Do not merge without human review.

## Scope and implementation

Blocking requires `WorkoutMode.AwaitingDecision` both before Home and again before the delayed
activity launch. Idle and Resting never request either action. The allowlist remains an allowlist:
an empty list blocks ordinary apps while essential packages remain usable.

The service reads only event type and package metadata. It ignores package names while Idle.
During an active workout it retains the latest package in memory, including during Resting, so
an ordinary app already open when the timer expires can be blocked without another app switch.
This preserves the existing expiry behavior while narrowing the previous always-on package tracking.
Package detection is not itself blocking. The onboarding disclosure now describes this distinction.
No event text, nodes, window content, view IDs, passwords, notification contents, or documents are read.
Production package diagnostics are disabled; the debug build retains logs and its Home diagnostic panel.

| Service configuration | Before | After |
| --- | --- | --- |
| `canRetrieveWindowContent` | `true` | `false` |
| Event types | `typeWindowStateChanged` and `typeWindowsChanged` | `typeWindowStateChanged` |
| Feedback | `feedbackGeneric` | `feedbackGeneric` |
| Flags | `flagRetrieveInteractiveWindows` | None (attribute omitted) |
| `canPerformGestures` | `false` | `false` |
| Notification timeout | 100 ms | 100 ms |

`TYPE_WINDOW_STATE_CHANGED` supplies the source package without retrieving nodes. Android requires
interactive-window access for `TYPE_WINDOWS_CHANGED`; keeping that event would preserve an unused
window-content capability. No Kotlin code uses the corresponding windows or nodes.
See [Android event documentation](https://developer.android.com/reference/android/view/accessibility/AccessibilityEvent#TYPE_WINDOW_STATE_CHANGED)
and [interactive-window flag requirements](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo#FLAG_RETRIEVE_INTERACTIVE_WINDOWS).
Package-only window events are not an authoritative focused-window API: split screen, transient system
surfaces, and OEM event delivery still require device checks. No OEM reliability claim is made here.

The launch coordinator coalesces pending work and applies a one-second global cooldown. A trailing
check handles the most recent blocked app when that cooldown expires, even without another event.
It cancels pending launches/rechecks on lock exit, disconnect/reset, and relevant allowlist changes.
Delayed activity launches recheck the current state and allowlist. Failed launches permit retry.
Home, host, Settings, System UI, installers, permission controllers and existing OEM launcher/search
exemptions are retained. Known Android/Google/Samsung phone, in-call and emergency surfaces are added
as fallbacks alongside the existing dynamically resolved default dialer and installed Home packages.

`BlockerActivity` retains the Part 2 completion behavior: rest choices dismiss; a saved completion
hands off to the persisted summary; a quick finish or failed logging offers the dismissible support
flow after blocking has ended. Saving a result may keep the activity alive, but does not keep blocking
active. Its existing lifecycle routing is extracted into a pure function for regression testing.

## Permissions

Removed `android.permission.POST_NOTIFICATIONS` and its launch-time runtime request. Source searches
found no notification channel, notification posting, or foreground service feature. No new notification
system was introduced. `android.permission.BIND_ACCESSIBILITY_SERVICE` remains a protection on the
service declaration, not a runtime permission request. No `QUERY_ALL_PACKAGES` or exact-alarm
permission was added. Third-party SDK permissions are unchanged. The merged debug manifest still
includes `INTERNET`, `ACCESS_NETWORK_STATE`, Google `AD_ID`, Android `ACCESS_ADSERVICES_AD_ID`,
`ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, and
the app's `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. The merger report attributes the foreground
service permission and `SystemForegroundService` to transitive WorkManager 2.7.0. The app does not
start a workout notification/foreground service, so that library declaration does not justify its
old notification prompt. The merged manifest was checked and contains no `POST_NOTIFICATIONS`.

## Automated verification

Run with Android Studio's bundled JDK (`JAVA_HOME`), using the existing SDK and dependencies:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
```

Coverage includes inactive phases, strict/allowlisted/essential/missing packages, Home ordering,
rapid same/different-package events, trailing cooldown enforcement, delayed phase/allowlist changes,
service reset, failed-launch retry, expiry with an app already open, and backend-backed blocker choices.
Existing final-set, duplicate finish, persistence recovery and full/partial history tests remain in the suite.

The new essential-package test was observed failing before the fallback exemptions were added.
The new cooldown test was observed failing before the trailing recheck was added.
Final results: `testDebugUnitTest` passed all 106 tests (zero failures/errors/skips);
`assembleDebug` succeeded; `lintDebug` succeeded with zero errors and 188 warnings.
`git diff --check` passed. Independent review found the cooldown issue and approved its fix.

## Files changed

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/fitness/restlock/backend/blocking/AppBlockerAccessibilityService.kt`
- `app/src/main/java/com/fitness/restlock/backend/blocking/BlockerLaunchCoordinator.kt`
- `app/src/main/java/com/fitness/restlock/backend/blocking/BlockingDiagnostics.kt`
- `app/src/main/java/com/fitness/restlock/backend/blocking/KnownExemptPackages.kt`
- `app/src/main/kotlin/com/restlock/BlockerActivity.kt`
- `app/src/main/kotlin/com/restlock/MainActivity.kt`
- `app/src/main/kotlin/com/restlock/ui/screens/HomeScreen.kt`
- `app/src/main/kotlin/com/restlock/ui/screens/PermissionOnboardingScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/xml/app_blocker_accessibility_service.xml`
- `app/src/test/java/com/fitness/restlock/backend/blocking/AppBlockPolicyTest.kt`
- `app/src/test/java/com/fitness/restlock/backend/blocking/BlockerLaunchCoordinatorTest.kt`
- `app/src/test/kotlin/com/restlock/ui/WorkoutFinishLoggingTest.kt`
- `docs/PHASE4_ACCESSIBILITY_VERIFICATION.md`

## Device verification checklist

No device was attached (`adb devices` returned an empty list), so **none of these scenarios has been
executed on a device**. Repeat on Pixel/AOSP and Samsung/One UI, recording Android/One UI version,
launcher, selected dialer and pass/fail. Re-enable Accessibility after installing the changed service
configuration and verify the disclosure remains required before opening system settings.

| Check | Steps | Expected | Status |
| --- | --- | --- | --- |
| Idle | Open ordinary apps before a session and after Finish | No blocking | Pending |
| Resting | Start a workout; open Chrome/Instagram during the countdown | App usable, no Home action | Pending |
| Zero | Let timer expire; open a non-allowed app | Home briefly, then one blocker | Pending |
| Already open at zero | Keep a non-allowed app visible through timer expiry | Blocker opens at expiry | Pending |
| Allowed app | Allow Spotify; open it during the lock | Remains usable | Pending |
| Strict mode | Empty allowlist; open an ordinary app at zero | Blocked | Pending |
| Settings | Open Settings during the lock | Usable, including Accessibility controls | Pending |
| Home/OEM search | Press Home; open Google/Samsung launcher search | Usable, no blocker loop | Pending |
| Custom launcher | Select a different installed launcher; press Home | Dynamically exempt, no loop | Pending |
| Phone | Open default and OEM dialers/in-call UI during lock | Usable; do not place test emergency calls | Pending |
| Core surfaces | Open permission controller/installer/System UI | Usable | Pending |
| Set done | Press Exercise done on a nonfinal set | Resting, blocker closes, apps usable | Pending |
| +30 sec | Press +30s rest | 30-second rest, blocker closes, apps usable | Pending |
| Finish | Finish saved and quick sessions; exercise summary/support dismiss/ad failure paths | Idle, one result, optional support never traps exit | Pending |
| Final set | At final planned set, open blocked app then Finish final set | Complete, one summary, no phantom rest/alarm/relaunch | Pending |
| Finish elsewhere | End session in main app while blocker is in background; return to it | Blocker dismisses or hands off to saved summary | Pending |
| Event noise | Rapidly reopen same app and alternate blocked apps | No activity spam/crash; later block still works | Pending |
| Cooldown | Open another blocked app within one second and leave it visible | Blocked after cooldown without a further gesture | Pending |
| Delayed launch | Resolve lock or allow app just after Home | No stale activity launch after resolution | Pending |
| Service lifecycle | Disable/re-enable service or restart process during a lock | No old pending launch; fresh blocked event works | Pending |
| OEM/multiwindow | Test split screen, transient System UI and lock screen | Essential surfaces safe; record detection limitations | Pending |
| Notifications | Fresh install/open on Android 13+ | No notification permission prompt | Pending |
| Diagnostics | Compare debug/release behavior | Package logs/panel in debug only; no content access | Pending |

## Deferred work

API 36, release signing/versioning, localization, CI, privacy-policy/Play Console work, and exercise
images remain outside this milestone. Device/OEM verification above remains required before release.
