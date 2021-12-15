package com.cloudlevi.ping.data

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    var userID: String? = "",
    var email: String? = "",
    var displayName: String? = "",
    var username: String? = "",
    var imageUrl: String? = "",
    var userOnline: Boolean? = false
):Parcelable{
    @IgnoredOnParcel
    var rateList: ArrayList<RatedPost> = arrayListOf()
}