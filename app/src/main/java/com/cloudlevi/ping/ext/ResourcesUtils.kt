package com.cloudlevi.ping.ext

import android.content.Context
import com.cloudlevi.ping.R
import com.cloudlevi.ping.fromCurrencySymbol
import com.cloudlevi.ping.fromLanguageSymbol

fun getPosForCurrency(context: Context, currency: String): Int {
    val currencyModified = currency.fromCurrencySymbol()
    val array = context.resources.getStringArray(R.array.array_currency_codes)
    return array.indexOfFirst { it == currencyModified }
}

fun getPosForLanguage(context: Context, languageCode: String): Int {
    val lang = languageCode.fromLanguageSymbol()
    val array = context.resources.getStringArray(R.array.language_array)
    return array.indexOfFirst { it == lang }
}