package com.cloudlevi.ping.ext

import kotlin.random.Random

fun sizeTheSame(firstList: Collection<Any>, secondList: Collection<Any>): Boolean =
    firstList.size == secondList.size

fun <T> Iterable<T>.getMax(default: Float = 0f, selector: (T) -> Float): Float {
    val iterator = iterator()
    if (!iterator.hasNext()) return default
    var maxValue = selector(iterator.next())
    while (iterator.hasNext()) {
        val v = selector(iterator.next())
        maxValue = maxOf(maxValue, v)
    }
    return maxValue
}