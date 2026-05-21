# Earn it! - Detailed App Brief For Google Play And AI Handoff

## 1. App Identity

App name: Earn it!

Android package name: `com.fitness.restlock`

App type: Android mobile app

Primary category suggestion: Health & Fitness

Secondary positioning: Focus, productivity, workout discipline

Default language suggestion: French (Canada) or English, depending on the first market. If the listing is created in French (Canada), localize the store text in French first.

Free or paid: Free for the MVP

Target users: People who train with sets and rest periods and want to stop themselves from scrolling or opening distracting apps when rest time is over.

## 2. One-Sentence Description

Earn it! is a workout rest timer that lets users rest between sets, then blocks non-allowed apps when the rest timer ends until the user chooses their next workout action.

## 3. Full Product Description

Earn it! helps users stay focused during workouts. The user starts a workout session, chooses a rest duration, and optionally chooses apps that are allowed during the lock period. While the rest timer is running, the user can use the phone normally. When the timer reaches 0:00, the app enters a decision state.

In the decision state, Earn it! prevents the user from opening non-allowed apps. If the user opens a blocked app, the app sends the user back to the Android home screen and opens the Earn it! decision screen. The decision screen gives the user three choices:

- Exercise done: counts the set as completed and starts the original rest duration again.
- Add 30 seconds: gives the user 30 more seconds of rest.
- Finish workout: ends the workout session and turns off app blocking until the next workout.

The app does not store workout history in the MVP. It stores only local settings and the current active session state.

## 4. Core Features

- Start a workout session.
- Choose a preset rest duration.
- Choose a custom rest duration.
- Choose apps that remain allowed during the lock period.
- Strict mode when no apps are allowed, meaning all non-essential apps are blocked during the decision lock.
- Automatically enter a decision state when the rest timer reaches 0:00.
- Block non-allowed apps only during the decision state.
- Allow essential apps such as Settings, launchers, System UI, permission controller, dialer/phone, package installer, and the app itself.
- Accessibility onboarding screen with clear disclosure and user consent.
- Debug status showing Accessibility state, lock mode, last detected app, and last blocked app.

## 5. Current App Behavior

The app does not block apps while the timer is still counting down. Blocking starts only when the timer finishes and the app mode becomes `AwaitingDecision`.

During `AwaitingDecision`, the app allows:

- The Earn it! app itself.
- Android launcher/home screen.
- Android Settings.
- System UI.
- Android permission controller.
- Phone/dialer/emergency-related core packages.
- Package installer.
- Apps the user explicitly allowed.

During `AwaitingDecision`, the app blocks:

- Any normal launchable app that is not essential and not in the allowed apps list.

## 6. Permissions And Sensitive APIs

### AccessibilityService

Earn it! uses Android AccessibilityService for app blocking.

Why it is needed:

The app must know which app is currently in front when the rest timer has ended. This lets Earn it! decide whether the foreground app is allowed or blocked.

When it runs:

The app uses this behavior only during the workout decision lock, after the rest timer reaches 0:00.

What it checks:

It checks the foreground app package name.

What it does:

If the foreground app is not allowed, Earn it! sends the user back to the Android home screen and opens the workout decision screen.

What it does not do:

Earn it! does not use Accessibility to read messages, passwords, form text, notifications, browsing content, private documents, or screen content for ads or analytics.

Important Play Console note:

This app is not a disability-focused accessibility tool. If Google Play asks whether it is an accessibility tool, the answer should be no. The API is used for a user-facing app-locking feature.

### Exact Alarm Permission

The app does not request `SCHEDULE_EXACT_ALARM` in the current Play-ready MVP.

Reason:

The timer works with an in-process coroutine while the app is alive and uses Android alarm fallback behavior when exact alarms are unavailable. Avoiding exact alarm permission reduces Google Play review risk.

## 7. Local Data Storage

The app stores the following data locally on the device:

