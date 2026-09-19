# Earn it! Privacy Policy

Last updated: September 19, 2026

Earn it! (`com.fitness.restlock`) is an Android workout planner and rest timer. Apps remain usable during rest. After the timer expires, it redirects non-allowed apps to a workout decision screen until you choose the next action.

## Information stored and used on your device

Earn it! uses local Android DataStore storage for:

- Rest duration, allowed app package names, current session phase, timer end time, completed/planned set counts, and extra-rest count.
- Active saved-workout identity and start time, plus completion information used to recover an interrupted save.
- Saved workout routines: ID, name, creation time, exercise IDs, sets, reps, and exercise order.
- Workout history: routine name, start/completion times when available, elapsed minutes when known, completed/planned sets when known, exercises reached, and estimated calories. Older records may lack timing or set counts.
- A pending workout summary, retained until dismissed.
- Profile fields you enter: age, sex, weight in kilograms, and height in centimetres, used for calorie estimates.

The app retains up to 30 saved routines and 30 workout logs. Home shows the latest three logs. Finishing a saved workout, including ending early, records its result. Quick start does not create a structured workout log. Calorie values are estimates, not medical measurements.

Earn it! queries launchable installed apps to display the allowed-app picker. The selected package names are saved locally. It has no account system or remote Earn it! backend and does not upload workout, profile, settings, or session data to an Earn it! server.

## Accessibility

With the service enabled, during an active workout (`Resting` and `AwaitingDecision`) Earn it! observes foreground application package-name changes. It temporarily remembers the latest package so it can act if that app is already open when rest expires. It ignores these events in `Idle`.

Apps remain usable throughout the rest countdown. Blocking occurs only in `AwaitingDecision`, after the timer reaches 0:00. If a foreground app is not allowed or exempt, Earn it! sends you to Android Home and opens the workout decision screen. You can mark **Exercise done** (or **Finish final set**), choose **+30s rest**, or finish the workout. Essential system apps and the user's allowed apps remain available.

Accessibility does not read screen text, messages, passwords, form contents, notification contents, browser contents, or Accessibility node trees. Window-content retrieval and gesture capability are disabled. Package observations are processed locally and are not sent by Earn it! to advertising or analytics services. Development builds can display and log package diagnostics; these diagnostics are disabled in release builds.

An in-app disclosure and consent checkbox precede first enabling the service in Android Accessibility settings. You can disable the service in those settings. This is a fitness utility, not a disability-focused accessibility tool.

## Advertising and network use

Earn it! includes Google AdMob rewarded ads through Google Mobile Ads SDK 24.7.0. The SDK initializes when the application starts and attempts to preload an ad immediately after initialization. Network activity can therefore occur before you choose creator support. Further loading can occur on demand or after an ad closes or fails to show.

Ad display is optional: you choose **Yes, support the creator** in the support dialog. The workout has already ended and blocking has stopped before this flow. Declining, dismissing, or an ad failure does not keep the workout locked.

Google AdMob may process advertising-related data according to Google's SDK behavior and policies. See [Google's privacy policy](https://policies.google.com/privacy) and [Mobile Ads data disclosure](https://developers.google.com/admob/android/privacy/play-data-disclosure). Google maintains that disclosure for its current SDK; the developer must verify the applicable behavior for the version and configuration shipped here before publication. Local workout storage does not mean the entire app is offline.

## Data controls, deletion, and Android backup

You can edit your profile and saved routines. Deleting a saved routine removes its template but preserves existing workout logs; an active routine cannot be deleted or edited. There is no individual history-deletion screen. Dismissing a summary preserves its history entry.

Clearing Earn it!'s application storage or uninstalling removes its local app storage. Android backup or device restoration may restore data separately. The manifest allows backup; Android 12+ rules exclude app data from cloud backup, while older Android versions have no explicit legacy backup exclusions. Device transfer also depends on the platform and device. Do not treat uninstalling as deletion of any system-managed backup. See [Android backup behavior](https://developer.android.com/identity/data/autobackup).

## Contact and publication

TODO: developer must provide contact email before publication.

TODO: host this privacy policy and provide the public URL before Play submission.

Publication review: the current app has no integrated regional advertising consent/CMP flow. The developer must resolve applicable consent requirements and verify SDK disclosures before release; see [the repository's submission checklist](GOOGLE_PLAY_DECLARATIONS.md).
