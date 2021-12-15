package com.cloudlevi.ping.ext

import android.content.Context

fun dpToPx(context: Context, dp: Int) =
    (dp * context.resources.displayMetrics.density + 0.5).toInt()