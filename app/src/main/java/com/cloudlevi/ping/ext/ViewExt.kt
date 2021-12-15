package com.cloudlevi.ping.ext

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

fun TextView.applyText(appliedText: String?) {
    if (appliedText.isNullOrEmpty()){
        makeGone()
        return
    }
    text = appliedText
    makeVisible()
}

fun View.makeGone() {
    this.visibility = View.GONE
}

fun View.makeVisible() {
    this.visibility = View.VISIBLE
}

fun View.visibleOrGone(isVisible: Boolean) {
    if (isVisible) this.makeVisible()
    else this.makeGone()
}


fun toggleAllViewsEnabled(isEnabled: Boolean, view: View){
    if (view is ViewGroup){
        view.forEach {
            toggleAllViewsEnabled(isEnabled, it)
        }
    }
    else view.isEnabled = isEnabled
}

fun RecyclerView.attachSnapHelper() {
    this.onFlingListener = null
    PagerSnapHelper().attachToRecyclerView(this)
}

fun RecyclerView.removeAnimations() {
    (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
}