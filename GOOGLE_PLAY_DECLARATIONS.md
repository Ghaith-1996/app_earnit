# Google Play Compliance Drafts

These drafts are written for the current app behavior. Review them again before submitting to Google Play.

## Accessibility API Declaration

Fitness Rest Lock uses AccessibilityService for its core app-locking feature. During a workout, the user selects apps that are allowed during a lock period. When the rest timer reaches 0:00, the app enters a decision lock. The Accessibility service detects the foreground app package name so the app can determine whether the opened app is allowed. If the foreground app is not allowed, Fitness Rest Lock sends the user back to the Android home screen and opens the workout decision screen with the choices to finish the workout, add 30 seconds of rest, or mark the exercise done.

The AccessibilityService API is used only while the app is in the decision lock state. It is not used to read messages, passwords, form text, notifications, or screen content for advertising or analytics. Foreground app package detection is processed locally on the device and is not transmitted to a server.

This app is not a disability-focused accessibility tool, so it should not declare `isAccessibilityTool=true`.

## In-App Disclosure Text

Rest Lock needs Accessibility only to detect blocked apps after your rest timer reaches 0:00. It checks the package name of the app currently in front, compares it with your allowed apps, and returns blocked apps to the workout decision screen. It does not read messages, passwords, form text, notifications, or screen content for ads or analytics. Your rest time, allowed apps, and current session stay on this device.

## Demo Video Checklist

Record a short video for Play Console showing:

1. The permission onboarding screen and disclosure.
2. The user checking the consent checkbox.
3. The user opening Android Accessibility settings and enabling Fitness Rest Lock.
4. The user starting a workout with a short timer.
5. The timer reaching 0:00.
6. The user opening a non-allowed app.
7. Fitness Rest Lock returning the user to the decision screen.
8. The user choosing exercise done or finish workout.

## Data Safety Draft

Suggested starting point for Play Console Data safety:

- Data collected by the app's own backend: No, because the app has no remote backend.
- Data collected by third-party SDK: Yes, Google AdMob rewarded ads may collect or process advertising identifiers, device identifiers, IP address, app interactions, and diagnostics according to Google's SDK behavior.
- Data shared: Yes, advertising-related data may be shared with Google AdMob when ads are loaded or shown.
- Data processed locally: selected rest duration, allowed app package names, active workout session state, and foreground app package name during decision locks.
- Account creation: No account.
- Data deletion: user can uninstall the app or clear app storage in Android settings.

Confirm exact Data Safety selections in Play Console against the current Google Mobile Ads SDK disclosure before release.

## Ads Declaration

The app now contains ads through Google AdMob rewarded ads.

Suggested Play Console answer:

- Contains ads: Yes.
- Ad type: Rewarded ad shown only when the user chooses "Yes, support the creator" after finishing a workout.
- Debug builds use Google's sample rewarded ad unit.
- Release builds use the production rewarded ad unit `ca-app-pub-2584072112522734/3207856102`.

## Exact Alarm Permission Decision

`SCHEDULE_EXACT_ALARM` was removed from the manifest for Play Store readiness. Fitness Rest Lock does not need to ask for exact alarm access for the MVP because the in-process coroutine timer handles active sessions while the app process is alive, and `AlarmManager.setAndAllowWhileIdle` provides a fallback when exact alarms are unavailable.

If a future version requires exact wakeups while the app is killed, revisit the policy and add a stronger user-facing justification before restoring this permission.

## Official Policy References

- Accessibility API policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Sensitive permissions policy: https://support.google.com/googleplay/android-developer/answer/16558241
- Data safety form guidance: https://support.google.com/googleplay/android-developer/answer/10787469
- Android exact alarm guidance: https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
