package com.cloudlevi.ping.data
import com.google.gson.annotations.SerializedName

data class ExchangeModel(
    @SerializedName("base_currency_code")
    val currencyCode: String,
    @SerializedName("base_currency_name")
    val currencyName: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("updated_date")
    val updated_date: String,
    @SerializedName("rates")
    val rates: HashMap<String, Currency>,
    @SerializedName("status")
    val status: String
)

data class Rate(
    val currency: Currency
)

data class Currency(
    @SerializedName("currency_name")
    val currencyName: String,
    @SerializedName("rate")
    val rate: Double,
    @SerializedName("rate_for_amount")
    val rateForAmount: Double
)