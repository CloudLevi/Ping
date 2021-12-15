package com.cloudlevi.ping.ext

fun Any.applyCurrencySymbol(symbol: String): String {
    return when (symbol.length) {
        1 -> "$symbol$this"
        else -> "$this $symbol"
    }
}