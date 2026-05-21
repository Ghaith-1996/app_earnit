package com.fitness.restlock.backend.alarm

interface RestAlarmScheduler {
    fun scheduleRestEnd(triggerAtMillis: Long)
    fun cancelRestEnd()
}
