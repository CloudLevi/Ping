package com.cloudlevi.ping.data

import android.content.Context
import android.net.Uri
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.cloudlevi.ping.R
import com.cloudlevi.ping.ext.applyCurrencySymbol
import com.cloudlevi.ping.ext.roundTo
import com.cloudlevi.ping.ui.bookingFragment.BookingViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Exclude
import java.text.DecimalFormat

data class BookingModel(
    val bookingID: String? = null,
    val apartmentID: String? = null,
    val landlordID: String? = "",
    val landLordDisplayName: String? = "",
    val landLordUserName: String? = "",
    val landLordImageURL: String? = "",
    val tenantID: String? = "",
    val checkInDate: Long? = 0L,
    val checkInTime: Long? = 0L,
    val checkOutDate: Long? = 0L,
    val extraInfo: String? = "",
    val paymentStatus: Int? = 0,
    val paymentType: Int? = 0,
    val rentTotal: Double? = 0.0,
    var rentTotalLocalized: Double = 0.0,
    var rentCurrency: String = ""
) {

    companion object {
        fun createFromSnapshot(
            snapshot: DataSnapshot,
            exRate: Double? = null,
            currency: String? = null
        ): BookingModel? {
            val bm = snapshot.getValue(BookingModel::class.java) ?: return null

            if (exRate != null && currency != null) {
                bm.rentTotalLocalized = ((bm.rentTotal ?: 0.0) * exRate).roundTo(2)
                bm.rentCurrency = currency
            }
            return bm
        }
    }

    fun getPaymentStatusEnum() = BookingStatus.values().first {
        it.ordinal == paymentStatus
    }

    fun getPricingText(): String {
        val rentTotal = "$${rentTotal}"
        val rentLocalizedString = DecimalFormat("#.#")
            .format(rentTotalLocalized)
            .applyCurrencySymbol(rentCurrency)
        if (rentTotalLocalized >= 0.0 && rentCurrency != "$")
            return rentTotal.plus(" ≈ ").plus(rentLocalizedString)

        return rentTotal
    }

    fun parsePaymentStatusText(context: Context): SpannableString {
        val enum = BookingStatus.values()
            .first { it.ordinal == paymentStatus }

        var colorSpan = R.color.text_color

        val resID = when (enum) {
            BookingStatus.BOOKED -> R.string.booked
            BookingStatus.PAID -> R.string.paid
            BookingStatus.IN_PROGRESS -> {
                colorSpan = R.color.orange
                R.string.in_progress
            }
            else -> {
                colorSpan = R.color.red_error
                R.string.finished
            }
        }

        val sString = SpannableString(context.getString(resID))
        sString.setSpan(
            ForegroundColorSpan(context.getColor(colorSpan)),
            0,
            sString.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return sString
    }

    @Exclude
    @set:Exclude
    @get:Exclude
    var aTitle: String? = ""

    @Exclude
    @set:Exclude
    @get:Exclude
    var aLocation: String? = ""

    @Exclude
    @set:Exclude
    @get:Exclude
    var aCity: String? = ""

    @Exclude
    @set:Exclude
    @get:Exclude
    var aCountry: String? = ""

    @Exclude
    @set:Exclude
    @get:Exclude
    var aAcreage: Double? = 0.0

    @Exclude
    @set:Exclude
    @get:Exclude
    var aRoomCount: Int? = 0

    @Exclude
    @set:Exclude
    @get:Exclude
    var aFurniture: Boolean = false

    @Exclude
    @set:Exclude
    @get:Exclude
    var aRating: Float? = 0F

    @Exclude
    @set:Exclude
    @get:Exclude
    var aImagesList: MutableList<Uri>? = mutableListOf(Uri.EMPTY)

    fun getCheckInLong(): Long = (checkInDate ?: 0L) + (checkInTime ?: 0L)

    fun roomCountString(context: Context): String {
        val resID = if (aRoomCount == 1) R.string.one_room
        else R.string.rooms
        return context.getString(resID, aRoomCount.toString())
    }

    fun isPaymentCreditCard() = paymentType == PaymentType.CARD.ordinal

    fun isPaymentCash() = paymentType == PaymentType.CASH.ordinal
}