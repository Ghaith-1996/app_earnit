package com.fitness.restlock.backend.permissions

import android.content.Intent
import com.fitness.restlock.backend.PermissionStatus

interface PermissionGateway {
    fun currentStatus(): PermissionStatus
    fun accessibilitySettingsIntent(): Intent
    fun appDetailsSettingsIntent(): Intent
}