- Selected rest duration.
- Allowed app package names.
- Active workout mode.
- Timer end timestamp.
- Completed set count for the current session only.

The app does not store workout history in the MVP.

The app does not require an account.

The app does not transmit this data to a server.

The app does not sell user data and has no remote backend.

The app uses Google AdMob rewarded ads for optional creator support after a workout ends.

The app does not use payments in the MVP.

## 8. Privacy Policy Draft

Fitness Rest Lock / Earn it! is a local workout rest timer and app-locking tool. The app stores rest duration, allowed apps, and current workout session state locally on the user's device.

Earn it! uses Android Accessibility only for app-locking. When a workout rest timer reaches 0:00, the Accessibility service detects the foreground app package name. If that app is not allowed, Earn it! sends the user back to the Android home screen and opens the workout decision screen.

Earn it! does not use Accessibility to read messages, passwords, form text, notifications, or screen content for advertising or analytics.

Earn it! itself has no account system, no remote user database, and stores no workout history in the MVP. Google AdMob may process advertising-related data when a rewarded ad is loaded or shown.

Users can delete local app data by uninstalling the app or clearing app storage in Android settings.

Developer contact email: replace this with the real developer email before publishing.

## 9. Google Play Console Suggested Answers

### Privacy Policy

Provide a privacy policy URL. The current Markdown privacy policy should be converted into a hosted web page before production submission.

Local file in project: `PRIVACY_POLICY.md`

### App Access

Suggested answer:

No special login or account is required. The app can be opened and tested without credentials.

Testing note:

The reviewer must enable the app's Accessibility service in Android settings to test app blocking.

### Ads

Suggested answer:

Yes, the app contains ads.

Details:

The app uses Google AdMob rewarded ads. A rewarded ad is shown only when the user chooses to support the creator after finishing a workout.

### Content Rating

Suggested positioning:

The app is a health and fitness / productivity utility. It does not contain user-generated content, violence, sexual content, gambling, alcohol/tobacco/drug promotion, or in-app purchases in the MVP.

### Target Audience

Suggested target:

Adults and teens who work out and can understand app permissions. A conservative target audience is 13+ or 16+, depending on the questionnaire. Avoid targeting children because the app uses AccessibilityService and app-control behavior.

### Data Safety

Suggested answer:

The app's own backend does not collect or share data off the device, but Google AdMob may collect or process advertising-related data when ads are loaded or shown.

Data processed locally:

- Rest duration.
- Allowed app package names.
- Active workout session state.
- Foreground app package name during decision locks.

Data shared:

- Advertising-related data may be shared with Google AdMob when ads are loaded or shown.

Data collected/transmitted:

- The app itself has no remote backend. Google AdMob may collect or process advertising identifiers, device identifiers, IP address, app interactions, and diagnostics according to Google's SDK behavior.

User data deletion:

- User can uninstall the app or clear app storage.

### Government Apps

Suggested answer:

No, this is not a government app.

### Financial Features

Suggested answer:

No, the app does not include financial features.

### Health

Suggested answer:

The app is fitness-related because it helps manage workout rest timing, but it does not provide medical advice, diagnosis, treatment, health measurements, medical records, or clinical functionality.

## 10. Google Play Store Listing Draft

### Short Description

Workout rest timer that blocks distractions when rest is over.

### Full Description

Earn it! helps you stay focused between workout sets.

Start a workout, choose your rest time, and decide which apps are allowed during lock mode. While the timer runs, use your phone normally. When rest reaches 0:00, Earn it! asks you to choose your next move: finish the workout, add 30 seconds, or mark the exercise done and start the next rest.

If you try to open a non-allowed app after the rest timer ends, Earn it! sends you back to the workout decision screen so you can get back to training.

Features:

- Preset and custom rest timers.
- Allowed apps list.
- Strict mode for blocking all non-essential apps.
- Exercise done, add 30 seconds, and finish workout actions.
- Local-only settings and current session state.
- No workout history stored in the MVP.
- No account required.

