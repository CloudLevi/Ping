package com.cloudlevi.ping.ext

import android.content.Context
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.SortBy

fun getStringForEnum(context: Context, sortBy: SortBy): String {
    val resID = when(sortBy){
        SortBy.PRICE -> R.string.price_per_day
        SortBy.TIME -> R.string.time_posted
        SortBy.NAME -> R.string.name
        SortBy.RATING -> R.string.rating
        SortBy.ACREAGE -> R.string.acreage
        SortBy.ROOM_AMOUNT -> R.string.room_amount
        else -> R.string.default_string
    }
    return context.getString(resID)
}