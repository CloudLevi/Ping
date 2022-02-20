package com.cloudlevi.ping.ui.bookingFragment

import android.content.ContentValues
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.*
import com.cloudlevi.ping.ext.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.cloudlevi.ping.ui.bookingFragment.BookingViewModel.ActionType.*
import com.cloudlevi.ping.ui.userChat.MessageMediaAdapter
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.HashMap
import kotlin.math.ceil

@HiltViewModel
class BookingViewModel @Inject constructor(
    val preferencesManager: PreferencesManager
) : ViewModel(), MediaAdapterVM {

    private val httpClient = OkHttpClient()

    val doAction = ActionLiveData<Action>()

    var imagesAdapter = MessageMediaAdapter(this)

    private val usersRef = FirebaseDatabase.getInstance().getReference("users")
    private val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
    private val storageRef = FirebaseStorage.getInstance()
    private var userID = ""

    var checkInDateLong = 0L
    var checkOutDateLong = 0L

    var checkInHour = 14
    var checkInMinute = 0

    var paymentType = PaymentType.NONE

    var specialWishesText = ""

    var aptPrice = 0.0
        set(value) {
            field = value
            total = (value + pingFee).roundTo(2)
        }
    var aptPriceLocalized = 0.0
        set(value) {
            field = value
            totalLocalized = (value + pingFeeLocalized).roundTo(2)
        }

    var pingFee = 0.0
        set(value) {
            field = value
            total = (value + aptPrice).roundTo(2)
        }
    var pingFeeLocalized = 0.0
        set(value) {
            field = value
            totalLocalized = (value + aptPriceLocalized).roundTo(2)
        }

    var total = 0.0
    var totalLocalized = 0.0

    var currentHomePost: ApartmentHomePost? = null
    var landLord: User? = null

    fun areDatesSelected() = checkInDateLong != 0L && checkOutDateLong != 0L

    fun isPaymentTypeSelected() = paymentType != PaymentType.NONE

    fun getCurrentHomePostImages(): List<StorageReference> {
        return currentHomePost?.imagesList
            ?.map { storageRef.getReferenceFromUrl(it) }
            ?: listOf()
    }

    init {
        viewModelScope.launch {
            userID = preferencesManager.getUserID()
        }
    }

    fun fragmentCreated(apartmentHomePost: ApartmentHomePost, mLandLord: User) {
        currentHomePost = apartmentHomePost
        landLord = mLandLord
        imagesAdapter.applyListOnly(getCurrentHomePostImages())
        imagesAdapter.notifyItemRangeInserted(0, currentHomePost?.imagesList?.size ?: 0)
    }

    fun clearFields() {
        checkInDateLong = 0L
        checkOutDateLong = 0L

        checkInHour = 14
        checkInMinute = 0

        paymentType = PaymentType.NONE

        specialWishesText = ""
        aptPrice = 0.0
        pingFee = 0.0
        total = 0.0
    }

    fun calculatePricing() {
        val rate = currentHomePost?.price?.toDouble() ?: return
        val rateLocalized = currentHomePost?.mGetCalculationPrice() ?: return
        val rateDailyLocalized: Double
        val priceType = currentHomePost?.priceType ?: return
        val rateDaily = when (priceType) {
            PRICE_TYPE_PER_DAY -> {
                rateDailyLocalized = rateLocalized
                rate
            }
            PRICE_TYPE_PER_WEEK -> {
                rateDailyLocalized = rateLocalized / 7.0
                rate / 7.0
            }
            PRICE_TYPE_PER_MONTH -> {
                rateDailyLocalized = rateLocalized / 30.5
                rate / 30.5
            }
            else -> {
                rateDailyLocalized = -1.0
                -1.0
            }
        }

        val checkInDateLong = checkInDateLong.toJodaTime()
            .withHourOfDay(checkInHour)
            .withMinuteOfHour(checkInMinute)
            .getTime()

        val checkOutDateLong = checkOutDateLong.toJodaTime()
            .withHourOfDay(checkInHour)
            .withMinuteOfHour(checkInMinute)
            .withSecondOfMinute(59)
            .getTime()

        val differenceMillis = checkOutDateLong - checkInDateLong
        var days = differenceMillis / daysInMilli
        if (days == 0L) days = 1L
        val hours = (differenceMillis % daysInMilli) / hoursInMilli
        if (hours >= 3L) days += 1L

        aptPrice = (days * rateDaily).roundTo(2)
        aptPriceLocalized = (days * rateDailyLocalized).roundTo(2)
        pingFee = if (paymentType == PaymentType.CARD) (aptPrice * 0.02).roundTo(2)
        else 0.0
        pingFeeLocalized =
            if (paymentType == PaymentType.CARD) (aptPriceLocalized * 0.02).roundTo(2)
            else 0.0
    }

    fun startPayment(cardParams: PaymentMethodCreateParams) {
        val mediaType: MediaType = "application/json; charset=utf-8".toMediaType()
        val payMap: MutableMap<String, Any> =
            hashMapOf(
                "currency" to "usd",
                "amount" to ceil(total * 100).toInt()
            )

        val json = Gson().toJson(payMap)

        val request: Request = Request.Builder()
            .url(STRIPE_BACKEND_URL + "create-payment-intent")
            .post(json.toRequestBody(mediaType))
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                doAction.post(Action(TOAST, msg = "Payment request not successful. Please try again"))
                doAction.post(Action(TOGGLE_PROGRESS, bool = false))
                Log.d(ContentValues.TAG, "onFailure: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful)
                    doAction.post(Action(TOAST, msg = "Payment request not successful. Please try again"))
                else {
                    secretReceived(response, cardParams)
                }
            }
        })
    }

    private fun secretReceived(response: Response, cardParams: PaymentMethodCreateParams) {
        val responseString = response.body?.string() ?: ""
        val map = Gson().fromJson(responseString, HashMap::class.java) ?: mapOf()

        val clientSecret: String = map["clientSecret"] as? String ?: ""

        val confirmParams = ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
            cardParams,
            clientSecret
        )
        doAction.post(Action(STRIPE_CONFIRM, confirmParams = confirmParams))
    }

    fun saveBookingRemotely(isPaid: Boolean = false) {
        val aPost = currentHomePost ?: return
        val bookingID = bookingsRef.push().key ?: return
        val bookingModel = BookingModel(
            bookingID = bookingID,
            apartmentID = aPost.apartmentPostID,
            landlordID = aPost.landLordID,
            landLordDisplayName = landLord?.displayName,
            landLordUserName = landLord?.username,
            tenantID = userID,
            checkInDate = checkInDateLong,
            checkInTime = hoursMinutesToMillis(checkInHour, checkInMinute),
            checkOutDate = checkOutDateLong,
            extraInfo = specialWishesText.trim(),
            paymentStatus = if (isPaid) BookingStatus.PAID.ordinal
            else BookingStatus.BOOKED.ordinal,
            paymentType = paymentType.ordinal,
            rentTotal = total
        )

        bookingsRef.child(bookingID).setValue(bookingModel).addOnSuccessListener { task: Void? ->
            doAction.set(Action(BOOKING_CREATED))
        }
    }

    override fun getCurrentListByID(id: String?): MutableList<StorageReference> {
        return getCurrentHomePostImages().toMutableList()
    }

    override fun getCurrentListByPos(pos: Int): MutableList<StorageReference> {
        return getCurrentHomePostImages().toMutableList()
    }

    override fun onMediaImageClick(id: String?, position: Int?) {
        doAction.set(Action(OPEN_IMAGE_AT, pos = position))
    }

    enum class ActionType {
        OPEN_IMAGE_AT,
        TOAST,
        STRIPE_CONFIRM,
        TOGGLE_PROGRESS,
        BOOKING_CREATED
    }

    data class Action(
        val actionType: ActionType,
        val pos: Int? = null,
        val msg: String? = null,
        val confirmParams: ConfirmPaymentIntentParams? = null,
        val bool: Boolean? = null
    )
}