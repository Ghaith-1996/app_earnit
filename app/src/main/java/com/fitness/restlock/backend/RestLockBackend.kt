package com.fitness.restlock.backend

import android.content.Context
import com.fitness.restlock.backend.alarm.AndroidRestAlarmScheduler
import com.fitness.restlock.backend.apps.AndroidInstalledAppRepository
import com.fitness.restlock.backend.apps.InstalledAppRepository
import com.fitness.restlock.backend.blocking.AndroidAppBlockPolicy
import com.fitness.restlock.backend.permissions.AndroidPermissionGateway
import com.fitness.restlock.backend.permissions.PermissionGateway
import com.fitness.restlock.backend.persistence.WorkoutPreferencesStore
import com.fitness.restlock.backend.session.DefaultWorkoutController
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope

object RestLockBackend {
    @Volatile
    private var controller: DefaultWorkoutController? = null

    @Volatile
    private var installedApps: InstalledAppRepository? = null

    @Volatile
    private var permissions: PermissionGateway? = null

    fun initialize(context: Context) {
        controller(context)
    }

    fun controller(context: Context): WorkoutController = defaultController(context)

    fun installedApps(context: Context): InstalledAppRepository {
        val appContext = context.applicationContext
        return installedApps ?: synchronized(this) {
            installedApps ?: AndroidInstalledAppRepository(
                context = appContext,
                blockPolicy = AndroidAppBlockPolicy(appContext),
            ).also { installedApps = it }
        }
    }

    fun permissions(context: Context): PermissionGateway {
        val appContext = context.applicationContext
        return permissions ?: synchronized(this) {
            permissions ?: AndroidPermissionGateway(appContext).also { permissions = it }
        }
    }

    internal fun handleRestTimerAlarm(context: Context): Job {
        return defaultController(context).handleRestTimerAlarm()
    }

    private fun defaultController(context: Context): DefaultWorkoutController {
        val appContext = context.applicationContext
        return controller ?: synchronized(this) {
            controller ?: DefaultWorkoutController(
                store = WorkoutPreferencesStore(appContext),
                alarmScheduler = AndroidRestAlarmScheduler(appContext),
                permissionGateway = permissions(appContext),
                blockPolicy = AndroidAppBlockPolicy(appContext),
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            ).also { controller = it }
        }
    }
}
