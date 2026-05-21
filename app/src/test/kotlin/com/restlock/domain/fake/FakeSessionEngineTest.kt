package com.restlock.domain.fake

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.restlock.domain.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Pinning the session state machine contract so the backend agent has a clear,
 * executable spec to swap in their real implementation against.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FakeSessionEngineTest {

    @Test
    fun `starts in Idle`() = runTest {
        val engine = newEngine()
        assertThat(engine.state.value.phase).isEqualTo(SessionState.Phase.Idle)
        assertThat(engine.state.value.remaining).isNull()
    }

    @Test
    fun `startWorkout transitions to Resting with chosen rest as remaining`() = runTest {
        val engine = newEngine()
        engine.startWorkout(90.seconds)

        val s = engine.state.value
        assertThat(s.phase).isEqualTo(SessionState.Phase.Resting)
        assertThat(s.chosenRest).isEqualTo(90.seconds)
        assertThat(s.remaining).isEqualTo(90.seconds)
        assertThat(s.blockerArmed).isFalse()
    }

    @Test
    fun `rest expiry transitions to AwaitingDecision with blockerArmed`() = runTest {
        val engine = newEngine()
        engine.startWorkout(1.seconds)

        // Drive the countdown past expiry.
        advanceTimeBy(1_500.milliseconds)

        val s = engine.state.value
        assertThat(s.phase).isEqualTo(SessionState.Phase.AwaitingDecision)
        assertThat(s.blockerArmed).isTrue()
    }

    @Test
    fun `exerciseDone counts a set and restarts chosen rest`() = runTest {
        val engine = newEngine()
        engine.startWorkout(1.seconds)
        advanceTimeBy(1_500.milliseconds) // expire to AwaitingDecision

        engine.exerciseDone()

        val s = engine.state.value
        assertThat(s.phase).isEqualTo(SessionState.Phase.Resting)
        assertThat(s.setsCompleted).isEqualTo(1)
        assertThat(s.remaining).isEqualTo(1.seconds)
        assertThat(s.blockerArmed).isFalse()
    }

    @Test
    fun `addThirtySeconds increments extras and starts 30s timer`() = runTest {
        val engine = newEngine()
        engine.startWorkout(1.seconds)
        advanceTimeBy(1_500.milliseconds)

        engine.addThirtySeconds()

        val s = engine.state.value
        assertThat(s.phase).isEqualTo(SessionState.Phase.Resting)
        assertThat(s.extraRests).isEqualTo(1)
        assertThat(s.remaining).isEqualTo(30.seconds)
        assertThat(s.blockerArmed).isFalse()
    }

    @Test
    fun `finishWorkout returns to Idle and clears counters`() = runTest {
        val engine = newEngine()
        engine.startWorkout(1.seconds)
        advanceTimeBy(1_500.milliseconds)
        engine.exerciseDone()
        advanceTimeBy(1_500.milliseconds)

        engine.finishWorkout()

        val s = engine.state.value
        assertThat(s.phase).isEqualTo(SessionState.Phase.Idle)
        assertThat(s.setsCompleted).isEqualTo(0)
        assertThat(s.extraRests).isEqualTo(0)
        assertThat(s.blockerArmed).isFalse()
    }

    @Test
    fun `state stream emits the resting transition synchronously after start`() = runTest {
        val engine = newEngine()
        engine.state.test {
            assertThat(awaitItem().phase).isEqualTo(SessionState.Phase.Idle)
            engine.startWorkout(60.seconds)
            assertThat(awaitItem().phase).isEqualTo(SessionState.Phase.Resting)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun TestScope.newEngine(): FakeSessionEngine {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = kotlinx.coroutines.CoroutineScope(dispatcher)
        return FakeSessionEngine(
            scope = scope,
            settingsRepository = FakeSettingsRepository(),
        )
    }
}
