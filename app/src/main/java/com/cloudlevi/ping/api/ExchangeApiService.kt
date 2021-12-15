package com.cloudlevi.ping.api

import com.cloudlevi.ping.EXCHANGE_HOST
import com.cloudlevi.ping.EXCHANGE_KEY
import com.cloudlevi.ping.data.ExchangeModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query


interface ExchangeApiService {

    @GET("convert")
    fun getExchangeRate(
        @Query("from") fromCurrency: String,
        @Query("to") toCurrency: String,
        @Query("amount") quote: Double,
        @Query("format") format: String = "json",
        @Header("x-rapidapi-host") host: String = EXCHANGE_HOST,
        @Header("x-rapidapi-key") key: String = EXCHANGE_KEY
    ): Call<ExchangeModel>

}