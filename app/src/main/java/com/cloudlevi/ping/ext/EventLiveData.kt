package com.cloudlevi.ping.ext

import androidx.lifecycle.MutableLiveData

class ActionLiveData<T> : MutableLiveData<Event<T>>() {

    fun set(t: T) {
        value = Event(t)
    }

    fun post(t: T) {
        postValue(Event(t))
    }
}

open class Event<out T>(private val data: T) {

    var hasBeenHandled = false
        private set

    fun getDataSafely(): T? {
        return if (hasBeenHandled) null
        else {
            hasBeenHandled = true
            data
        }
    }

    fun peekData(): T = data
}