Accessibility disclosure:

Earn it! uses Android Accessibility only to detect the foreground app during the decision lock after your rest timer reaches 0:00. This lets the app return blocked apps to the workout decision screen. Earn it! does not read messages, passwords, form text, notifications, or screen content for ads or analytics.

### Feature Graphic / Screenshot Ideas

Screenshot 1:

Main timer screen with rest duration visible.

Screenshot 2:

Custom timer selection.

Screenshot 3:

Allowed apps screen.

Screenshot 4:

Decision screen after timer reaches 0:00.

Screenshot 5:

Accessibility disclosure/onboarding screen.

## 11. Accessibility Declaration Draft For Play Console

Earn it! uses AccessibilityService for its core app-locking feature. During a workout, the user selects apps that remain allowed during a lock period. When the rest timer reaches 0:00, the app enters a decision lock. The Accessibility service detects the foreground app package name so the app can determine whether the opened app is allowed.

If the foreground app is not allowed, Earn it! sends the user back to the Android home screen and opens the workout decision screen with choices to finish the workout, add 30 seconds of rest, or mark the exercise done.

The AccessibilityService API is used only while the app is in the decision lock state. It is not used to read messages, passwords, form text, notifications, or screen content for advertising or analytics. Foreground app package detection is processed locally on the device and is not transmitted to a server.

## 12. Demo Video Checklist For Google Play Review

Record a short video showing:

1. Open Earn it!
2. Show the Accessibility disclosure screen.
3. Check the consent checkbox.
4. Open Android Accessibility settings.
5. Enable the Earn it! Accessibility service.
6. Return to the app.
7. Start a workout with a short rest timer.
8. Wait until the timer reaches 0:00.
9. Open a non-allowed app.
10. Show Earn it! returning the user to the decision screen.
11. Press Exercise done or Finish workout.
12. Show blocking stops after finishing the workout.

## 13. Technical Handoff Summary

Frontend/backend architecture:

- Kotlin Android app.
- Compose UI.
- Local backend exposed through StateFlow and commands.
- Jetpack DataStore for local persistence.
- AccessibilityService for foreground app detection.
- AlarmManager fallback for timer wakeup.

Main backend API:

- `WorkoutController.state`
- `setRestDuration(seconds)`
- `setAllowedApps(packages)`
- `startWorkout()`
- `addThirtySecondsRest()`
- `exerciseDone()`
- `finishWorkout()`

State fields:

- Mode: Idle, Resting, AwaitingDecision.
- Rest duration seconds.
- Remaining seconds.
- Allowed apps.
- Permission status.
- Completed sets.

Important behavior rule:

Blocking only happens in `AwaitingDecision`. No apps should be blocked in Idle or Resting.

## 14. Important Review Risks

Main risk:

Google Play may closely review or reject apps that use AccessibilityService if the disclosure is unclear or if the use does not match the declared feature.

Mitigation:

- Keep the Accessibility disclosure visible before opening settings.
- Explain exactly what data is used.
- Say that the foreground package name is used only during the decision lock.
- Say that no screen content is read for ads or analytics.
- Provide a clear demo video.
- Do not add hidden analytics or ads before Play review.

Second risk:

If future versions add analytics, accounts, cloud sync, payments, or history, update Data Safety and the privacy policy before release.

## 15. Final Notes For Another AI

When helping submit this app to Google Play, do not describe Earn it! as a medical app. It is a fitness productivity tool, not a medical or health-data app.

Do not claim the app works as full device-owner/kiosk locking. It uses AccessibilityService to redirect blocked apps during the workout decision lock.

Do not claim the app blocks apps during the rest countdown. The intended behavior is: rest countdown allows normal phone use, then app blocking starts only after the timer reaches 0:00.

Do not say the app collects user data. The current MVP is local-only.

Before final publication, confirm the hosted privacy policy URL, developer email, release signing, screenshots, and final package name.
