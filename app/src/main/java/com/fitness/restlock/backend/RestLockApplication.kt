package com.fitness.restlock.backend

import android.app.Application

open class RestLockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RestLockBackend.initialize(this)
    }
}
