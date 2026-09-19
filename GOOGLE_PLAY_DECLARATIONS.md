# Earn it! — Google Play declaration drafts

Source reviewed: September 19, 2026. These are repository drafts, not submitted forms or a guarantee of approval. Package: `com.fitness.restlock`.

## Accessibility API declaration

Earn it! uses AccessibilityService for its core rest-expiry app-redirection feature. During an active workout, including the rest countdown, the service observes foreground package-name changes from window-state events and temporarily remembers the latest package. This enables redirection when rest expires even if another app is already open. Events are ignored while idle.

Normal apps remain usable in `Resting`. Only in `AwaitingDecision`, after the rest timer reaches 0:00, does Earn it! enforce the allowlist. A non-allowed, non-exempt foreground app is redirected through Android Home to the workout decision screen. Actions are **Exercise done**, **Finish final set** on the final planned set, **+30s rest**, and **Finish/Finish workout**. Completing or finishing the session stops blocking; extra rest also stops blocking during its countdown. Essential system packages remain exempt.

Only package metadata is used. Earn it! does not read screen text, messages, passwords, forms, notification contents, browser contents, or Accessibility node trees. `canRetrieveWindowContent=false`, `canPerformGestures=false`; only `typeWindowStateChanged` is requested. Observations remain local and are not passed to advertising or analytics. Release builds do not emit package diagnostics.

The app is not a disability-focused accessibility tool and does not declare `isAccessibilityTool=true`. First enabling the service requires reviewing an in-app disclosure, selecting the consent checkbox, and opening Android Accessibility settings. The user can disable the service there. See [Google Play's Accessibility policy](https://support.google.com/googleplay/android-developer/answer/10964491) and [disclosure/consent guidance](https://support.google.com/googleplay/android-developer/answer/11150561).

## In-app disclosure alignment

`PermissionOnboardingScreen` and `accessibility_service_description` explain active-workout package observation, normal phone use during rest, expiry-only blocking, and no screen-content access. The onboarding also distinguishes local workout/profile/history storage from AdMob network processing. Accessibility consent is not advertising consent.

## Review video checklist

1. Show Earn it!'s disclosure and unchecked consent; demonstrate that first opening Accessibility settings is disabled until consent.
2. Check consent, open Android Accessibility settings, and enable Earn it!.
3. Start a short rest timer and use a non-allowed app normally during the countdown.
4. Remain in that app until 0:00; show redirection to the decision screen.
5. Show Exercise done and +30s rest returning to an unblocked countdown.
6. Show Finish final set completing a saved routine and opening its summary/history.
7. Show Finish workout ending early and removing the lock. Show allowed/essential apps remaining usable.

## Local data and Data Safety draft

Earn it! has no account or remote backend. Its DataStore records include:

- Rest settings, allowed package names, current session phase/timer, set counts, extra rests, active routine/start time, and completion recovery information.
- Saved routine IDs/names/creation times and ordered exercise IDs/sets/reps (up to 30 routines).
- Workout logs (up to 30): name, timestamps, duration where known, reached exercise count, completed/planned set counts where known, and calorie estimates; plus the pending summary.
- Profile age, sex, weight, and height for calorie estimates.

Foreground package observations are transient during active workouts, with debug-only development diagnostics. Installed launcher apps are queried for the allowlist picker. No code sends the above fitness/profile/session data to an Earn it! backend or supplies it to AdMob requests.

Deleting a routine preserves logs. No individual log-deletion UI exists. Clearing application storage or uninstalling removes local storage, subject to possible Android backup/restore. `allowBackup=true`; Android 12+ cloud exclusions exist, but legacy exclusions do not. Audit older Android backup and device transfer before promising exclusive on-device retention; see [Android backup documentation](https://developer.android.com/identity/data/autobackup).

Google Play distinguishes on-device processing from off-device collection, and requires third-party SDK practices to be included. Do not infer final checkboxes just from the absence of an Earn it! backend. See [Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469).

**VERIFY IN CURRENT GOOGLE PLAY / ADMOB DOCUMENTATION BEFORE SUBMISSION:** collection/sharing, purposes, optionality, data-type mapping, encryption/deletion answers, and applicable regional configuration for every SDK in the shipped artifact. Include profile and fitness/history processing in the assessment rather than omitting these features.

## AdMob and consent release work

The app contains ads: Google Mobile Ads SDK **24.7.0**. `RestLockApp.onCreate()` initializes AdMob and its completion callback preloads a rewarded ad. Loading is not gated by the support button. Display is requested only after **Yes, support the creator**; the session has already ended. Quick start leads to support directly; saved workouts first show a result with an optional support action. Ad failure permits continuation.

Debug uses Google's sample IDs; release uses the configured production IDs. Do not copy production IDs into documentation or review reports.

Google AdMob may process advertising-related data according to Google's SDK behavior and policies. The [current Mobile Ads disclosure](https://developers.google.com/admob/android/privacy/play-data-disclosure) describes IP addresses, product interactions, diagnostics, and device/account identifiers, but currently targets a newer SDK than 24.7.0. This is a verification checklist, not a verified exhaustive category list for this build. Confirm shipped SDK behavior and console configuration before selecting final answers.

No UMP/CMP consent flow is integrated in current app code. Regional consent configuration and any required implementation remain release work, including reviewing startup initialization/preloading. Consult [Google's EEA/UK/Switzerland CMP requirements](https://support.google.com/admob/answer/13554116) and [EU user consent guidance](https://www.google.com/about/company/user-consent-policy-help/). The optional support prompt does not replace advertising consent. Part 6 does not add a CMP or change the advertising flow.

## Permissions and timer implementation

The app declares a system-bound Accessibility service. Its merged debug manifest also includes SDK/library permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_ADSERVICES_TOPICS`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, and its signature-protected dynamic-receiver permission. A library permission does not mean Earn it! runs a workout foreground service: there is no workout notification/service implementation.

No `QUERY_ALL_PACKAGES`, `SCHEDULE_EXACT_ALARM`, or `POST_NOTIFICATIONS` permission is requested by the current app. Package visibility uses launcher/home intent queries. Rest timing uses coroutine ticking and `AlarmManager.setAndAllowWhileIdle`; background delivery can be delayed. Do not promise exact delivery while the process is stopped.

## Human input before submission

- TODO: developer must provide contact email before publication.
- TODO: host the privacy policy and provide its public URL; connect it to the listing and in-app access before publication.
- Complete Play Console app access (no login), ads, target audience/content rating, health/fitness, Data Safety, and Accessibility declarations based on the actual distribution and SDK configuration.
- Record the demonstration video, resolve regional consent/CMP requirements, and review backup behavior on supported Android versions.
- Verify the highest previously uploaded version code and release artifact using README's existing signing workflow.

These drafts do not submit forms or establish legal compliance.
