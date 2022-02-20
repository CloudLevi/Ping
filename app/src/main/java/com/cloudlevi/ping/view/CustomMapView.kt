package com.cloudlevi.ping.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.google.android.gms.maps.MapView
import android.view.MotionEvent

class CustomMapView(context: Context, attrs: AttributeSet?) : MapView(context, attrs){

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_UP -> {
                this.parent.requestDisallowInterceptTouchEvent(false)
            }
            MotionEvent.ACTION_DOWN -> {
                this.parent.requestDisallowInterceptTouchEvent(true)
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}