# Part 6 — compliance and production copy verification

Reviewed September 19, 2026. Base: `bd99e12` on `origin/master` (Part 5 merged).
Branch: `chore/play-compliance-polish`. No existing Part 6 branch/PR was found.

## Source inventory and corrections

- `WorkoutPreferencesStore`: rest settings, allowed packages, session mode/timer,
  completed/planned sets, extra rests, active routine/start time and completion recovery.
- `BackendFitnessRepository`: age/sex/weight/height, up to 30 saved routines and 30 logs,
  active metadata and pending summary. Routine plans include exercise IDs/sets/reps/order.
  Logs include timestamps/duration and counts where known, reached exercises, and calories.
- Home displays three recent logs. Quick start has no structured log. Routine deletion
  preserves history; there is no individual log-deletion UI.
- `RestLockApp` initializes Google Mobile Ads 24.7.0 at startup; its callback preloads.
  Display is optional after the session ends. No integrated UMP/CMP flow is present.
- Accessibility observes active-workout package metadata in both `Resting` and
  `AwaitingDecision`; idle events are ignored. Enforcement remains AwaitingDecision-only.
  Window-content retrieval and gestures are disabled; no screen text/content or node reads.
- Home diagnostics and service package logs already use `FLAG_DEBUGGABLE`; preserved.
- The actual action remains **Exercise done**, or **Finish final set** for the final planned
  set. Internal API names and progression were preserved.

Updated `PRIVACY_POLICY.md`, `AGENTS.md`, `APP_BRIEF_FOR_AI.md`,
`GOOGLE_PLAY_DECLARATIONS.md`, `BACKEND_HANDOFF.md`, and `README.md` to correct missing
history/profile/routine documentation, obsolete fake wiring/Room/foreground-service
claims, Accessibility observation timing, pre-completion support-flow claims, and
overbroad local/offline language. Removed production AdMob IDs from handoff/declaration
text. Public branding is **Earn it!**; application ID remains `com.fitness.restlock`.

Android backup caveat is explicit: `allowBackup=true`, Android 12+ cloud exclusions,
no legacy backup rules, and platform-dependent device transfer. No backup policy changed.
Current official Google references and SDK-version uncertainty are in the declaration
draft. Final Console answers are not asserted or submitted.

## Copy and localization foundation

Default resources contain 175 strings and nine plural groups. Migrated:

- HomeScreen, WorkoutScreen, SettingsScreen, AppPickerScreen.
- PermissionOnboardingScreen, BlockerScreen, SetupSheet.
- SupportCreatorDialog, WorkoutSummaryDialog, MainBottomBar.
- StateChip and compact duration formatting in Format.
- WorkoutViewModel management messages now carry `@StringRes` IDs, resolved by Compose.
  Existing tests assert the corresponding message IDs while retaining behavior assertions.

The onboarding remains scrollable and retains its consent gate. It now identifies
local profile/history data, separates AdMob startup loading from Accessibility consent,
and explicitly excludes browser contents and node trees. The service description and
privacy/Play drafts agree with the service's observation-versus-enforcement behavior.

Counts for allowed apps, exercises, sets, reps and summary progress use plurals.
Dynamic text uses positional arguments. `kcal` unit labels have narrowly scoped lint
annotations because the abbreviation is invariant; noun counts use plurals.

English is the default. No additional translation was added. Intentional remaining
literals/content: debug-only diagnostic text, animation labels/routes/identifiers,
clock/date formats, exercise catalog labels and artwork equipment abbreviations,
user-entered/stored routine names and existing persisted default names (`New workout`,
`Workout`). Domain data and catalog content were not migrated or rewritten.

## Automated verification

Commands used the existing Android Studio JDK and unmodified Gradle configuration:

| Check | Result |
| --- | --- |
| `testDebugUnitTest` before changes | PASS |
| `lintDebug` before changes | PASS; 0 errors, 154 warnings |
| `testDebugUnitTest` after changes | PASS; 106 tests, 0 failures, 0 errors |
| `lintDebug` after changes | PASS; 0 errors, 154 warnings |
| `assembleDebug` after changes | PASS; debug APK produced |
| `git diff --check` | PASS |
| Independent whole-change review | No blocking findings or accidental business-logic changes |

An intermediate test run caught an overly broad resource-ID expectation for a stale
edit of a deleted workout. The assertion now expects the existing, more specific
builder message; production behavior was unchanged. The final full suite passes.

Optional `processReleaseResources` was attempted but invokes Part 5's signing guard
and failed because local signing credentials are absent. The guard was preserved;
no signed release artifact was requested or produced. Release diagnostics were
verified by inspecting the existing flag checks, not a release runtime test.

## Device smoke verification

Used a fresh disposable Android API 37 emulator with the debug APK. Initial ADB
authorization/boot issues were resolved without changing the application. Test
profile/routine/allowlist data were confined to this emulator. Accessibility was
enabled through ADB as a test fixture; the complete Android Settings enable flow
was not exercised.

- Home and recent-history card: branding, rest label, singular allowed-app count,
  completed-set fraction, duration and calories rendered correctly.
- Workouts/builder: empty and saved states, catalog sections, exercise/set counts,
  creation and starting a one-exercise, one-set routine rendered correctly.
- Settings: profile fields and production labels rendered correctly.
- App picker: strict mode, zero apps and one selected app rendered correctly.
- Permission onboarding: top/bottom disclosure remained scrollable and readable;
  the Accessibility-settings button was disabled before consent.
- Blocker: app label, final-set action, progress and rest/finish actions rendered.
- Completion summary: one completed set, one reached exercise, duration, calories,
  Done and optional creator support rendered correctly; Home showed the saved log.
- Support dialog: both options rendered; No thanks returned to the completed state.
- Delete dialog: routine name, history-preservation explanation and both actions
  rendered correctly. Deleting the test routine removed it from saved workouts;
  both completed workout logs remained on Home.
- Home debug diagnostics were visible in this debug build.

Contacts, a non-allowed app, remained usable during the rest countdown. In a passive
repeat without UIAutomator inspection during the countdown, logs showed transition
to `AwaitingDecision` at 16:02:45.287 and blocking at 16:02:45.422; the blocker appeared
without reopening Contacts. Final-set completion returned to Idle and saved a summary.
An earlier attempt with UIAutomator inspection did not redirect until Contacts was
reopened. That result did not reproduce without the inspection tool; Accessibility
test-harness interference is suspected, not established as an application defect.

No missing-resource crashes, literal formatting placeholders or clipped copy were
observed on the inspected screens. This is one emulator/default font configuration,
not an exhaustive device, font-scale or language matrix. Release diagnostics remain
source-verified only because local release signing credentials are unavailable.
Screenshots and UI inspection artifacts are retained locally under the ignored
`build/part6-ui/` directory.

## Regression scope and remaining release work

No changes to session state/progression/final-set rules, fitness persistence/history
calculations/deletion behavior, Accessibility policy/service, AlarmManager, AdMob
control flow, signing/versioning/SDK configuration, application ID, or exercise images.
The ViewModel change affects presentation message values only.

Remaining: developer contact email, hosted privacy-policy URL and in-app access,
Play Console forms/final Data Safety and Accessibility submission/video, regional
consent/CMP setup and implementation, Android backup/device-transfer review, and
broader physical-device/release verification. Full translation/catalog localization remains separate work.
No CI, image generation, accounts, analytics, Firebase, or remote backend was added.
