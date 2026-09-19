# Earn it! — product and Google Play handoff

Reviewed September 19, 2026 against current source. Public name: **Earn it!**. Android application ID: `com.fitness.restlock`. Native Kotlin/Jetpack Compose; min SDK 26, compile/target SDK 36. The default UI language is English; no complete second-language translation is provided.

## Product

Earn it! combines workout routines with a rest timer that redirects distractions when rest ends. It is a fitness utility with calorie estimates, not a medical app or full device-owner/kiosk lock.

- Quick start with preset/custom rest (5 seconds to 60 minutes).
- Choose apps allowed during lock. An empty allowlist means strict mode, with essential system apps exempt.
- Create a named routine from catalog exercises, configure sets/reps, reorder exercises, edit/delete saved routines, and start a saved workout. An active routine cannot be edited/deleted; the UI prevents another start during a session.
- Progress through sets/exercises. **Exercise done** marks a set and resumes rest; **Finish final set** completes the last planned set without another countdown. **+30s rest** adds rest without completing a set. **Finish/Finish workout** can end early.
- Saved workouts produce a completion/early-ending summary and local history. Home displays the latest three sessions; storage retains up to 30 logs. Quick start has no structured log.
- Optional profile (age, sex, weight, height) refines estimated calories. These estimates are not medical measurements.
- Optional rewarded-ad creator support after completion. Blocking diagnostics are only for debug builds.

## Accessibility: observation versus action

The enabled service observes foreground application package-name changes during active workouts, in both `Resting` and `AwaitingDecision`, and remembers the latest package. It ignores events in `Idle`. This allows enforcement when the user is already inside another app at expiry.

| Phase | Blocking |
| --- | --- |
| Idle | None |
| Resting | None; normal phone use during countdown |
| AwaitingDecision | Allowlist enforcement after 0:00 |

For a non-allowed, non-exempt app in the decision state, the service sends the user to Android Home and opens the workout decision screen. Essential launchers/settings/dialer/system surfaces remain available. The exact policy is in `AppBlockPolicy`, `AndroidAppBlockPolicy`, and `KnownExemptPackages`.

Only package metadata is used. Screen text, messages, passwords, form contents, notification contents, browser contents, and Accessibility node trees are not read. `canRetrieveWindowContent=false`; gesture capability is disabled. Metadata is processed locally, not supplied to ads/analytics. Package diagnostic output is debug-only.

The scrollable in-app disclosure and explicit checkbox precede first enabling Accessibility in Android settings. Earn it! is not a disability-focused accessibility tool. Preserve disclosure and user control.

## Storage and deletion

Two local DataStore stores hold settings/allowlist/session timing and recovery data, and fitness data: profile age/sex/weight/height, saved routine IDs/names/creation dates, exercise IDs/sets/reps/order, active routine/start time, logs, and a pending summary. Logs contain routine name, completion/start timestamps and elapsed duration where known, reached exercises, completed/planned set counts where known, and estimated calories. Legacy records may lack fields. Capacity is 30 routines and 30 logs.

Deleting a routine removes the template and preserves history. Dismissing a summary preserves its log. There is no individual history-deletion screen. Clearing app storage or uninstalling removes local app storage, but Android backup/restore is separate. The manifest allows backup; Android 12+ cloud exclusions are configured, while legacy backup exclusions are absent. Do not promise that Android can never back up or transfer data.

No account, remote Earn it! backend, cloud-sync feature, or app analytics service exists. The application does not upload workout/profile/settings/session records to an Earn it! server. This does not mean all app activity is offline.

## Advertising and submission

Google Mobile Ads SDK 24.7.0 initializes at application startup and immediately attempts rewarded-ad preloading. On-demand loading and replenishment also exist. Display requires **Yes, support the creator**. Saved workouts show a summary first, with an optional support action; quick start may show support directly after ending. The session is already idle, and declining/dismissing/ad failure allows exit.

Google AdMob may process advertising-related data according to Google's SDK behavior and policies. No fitness/profile/package observations are explicitly supplied in the app's ad requests. There is no integrated UMP/CMP flow; regional consent configuration and implementation may be required before release. Do not add a consent SDK silently in this milestone.

Use [the privacy policy](PRIVACY_POLICY.md) as the maintained privacy draft and [Google Play declarations](GOOGLE_PLAY_DECLARATIONS.md) for SDK categories, current official policy references, permissions, the review video, and outstanding submission decisions. **VERIFY IN CURRENT GOOGLE PLAY / ADMOB DOCUMENTATION BEFORE SUBMISSION**: exact Data Safety answers, SDK-version applicability, audience/content rating, and regional consent. Do not claim approval or legal compliance.

TODO: developer must provide contact email before publication.

TODO: host this privacy policy and provide the public URL before Play submission; add in-app access to that URL.

## Store listing draft

Short description: Workout rest timer that blocks distractions when rest is over.

Earn it! helps you return to training after rest. Build a routine, choose exercises and sets, and set your rest interval. Use your phone normally during the countdown. At 0:00, non-allowed apps redirect to your workout decision screen. Mark Exercise done, add 30 seconds, or finish the workout. Finish final set completes a saved routine; see your summary and recent workout history with approximate calories.

Save and edit routines, choose allowed apps, and personalize calorie estimates with an optional profile. Workout, profile, settings, and session records use local storage without an Earn it! account or backend. Google AdMob supports optional rewarded advertising and may use network data even before an ad is chosen.

Accessibility disclosure: Earn it! observes foreground package-name changes during active workouts, including rest, but blocks only after the countdown expires. It does not read screen text, messages, passwords, forms, notifications, browser contents, or node trees. Users review the disclosure and consent before enabling the service.

Suggested screenshots: Home/rest timer, routine builder, allowed-app picker, decision screen, disclosure, and completion/history. No new artwork is part of this milestone.

## Technical handoff

`RestLockApp` wires real backend adapters: `SessionEngine`, `SettingsRepository`, `FitnessRepository`, `InstalledAppsProvider`, and `PermissionGateway`. `WorkoutController` owns timing/state through DataStore, coroutine ticking, and inexact `AlarmManager.setAndAllowWhileIdle`. There is no workout countdown foreground service or Room storage. Fakes support tests.

Use [BACKEND_HANDOFF.md](BACKEND_HANDOFF.md) for completion/recovery and [README.md](README.md) for debug checks and the existing release-signing/versioning workflow. Preserve package identity, final-set behavior, allowlist timing, and ad control flow. CI, exercise-image work, and new backend services are outside Part 6.
