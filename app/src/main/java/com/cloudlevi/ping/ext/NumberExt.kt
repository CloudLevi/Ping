package com.cloudlevi.ping.ext

import java.math.BigDecimal
import java.math.RoundingMode

fun Number.toMinuteString() = if (this.toString().length == 2) this.toString()
else "0$this"

fun Double.roundTo(places: Int) = BigDecimal(this)
        .setScale(places, RoundingMode.HALF_EVEN)
        .toDouble()