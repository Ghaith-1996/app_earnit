# Milestone 2: accurate workout completion and history

Branch: `feature/workout-completion-history`. Integration target: `master`,
explicitly approved because this repository has no `main` branch.

## Phase status

- Phase 4 — Active workout metadata: implemented.
- Phase 5 — Accurate history: implemented.
- Phase 6 — Completion summary: implemented.

## Behavior

Starting a saved routine persists its ID and original wall-clock start time.
The backend stores both with the session-start transition, then the adapter
copies them into the fitness repository. On process restart, a running backend
session restores this metadata before accepting further commands. This also
covers interruption between the two stores' initial writes.

Finishing captures completed sets and the finish timestamp from the controller's
persisted transition before live counters reset to Idle. The final planned set
is included; Finish early does not add a set. A durable completion receipt allows
logging to resume after process death or a temporary fitness-storage failure.
Blocking and the timer end before logging and before any optional ad.

The fitness repository atomically creates a log, stores that same log as the
pending summary, and clears active metadata. A second finish finds no active ID
and creates no duplicate. The adapter acknowledges the backend receipt after
the fitness transaction. Restarting after logging but before acknowledgement
does not reconstruct the cleared active ID or duplicate the log.

Quick Start stays an open-ended rest-lock session and creates no structured log.
The builder's old Log now action was removed because it fabricated a completion
from an unperformed plan. Saving and starting routines remain available.

## Results and rounding

`ActiveWorkoutSession` contains `workoutId` and nullable `startedAtMillis`.
New sessions always have a start; null represents legacy timing that was never
recorded, rather than pretending the user started at app reopening.

`WorkoutLog` keeps its existing name, finish time, calories, exercise count, and
duration fields. It adds start time and nullable completed/planned set counts.
Completion status is derived from the counts, so it cannot disagree with them.
Historical counts and calorie totals are stored rather than recalculated when
the user later edits their routine or profile.

`WorkoutResults` distributes completed sets in ranked exercise order. For a
4-set exercise followed by a 3-set exercise, five completed sets mean 4 + 1;
untouched exercises are excluded. Exercise count means exercises with at least
one acknowledged set. Calories reuse the existing MET/profile approximation on
only this completed work. Zero completed sets mean zero estimated calories.

Duration is `max(0, completedAt - startedAt) / 60_000`, rounded down to whole
minutes. Thus 59 seconds displays 0 min and 60 seconds displays 1 min. Backwards
clock changes produce zero; arithmetic overflow saturates safely. Unknown
legacy start time produces unknown duration, displayed as unavailable.

## Backward compatibility

- Existing saved-workout formats (`workout-v1` and `workout-v2`) remain readable.
- Existing `active_workout_id` remains readable. Missing start time stays null.
- New records use `log-v2`; `log-v1` records retain their original duration,
  calories, name, finish time, and exercise count. Missing start/count/status
  fields remain unknown. Rewriting the log list preserves those unknown values.
- Percent-encoded legacy names, literal plus signs, Unicode, and separators are
  preserved. There is no database reset or destructive migration.
- Missing or empty referenced workouts clear active metadata without a fake log.
- The existing retention limits remain 30 saved routines and 30 recent logs.

## Summary and support

Home shows the persisted summary after full completion or early finish, even
after process restart. Navigation returns to Home when a pending result appears.
It contains duration, completed/planned sets, exercises reached, approximate
calories, and completion status. Done dismisses it persistently. The optional
support action comes after the saved result and never controls logging.

BlockerActivity waits for completion bookkeeping, then opens Home for the
summary. Completion state lives in its ViewModel so activity rotation does not
lose the handoff. Returning to Resting always dismisses the blocker, including
a rapid +30s followed by a stale final-set click. Quick Start still offers the
optional support flow after ending; declining or ad failure permits exit.

## Automated verification

Using Android Studio's bundled JDK:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

The final command results are recorded in the pull request and completion report.
Coverage includes real file-backed fitness DataStore reopen, both session and
fitness store reopen during a 42-minute workout, interrupted start metadata
handoff, interrupted finish recovery, Home/Blocker double finish, full and partial
results, distribution boundaries, duration bounds, zero-work calories, legacy
decoding, v2 roundtrip, summary identity/dismissal, and missing references.

## Remaining device verification

No device or emulator was connected (`adb devices` returned an empty list), so
no manual Android UI or AdMob scenario is claimed as tested.

1. Start a routine from Home and Workouts; finish all sets and inspect the summary.
2. End after 3 of 12 sets; verify history/summary both show 3/12 and Ended early.
3. Finish from BlockerActivity; rotate during saving and check summary handoff.
4. On the final set, tap +30s then Exercise done rapidly; the blocker must close.
5. Kill/reopen the process during a routine; finish and verify original duration.
6. Finish twice from Home/Blocker; verify exactly one history record.
7. Decline support or simulate ad load/show failure; the workout remains finished.
8. Verify apps are usable in Idle/Resting and blocked only in AwaitingDecision.

## Files changed

Paths below are relative to the repository root.

- `MILESTONE_2.md`
- `app/src/main/java/com/fitness/restlock/backend/WorkoutController.kt`
- `app/src/main/java/com/fitness/restlock/backend/persistence/WorkoutPreferencesStore.kt`
- `app/src/main/java/com/fitness/restlock/backend/session/DefaultWorkoutController.kt`
- `app/src/main/java/com/fitness/restlock/backend/session/WorkoutSessionSnapshot.kt`
- `app/src/main/kotlin/com/restlock/BlockerActivity.kt`
- `app/src/main/kotlin/com/restlock/domain/FitnessModels.kt`
- `app/src/main/kotlin/com/restlock/domain/FitnessRepository.kt`
- `app/src/main/kotlin/com/restlock/domain/SessionEngine.kt`
- `app/src/main/kotlin/com/restlock/domain/WorkoutResults.kt`
- `app/src/main/kotlin/com/restlock/domain/backend/BackendFitnessRepository.kt`
- `app/src/main/kotlin/com/restlock/domain/backend/BackendSessionEngine.kt`
- `app/src/main/kotlin/com/restlock/domain/fake/FakeSessionEngine.kt`
- `app/src/main/kotlin/com/restlock/ui/BlockerViewModel.kt`
- `app/src/main/kotlin/com/restlock/ui/HomeViewModel.kt`
- `app/src/main/kotlin/com/restlock/ui/WorkoutViewModel.kt`
- `app/src/main/kotlin/com/restlock/ui/nav/RestLockNavGraph.kt`
- `app/src/main/kotlin/com/restlock/ui/screens/HomeScreen.kt`
- `app/src/main/kotlin/com/restlock/ui/screens/WorkoutScreen.kt`
- `app/src/main/kotlin/com/restlock/ui/screens/WorkoutSummaryDialog.kt`
- `app/src/test/kotlin/com/restlock/domain/WorkoutResultsTest.kt`
- `app/src/test/kotlin/com/restlock/domain/backend/BackendFitnessRepositoryTest.kt`
- `app/src/test/kotlin/com/restlock/ui/WorkoutFinishLoggingTest.kt`

## Deferred

Part 3 remains excluded: routine deletion, builder validation overhaul,
Accessibility hardening, notification permission cleanup, API 36, release/signing,
privacy declarations, localization, CI, images/optimization, and repository cleanup.
