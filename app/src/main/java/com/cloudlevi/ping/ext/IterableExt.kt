package com.cloudlevi.ping.ext

import android.net.Uri

fun sizeTheSame(firstList: Collection<Any>, secondList: Collection<Any>): Boolean =
    firstList.size == secondList.size