package com.cloudlevi.ping

import java.util.*

const val STRIPE_BACKEND_URL = "https://nazar-ping.herokuapp.com/"
const val STRIPE_KEY_TEST =
    "pk_test_51Jz6KABd8FsnZNBLqye5OcsppUUn0uLZgaGg3OKtDxpcMseKYgb8U3bj3EIuHpEgRv30Hgp7NQLPvpPSe0Zxc6vB00bU4DIMt3"
const val EXCHANGE_URL = "https://currency-converter5.p.rapidapi.com/currency/"
const val EXCHANGE_HOST = "currency-converter5.p.rapidapi.com"
const val EXCHANGE_KEY = "bd0abe5c33msh4bba22c94f6b8b9p1b0eb9jsnafb066050e96"
const val SHARED_PREFERENCES_KEY = "prefs"


val currencySymbols = mapOf(
    "usd" to "$",
    "uah" to "₴",
    "pln" to "zł",
    "eur" to "€",
    "gbp" to "£",
    "jpy" to "¥",
    "rub" to "₽"
)

val languageSymbols = mapOf(
    "english" to "en",
    "polish" to "pl",
    "russian" to "ru",
)

fun String.toLanguageSymbol() = languageSymbols[this.lowercase()] ?: "en"

fun String.fromLanguageSymbol(): String {
    var result = "English"
    for (a in languageSymbols) {
        if (a.value == this) result = a.key.replaceFirstChar { it.uppercase() }
    }
    return result
}

fun String.toCurrencySymbol(): String {
    val thisKey = this.lowercase()
    return if (currencySymbols.containsKey(thisKey)) currencySymbols[thisKey] ?: this
    else this
}

fun String.fromCurrencySymbol(): String {
    return if (currencySymbols.keys.contains(this.lowercase())) this.uppercase()
    else {
        var result = this.uppercase()
        for (a in currencySymbols) {
            if (a.value == this) result = a.key
        }
        return result.uppercase()
    }
}