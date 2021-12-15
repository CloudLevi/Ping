package com.cloudlevi.ping

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PingApplication: Application() {

    companion object {
        lateinit var instance: Application
    }


    override fun onCreate() {
        super.onCreate()

        instance = this
    }
}