package com.fitness.restlock.backend.blocking

import com.fitness.restlock.backend.WorkoutMode
import com.fitness.restlock.backend.WorkoutState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BlockerLaunchCoordinatorTest {
    @Test
    fun idleAndRestingNeverRequestHomeOrBlocker() = runTest {
        val fixture = fixture()
        listOf(WorkoutMode.Idle, WorkoutMode.Resting).forEach { mode ->
            fixture.state = WorkoutState(mode = mode)
            fixture.coordinator.onForegroundPackageChanged(BLOCKED)
            advanceTimeBy(2_000)
            fixture.coordinator.onStateChanged()
            assertEquals(0, fixture.homeCount)
            assertTrue(fixture.launches.isEmpty())
        }
    }

    @Test
    fun allowedExemptAndMissingPackagesNeverRequestActions() = runTest {
        val fixture = fixture()
        fixture.state = fixture.state.copy(allowedApps = setOf("com.music"))
        listOf("com.music", HOST, HOME, "com.android.settings", "com.android.phone", null, "", " ").forEach {
            fixture.coordinator.onForegroundPackageChanged(it)
            advanceTimeBy(2_000)
        }
        assertEquals(0, fixture.homeCount)
        assertTrue(fixture.launches.isEmpty())
    }

    @Test
    fun blockedAppGoesHomeThenOpensOneBlockerWithoutLauncherLoop() = runTest {
        val fixture = fixture()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        assertEquals(1, fixture.homeCount)
        assertTrue(fixture.launches.isEmpty())
        fixture.coordinator.onForegroundPackageChanged(HOME)
        advanceTimeBy(119)
        assertTrue(fixture.launches.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        fixture.coordinator.onForegroundPackageChanged(HOST)
        advanceTimeBy(2_000)
        assertEquals(1, fixture.homeCount)
        assertEquals(listOf(BLOCKED), fixture.launches)
    }

    @Test
    fun rapidEventsAcrossPackagesCoalesceAndLaterEventsStillWork() = runTest {
        val fixture = fixture()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        repeat(10) {
            advanceTimeBy(50)
            fixture.coordinator.onForegroundPackageChanged(if (it % 2 == 0) BLOCKED else "com.other")
        }
        assertEquals(1, fixture.homeCount)
        assertEquals(listOf(BLOCKED), fixture.launches)
        advanceTimeBy(500)
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        advanceTimeBy(120)
        runCurrent()
        assertEquals(2, fixture.homeCount)
        assertEquals(listOf(BLOCKED, BLOCKED), fixture.launches)
    }

    @Test
    fun cooldownRechecksLatestBlockedAppWithoutNeedingAnotherEvent() = runTest {
        val fixture = fixture()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        advanceTimeBy(500)
        fixture.coordinator.onForegroundPackageChanged("com.other")
        advanceTimeBy(620)
        runCurrent()
        assertEquals(listOf(BLOCKED, "com.other"), fixture.launches)
    }

    @Test
    fun cooldownRetryDoesNotInterruptAllowedAppsOrSurviveEndOfLock() = runTest {
        val fixture = fixture()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        advanceTimeBy(500)
        fixture.coordinator.onForegroundPackageChanged("com.other")
        fixture.coordinator.onForegroundPackageChanged(HOME)
        advanceTimeBy(1_000)
        assertEquals(listOf(BLOCKED), fixture.launches)

        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        advanceTimeBy(500)
        fixture.coordinator.onForegroundPackageChanged("com.other")
        fixture.state = fixture.state.copy(mode = WorkoutMode.Resting)
        fixture.coordinator.onStateChanged()
        advanceTimeBy(1_000)
        assertEquals(listOf(BLOCKED, BLOCKED), fixture.launches)
    }

    @Test
    fun leavingLockCancelsPendingLaunchAndReleasesThrottleForNextLock() = runTest {
        val fixture = fixture()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        runCurrent()
        fixture.state = fixture.state.copy(mode = WorkoutMode.Resting)
        fixture.coordinator.onStateChanged()
        advanceTimeBy(200)
        assertTrue(fixture.launches.isEmpty())
        fixture.state = fixture.state.copy(mode = WorkoutMode.AwaitingDecision)
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        advanceTimeBy(120)
        runCurrent()
        assertEquals(listOf(BLOCKED), fixture.launches)
    }

    @Test
    fun delayedLaunchRechecksLatestModeEvenBeforeStateObserverRuns() = runTest {
        listOf(WorkoutMode.Idle, WorkoutMode.Resting).forEach { mode ->
            val fixture = fixture()
            fixture.coordinator.onForegroundPackageChanged(BLOCKED)
            fixture.state = fixture.state.copy(mode = mode)
            advanceTimeBy(120)
            runCurrent()
            assertTrue("No stale launch in $mode", fixture.launches.isEmpty())
        }
    }

    @Test
    fun delayedLaunchRechecksAllowlistEvenBeforeStateObserverRuns() = runTest {
        val fixture = fixture()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        fixture.state = fixture.state.copy(allowedApps = setOf(BLOCKED))
        advanceTimeBy(120)
        runCurrent()
        assertTrue(fixture.launches.isEmpty())
    }

    @Test
    fun allowingPendingAppCancelsItsLaunch() = runTest {
        val fixture = fixture()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        fixture.state = fixture.state.copy(allowedApps = setOf(BLOCKED))
        fixture.coordinator.onStateChanged()
        advanceTimeBy(120)
        runCurrent()
        assertTrue(fixture.launches.isEmpty())
    }

    @Test
    fun timerExpiryChecksAppAlreadyOpenDuringRest() = runTest {
        val fixture = fixture()
        fixture.state = fixture.state.copy(mode = WorkoutMode.Resting)
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        assertEquals(0, fixture.homeCount)
        fixture.state = fixture.state.copy(mode = WorkoutMode.AwaitingDecision)
        fixture.coordinator.onStateChanged()
        advanceTimeBy(120)
        runCurrent()
        assertEquals(listOf(BLOCKED), fixture.launches)
    }

    @Test
    fun idleDoesNotRememberPackagesForLaterWorkout() = runTest {
        val fixture = fixture()
        fixture.state = fixture.state.copy(mode = WorkoutMode.Idle)
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        fixture.state = fixture.state.copy(mode = WorkoutMode.AwaitingDecision)
        fixture.coordinator.onStateChanged()
        advanceTimeBy(120)
        assertEquals(0, fixture.homeCount)
    }

    @Test
    fun resetCancelsPendingWorkAndReconnectCanBlockAgain() = runTest {
        val fixture = fixture()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        runCurrent()
        fixture.coordinator.reset()
        fixture.coordinator.onStateChanged()
        advanceTimeBy(120)
        runCurrent()
        assertTrue(fixture.launches.isEmpty())
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        advanceTimeBy(120)
        runCurrent()
        assertEquals(listOf(BLOCKED), fixture.launches)
    }

    @Test
    fun cancelledJobsCannotClearNewPendingLaunch() = runTest {
        val fixture = fixture()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        runCurrent()
        fixture.coordinator.reset()
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        runCurrent()
        fixture.coordinator.onForegroundPackageChanged("com.other")
        advanceTimeBy(120)
        runCurrent()
        assertEquals(listOf(BLOCKED), fixture.launches)
        assertEquals(2, fixture.homeCount)
    }

    @Test
    fun failedActivityLaunchCanRetryOnNextEvent() = runTest {
        val fixture = fixture()
        fixture.launchSucceeds = false
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        advanceTimeBy(120)
        runCurrent()
        fixture.launchSucceeds = true
        fixture.coordinator.onForegroundPackageChanged(BLOCKED)
        advanceTimeBy(120)
        runCurrent()
        assertEquals(listOf(BLOCKED, BLOCKED), fixture.launches)
    }

    private fun TestScope.fixture(): Fixture = Fixture(this)

    private class Fixture(scope: TestScope) {
        var state = WorkoutState(mode = WorkoutMode.AwaitingDecision)
        var homeCount = 0
        var launchSucceeds = true
        val launches = mutableListOf<String>()
        val coordinator = BlockerLaunchCoordinator(
            policy = StaticAppBlockPolicy(setOf(HOST, HOME) + KnownExemptPackages.coreSystemPackages),
            scope = scope,
            currentState = { state },
            elapsedRealtime = { scope.testScheduler.currentTime },
            goHome = { homeCount++ },
            openBlocker = { launches += it; launchSucceeds },
        )
    }

    private companion object {
        const val BLOCKED = "com.social.app"
        const val HOST = "com.fitness.restlock"
        const val HOME = "com.launcher"
    }
}
