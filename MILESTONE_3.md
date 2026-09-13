# Milestone 3: saved workout management

Branch: `feature/workout-management-hardening`. Target: `master`, retaining the
user-approved target from Part 2 because this repository has no `main` branch.
The branch starts from `e5ddfa6`, which includes merged Parts 1 and 2.

## Deletion and active-session protection

The Workouts screen exposes Start and an overflow menu containing Edit and
Delete. Delete opens a confirmation naming the routine and explaining that
history remains. Cancel does not mutate storage. Deleting an unknown ID is a
no-op; deleting an active routine is rejected with an explanation to finish the
current workout first. Only saved routines are removed; logs and the pending
completion summary are independent and remain intact.

Save and delete check active identity within the repository's DataStore edit.
They also consult the persisted backend identity, protecting the interval after
process restart before fitness metadata is restored. The session adapter holds
the same per-DataStore mutex from selecting a routine through starting the
session and persisting its active ID. A concurrent delete or edit cannot remove
or replace the definition chosen for that start. The storage retention cap
cannot evict the active routine either.

## Builder behavior

- Empty routines are rejected by the ViewModel and repository.
- Names are trimmed; whitespace-only names become `Workout` without changing
  meaningful capitalization.
- Exercise normalization is centralized: stable ascending rank, first occurrence
  per exercise ID, known catalog IDs only, domain set/rep limits, contiguous ranks.
- Removing an exercise immediately recalculates selection, ranks, total sets,
  total reps, duration estimate, and calorie estimate.
- First-up, last-down, and unknown-item moves are safe no-ops.
- Edits preserve ID and original creation time. The repository rejects a stale
  edit if its routine was deleted instead of recreating it.
- Active routine edits are blocked by repository validation in addition to UI
  protections. Builder mutations do not modify a running definition.
- Builder instances use UUID identity. Repeated Save does not create duplicate
  routines; an older asynchronous save cannot close a newer builder. Storage
  errors keep the user's draft available for retry.

## Repository contract

`saveWorkout(workout, requireExisting = false)` returns `WorkoutMutationResult`.
An edit supplies `requireExisting = true`. `deleteWorkout(workoutId)` returns the
same result type: `Success`, `ActiveWorkout`, `InvalidWorkout`, or `NotFound`.

`withWorkoutStartLock(action)` coordinates session starts with routine changes.
The production implementation shares its mutex across repositories using the
same DataStore. The application supplies the backend active-ID reader to
`BackendFitnessRepository`; its default is suitable for standalone storage tests.

## Persistence compatibility

Supported formats remain **workout-v1, workout-v2, log-v1, and log-v2**. No new
serialization version or database is introduced.

Legacy workouts without explicit sets, reps, or rank use the existing defaults
and list position. Current workouts preserve ID, normalized name, original
creation time, exercise IDs, sets, reps, and repaired rank. Unknown catalog IDs
and duplicate exercise IDs are removed during normalization; valid entries in
the same routine remain usable. An all-invalid/empty routine is not exposed as a
startable routine. Malformed records are isolated so valid neighboring records
continue loading. Existing logs are not recalculated when routines are edited
or deleted; unknown legacy log fields stay unknown.

The existing limits of 30 saved routines and 30 logs remain. Unsupported versions
and irrecoverably malformed records are skipped; there is no whole-store reset.

## Verification

Use Android Studio's bundled JDK on this machine:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Final verification: `testDebugUnitTest` passed all 86 tests with zero failures,
errors, or skips; `assembleDebug` succeeded; `lintDebug` completed with zero
errors and 188 warnings. The combined command finished successfully in 1m 16s.
The new concurrent start/delete/edit
regression failed before synchronization was added and passed after the fix.
Tests exercise the real Preferences DataStore, including multiple repository
instances and file reopen, in addition to domain and ViewModel behavior.

Manual device checks remain: create Push/Pull/Leg routines, edit sets and reorder,
remove an exercise and inspect estimates, cancel/confirm Pull deletion, verify
its history remains, try active-routine edit/delete, and reopen old stored data.
Also smoke-test prior workout progression, completion summary, and blocking only
in AwaitingDecision. Manual Android UI checks are not claimed without a device.

## Deferred

No Part 4 work: Accessibility hardening, notifications, API 36, release signing,
Google Play/privacy changes, localization, CI, images, analytics dashboards,
cloud sync, or accounts.
