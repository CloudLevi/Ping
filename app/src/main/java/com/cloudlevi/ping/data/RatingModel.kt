package com.cloudlevi.ping.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RatingModel(
    val userID: String? = "",
    var displayName: String? = "",
    val apartmentID: String? = "",
    val rating: Double = 0.0,
    var comment: String? = "",
    var timeStamp: Long? = 0L
): Parcelable {
}