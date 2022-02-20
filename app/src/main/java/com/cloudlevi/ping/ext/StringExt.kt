package com.cloudlevi.ping.ext

import android.util.Patterns

fun Any.applyCurrencySymbol(symbol: String): String {
    return when (symbol.length) {
        1 -> "$symbol$this"
        else -> "$this $symbol"
    }
}

fun String.trimDigits(): String {
    val onlyDigitsAndDots = this.filter { it.isDigit() || it == '.' }

    return if (onlyDigitsAndDots.count { it == '.' } > 1) {
        val before = onlyDigitsAndDots.substringBefore('.')
        val after = onlyDigitsAndDots.substringAfter('.').filter { it != '.' }
        "$before.$after"
    } else onlyDigitsAndDots
}

fun String.trimDigitsInt(): String =this.filter { it.isDigit() }

fun String.isValidEmail(): Boolean = this.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()