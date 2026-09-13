# Milestone 1: saved workout sessions

This change covers saved-workout launch, set/exercise progression, and the final set.

## Behavior

- Home has a **Start saved workout** chooser. The play button beside a saved
  workout on the Workouts tab starts that routine and opens Home.
- Launch uses the selected rest interval, starts in `Resting`, and resets progress
  to zero completed sets. The preview shows set 1 of the first ranked exercise.
- At `0:00`, the session enters `AwaitingDecision`. Only this phase blocks apps.
- **Exercise done** completes one set and starts the next rest. Exercise changes
  happen at the planned set boundaries. **+30s rest** never advances a set.
- On the final planned set, the primary action reads **Finish final set**.
  It goes directly to `Idle`, cancels the alarm, clears the session counters and
  selected workout, and dismisses the blocker. There is no extra rest afterward.
- Quick sessions remain open ended. Manual Finish retains its existing support
  dialog. Existing completion logging is reused; no new history feature is added.
- Starting another session cannot replace an active one. Routine editing is
  disabled while a session is running so its exercise order and set counts stay stable.

## Implementation map

- `HomeScreen`, `WorkoutScreen`, `RestLockNavGraph`: launch wiring and final-set labels.
- `WorkoutViewModel`: reassign ranks after moving an exercise, so normalization
  preserves the chosen order.
- `BackendSessionEngine`: serializes launch/finish bookkeeping across activities.
  Existing active-workout selection and logging stay in `FitnessRepository`.
- `WorkoutSessionSnapshot` / `WorkoutPreferencesStore`: persist nullable
  `plannedSets` alongside the timer and completed-set counter. Missing values
  preserve the existing open-ended behavior for older sessions.
- `WorkoutSessionReducer`: enforces final-set completion and rejects duplicate
  starts while active. The controller awaits persisted transitions before the
  adapter performs completion bookkeeping.
- `WorkoutProgress`: its existing cumulative-set calculation is retained and
  covered with uneven set counts and exercise boundaries.

## Automated checks

Run `testDebugUnitTest` and `assembleDebug` with the Gradle wrapper.
The session integration tests use the real controller, reducer, adapter, and
file-backed Preferences DataStore. Android permissions, alarm delivery, and the
fitness repository are test doubles. Tests reopen the session DataStore to verify
recovery of the completed-set counter and final-set limit.

## Device smoke test

No device or configured emulator was available during implementation.

1. Save two exercises with 2 sets and 1 set. Move them up/down, save, and check order.
2. Choose a short rest and launch from each saved-workout entry point.
3. Verify apps remain usable during rest, then lock only after `0:00`.
4. Complete sets from Home and the blocker; check exercise, set number, and reps.
5. Use **+30s rest** on the final set; verify progress stays unchanged.
6. Choose **Finish final set**; verify Idle, no blocking, and no subsequent timer.
7. Start a quick session and verify it continues beyond the prior routine's set limit.
