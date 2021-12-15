package com.cloudlevi.ping.data

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.cloudlevi.ping.*
import com.cloudlevi.ping.ext.applyCurrencySymbol
import com.cloudlevi.ping.ext.roundTo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Exclude
import com.google.gson.Gson
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.text.DecimalFormat
import kotlin.math.round

@Parcelize
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
    var price: Int = 0,
    var priceType: Int? = PRICE_TYPE_PER_DAY,
    var firstImageReference: String = "",
    var imagesReference: String = "",
    var imageCount: Int = 0,
    var timeStamp: Long = 0,
    @Ignore
    @Exclude @set:Exclude @get:Exclude
    var imagesList: MutableList<Uri> = mutableListOf(),
    @Ignore
    @Exclude @set:Exclude @get:Exclude
    var priceLocalized: Double = -1.0,
    @Ignore
    @Exclude @set:Exclude @get:Exclude
    var currency: String = "$"
) : Parcelable {

    companion object {
        fun createFromSnapshot(
            snapshot: DataSnapshot,
            currency: String? = null,
            exRate: Double? = null
        ): ApartmentHomePost? {
            val aptHomePost = snapshot.getValue(ApartmentHomePost::class.java) ?: return null
            aptHomePost.ratingsList =
                snapshot.child("ratings").children.mapNotNull { ratingSnapshot ->
                    ratingSnapshot.getValue(RatingModel::class.java)
                }
            if (exRate != null && currency != null) {
                aptHomePost.priceLocalized = (aptHomePost.price * exRate).roundTo(2)
                aptHomePost.currency = currency
            }
            return aptHomePost
        }
    }

    @Ignore
    @IgnoredOnParcel
    var ratingsList: List<RatingModel> = listOf()

    fun calculateAverageRating() = if (ratingsList.isNullOrEmpty()) 0.0
    else ratingsList.map { it.rating }.average()

    fun reviewsCount() = ratingsList.size

    fun findReviewForID(userID: String): RatingModel? = ratingsList.find { it.userID == userID }

    fun getCalculationPrice() = if (priceLocalized >= 0.0) priceLocalized else price.toDouble()

    fun priceTypeString(context: Context): String {
        return when (priceType) {
            PRICE_TYPE_PER_DAY -> context.getString(R.string.day)
            PRICE_TYPE_PER_WEEK -> context.getString(R.string.week)
            PRICE_TYPE_PER_MONTH -> context.getString(R.string.month)
            else -> ""
        }
    }

    fun applyCurrency(mCurrency: String, exRate: Double) {
        priceLocalized = (price * exRate).roundTo(2)
        this.currency = mCurrency
    }

    fun getPricingText(): String =
        if (priceLocalized >= 0.0) DecimalFormat("#.#").format(priceLocalized)
            .applyCurrencySymbol(currency) else "$${price}"

    fun roomCountString(context: Context): String {
        val resID = if (roomAmount == 1) R.string.one_room
        else R.string.rooms
        return context.getString(resID, roomAmount.toString())
    }

    fun calculateFairPrice(): Double = when (priceType) {
        PRICE_TYPE_PER_DAY -> price.toDouble()
        PRICE_TYPE_PER_WEEK -> price / 7.0
        PRICE_TYPE_PER_MONTH -> price / 30.5
        else -> -1.0
    }

    fun matchesType(type: Int) = type == APT_TYPE_ALL || aptType == type

    fun matchesFurniture(type: Int): Boolean {
        if (type == APT_FURNISHED_ALL) return true
        val isFurnished = if (isFurnished) APT_FURNISHED_YES else APT_FURNISHED_NO
        return type == isFurnished
    }

    fun matchesRentType(acceptedTypes: List<Int>): Boolean {
        if (acceptedTypes.contains(PRICE_TYPE_ALL)) return true
        return acceptedTypes.contains(priceType)
    }

    fun matchesAverageRating(acceptedRange: ClosedFloatingPointRange<Float>) =
        calculateAverageRating() in acceptedRange

    fun matchesFloor(acceptedRange: ClosedFloatingPointRange<Float>) =
        aptFloor.toFloat() in acceptedRange

    fun matchesRooms(acceptedRange: ClosedFloatingPointRange<Float>) =
        roomAmount.toFloat() in acceptedRange

    fun matchesPrice(acceptedRange: ClosedFloatingPointRange<Float>) =
        getCalculationPrice().toFloat() in acceptedRange
}