package com.cloudlevi.ping.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cloudlevi.ping.PRICE_TYPE_PER_DAY
import com.google.firebase.storage.StorageReference

@Entity(tableName = "apartments_homepage_table")
data class ApartmentHomePost constructor(
    @PrimaryKey
    var apartmentPostID: String = "",
    var landLordID: String = "",
    var title: String = "",
    var aptType: Int = 0,
    var aptFloor: Int = 0,
    var roomAmount: Int = 0,
    var isFurnished: Boolean = false,
    var acreage: Double = 0.0,
    var country: String = "",
    var city: String = "",
    var address: String = "",
    var description: String = "",
    var rating: Float = 0F,
    var ratingTotal: Float = 0F,
    var ratingQuantity: Int = 0,
    var price: Int = 0,
    var priceType: Int? = PRICE_TYPE_PER_DAY,
    var firstImageReference: String = "",
    var imagesReference: String = "",
    var imageCount: Int = 0,
    var timeStamp: Long = 0
) {

}