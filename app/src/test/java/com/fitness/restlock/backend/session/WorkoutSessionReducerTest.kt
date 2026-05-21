package com.fitness.restlock.backend.session

import com.fitness.restlock.backend.WorkoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionReducerTest {
    @Test
    fun startWorkoutStartsRestAndResetsCompletedSets() {
        val snapshot = WorkoutSessionSnapshot(
            restDurationSeconds = 60,
            completedSets = 4,
            extraRests = 2,
        )

        val result = WorkoutSessionReducer.startWorkout(snapshot, nowMillis = 1_000L)

        assertEquals(WorkoutMode.Resting, result.mode)
        assertEquals(61_000L, result.timerEndEpochMillis)
        assertEquals(0, result.completedSets)
        assertEquals(0, result.extraRests)
    }

    @Test
    fun restingExpiresToAwaitingDecision() {
        val snapshot = WorkoutSessionSnapshot(
            mode = WorkoutMode.Resting,
            timerEndEpochMillis = 5_000L,
        )

        val result = WorkoutSessionReducer.expireIfNeeded(snapshot, nowMillis = 5_000L)

        assertEquals(WorkoutMode.AwaitingDecision, result.mode)
        assertEquals(0L, result.timerEndEpochMillis)
    }

    @Test
    fun addThirtySecondsOnlyWorksWhenAwaitingDecision() {
        val snapshot = WorkoutSessionSnapshot(mode = WorkoutMode.AwaitingDecision)

        val result = WorkoutSessionReducer.addThirtySecondsRest(snapshot, nowMillis = 10_000L)

        assertEquals(WorkoutMode.Resting, result.mode)
        assertEquals(40_000L, result.timerEndEpochMillis)
        assertEquals(1, result.extraRests)
    }

    @Test
    fun exerciseDoneIncrementsSetAndRestartsOriginalRest() {
        val snapshot = WorkoutSessionSnapshot(
            mode = WorkoutMode.AwaitingDecision,
            restDurationSeconds = 75,
            completedSets = 2,
        )

        val result = WorkoutSessionReducer.exerciseDone(snapshot, nowMillis = 10_000L)

        assertEquals(WorkoutMode.Resting, result.mode)
        assertEquals(85_000L, result.timerEndEpochMillis)
        assertEquals(3, result.completedSets)
    }

    @Test
    fun finishWorkoutResetsActiveSessionButKeepsSettings() {
        val snapshot = WorkoutSessionSnapshot(
            mode = WorkoutMode.AwaitingDecision,
            restDurationSeconds = 120,
            allowedApps = setOf("com.social.app"),
            timerEndEpochMillis = 44_000L,
            completedSets = 3,
            extraRests = 2,
        )

        val result = WorkoutSessionReducer.finishWorkout(snapshot)

        assertEquals(WorkoutMode.Idle, result.mode)
        assertEquals(0L, result.timerEndEpochMillis)
        assertEquals(0, result.completedSets)
        assertEquals(0, result.extraRests)
        assertEquals(120, result.restDurationSeconds)
        assertEquals(setOf("com.social.app"), result.allowedApps)
    }

    @Test
    fun restDurationIsClampedToSupportedRange() {
        val short = WorkoutSessionReducer.setRestDuration(
            WorkoutSessionSnapshot(),
            seconds = 0,
        )
        val long = WorkoutSessionReducer.setRestDuration(
            WorkoutSessionSnapshot(),
            seconds = 9_999,
        )

        assertEquals(WorkoutSessionReducer.MIN_REST_SECONDS, short.restDurationSeconds)
        assertEquals(WorkoutSessionReducer.MAX_REST_SECONDS, long.restDurationSeconds)
    }

    @Test
    fun remainingSecondsRoundsUp() {
        val snapshot = WorkoutSessionSnapshot(
            mode = WorkoutMode.Resting,
            timerEndEpochMillis = 2_001L,
        )

        assertEquals(3, WorkoutSessionReducer.remainingSeconds(snapshot, nowMillis = 0L))
        assertTrue(WorkoutSessionReducer.remainingSeconds(snapshot, nowMillis = 2_001L) == 0)
    }
}
