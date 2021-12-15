package com.cloudlevi.ping.data

import android.content.Context
import androidx.annotation.StringRes
import com.cloudlevi.ping.R

enum class RentalMode {
    TENANT_MODE,
    LANDLORD_MODE
}

enum class BookingStatus {
    PAID,
    BOOKED,
    IN_PROGRESS,
    FINISHED
}

enum class PaymentType {
    NONE,
    CARD,
    CASH
}

enum class SortBy {
    NONE,
    PRICE,
    TIME,
    NAME,
    RATING,
    ACREAGE,
    ROOM_AMOUNT
}

enum class SortOrder {
    NONE,
    ASCENDING,
    DESCENDING
}