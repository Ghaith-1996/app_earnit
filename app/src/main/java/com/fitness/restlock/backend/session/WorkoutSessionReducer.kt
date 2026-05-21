package com.fitness.restlock.backend.session

import com.fitness.restlock.backend.WorkoutMode
import kotlin.math.ceil

object WorkoutSessionReducer {
    const val MIN_REST_SECONDS = 5
    const val MAX_REST_SECONDS = 3_600
    const val ADDITIONAL_REST_SECONDS = 30

    fun setRestDuration(
        snapshot: WorkoutSessionSnapshot,
        seconds: Int,
    ): WorkoutSessionSnapshot {
        return snapshot.copy(restDurationSeconds = seconds.sanitizeRestSeconds())
    }

    fun setAllowedApps(
        snapshot: WorkoutSessionSnapshot,
        packages: Set<String>,
    ): WorkoutSessionSnapshot {
        return snapshot.copy(allowedApps = packages.asSanitizedPackageSet())
    }

    fun startWorkout(
        snapshot: WorkoutSessionSnapshot,
        nowMillis: Long,
    ): WorkoutSessionSnapshot {
        return snapshot.copy(
            mode = WorkoutMode.Resting,
            timerEndEpochMillis = nowMillis + snapshot.restDurationSeconds * 1_000L,
            completedSets = 0,
            extraRests = 0,
        )
    }

    fun addThirtySecondsRest(
        snapshot: WorkoutSessionSnapshot,
        nowMillis: Long,
    ): WorkoutSessionSnapshot {
        if (snapshot.mode != WorkoutMode.AwaitingDecision) return snapshot
        return snapshot.copy(
            mode = WorkoutMode.Resting,
            timerEndEpochMillis = nowMillis + ADDITIONAL_REST_SECONDS * 1_000L,
            extraRests = snapshot.extraRests + 1,
        )
    }

    fun exerciseDone(
        snapshot: WorkoutSessionSnapshot,
        nowMillis: Long,
    ): WorkoutSessionSnapshot {
        if (snapshot.mode != WorkoutMode.AwaitingDecision) return snapshot
        return snapshot.copy(
            mode = WorkoutMode.Resting,
            timerEndEpochMillis = nowMillis + snapshot.restDurationSeconds * 1_000L,
            completedSets = snapshot.completedSets + 1,
        )
    }

    fun finishWorkout(snapshot: WorkoutSessionSnapshot): WorkoutSessionSnapshot {
        return snapshot.copy(
            mode = WorkoutMode.Idle,
            timerEndEpochMillis = 0L,
            completedSets = 0,
            extraRests = 0,
        )
    }

    fun expireIfNeeded(
        snapshot: WorkoutSessionSnapshot,
        nowMillis: Long,
    ): WorkoutSessionSnapshot {
        if (snapshot.mode != WorkoutMode.Resting) return snapshot
        if (snapshot.timerEndEpochMillis <= 0L || snapshot.timerEndEpochMillis > nowMillis) {
            return snapshot
        }
        return snapshot.copy(
            mode = WorkoutMode.AwaitingDecision,
            timerEndEpochMillis = 0L,
        )
    }

    fun remainingSeconds(
        snapshot: WorkoutSessionSnapshot,
        nowMillis: Long,
    ): Int {
        if (snapshot.mode != WorkoutMode.Resting || snapshot.timerEndEpochMillis <= nowMillis) {
            return 0
        }
        val remainingMillis = snapshot.timerEndEpochMillis - nowMillis
        return ceil(remainingMillis / 1_000.0).toInt()
    }

    private fun Int.sanitizeRestSeconds(): Int = coerceIn(MIN_REST_SECONDS, MAX_REST_SECONDS)

    private fun Set<String>.asSanitizedPackageSet(): Set<String> {
        return asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
