package com.cloudlevi.ping

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PingApplication: Application() {

    companion object {
        lateinit var instance: PingApplication
    }

    var connectivityManager: ConnectivityManager? = null

    override fun onCreate() {
        super.onCreate()

        instance = this

        connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
}