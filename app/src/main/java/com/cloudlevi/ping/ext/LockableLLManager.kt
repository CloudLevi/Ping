package com.cloudlevi.ping.ext

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class LockableLLManager(context: Context, orientation: Int, reverseLayout: Boolean) :
    LinearLayoutManager(context, orientation, reverseLayout) {

    var scrollLocked = false

    override fun canScrollVertically(): Boolean {
        return !scrollLocked
    }
}