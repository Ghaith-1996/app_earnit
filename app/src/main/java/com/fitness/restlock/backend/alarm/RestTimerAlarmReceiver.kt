package com.fitness.restlock.backend.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fitness.restlock.backend.RestLockBackend

class RestTimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_REST_TIMER_EXPIRED) return

        val pendingResult = goAsync()
        RestLockBackend.handleRestTimerAlarm(context)
            .invokeOnCompletion { pendingResult.finish() }
    }

    companion object {
        const val ACTION_REST_TIMER_EXPIRED =
            "com.fitness.restlock.backend.action.REST_TIMER_EXPIRED"
    }
}
