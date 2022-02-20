package com.cloudlevi.ping.ext

import android.os.Build
import com.cloudlevi.ping.PingApplication

fun hasInternet() =
    if (Build.VERSION.SDK_INT > 23)
        PingApplication.instance.connectivityManager?.activeNetwork != null
    else PingApplication.instance.connectivityManager?.activeNetworkInfo?.isConnected == true