package com.cloudlevi.ping.ui.yourBookings

import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.MediaAdapterVM
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.*
import com.cloudlevi.ping.ext.ActionLiveData
import com.cloudlevi.ping.ext.SimpleEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.cloudlevi.ping.ui.yourBookings.YourBookingsViewModel.ActionType.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.runBlocking

@HiltViewModel
class YourBookingsViewModel @Inject constructor(
    val dataStoreManager: PreferencesManager
) : ViewModel(), MediaAdapterVM {

    var userID = ""

    val doAction = ActionLiveData<Action>()

    var bookingsList = mutableListOf<BookingModel>()
    var adapter = YourBookingsAdapter(this)

    var currency = "USD"
    var exRate = 1.0

    private var currentMode = RentalMode.TENANT_MODE
        set(value) {
            field = value
            determineFragmentTitle()
        }

    private val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
    private val aptsRef = FirebaseDatabase.getInstance().getReference("apartments")
    private var storageRef = FirebaseStorage.getInstance().getReference("ApartmentUploads")

    fun fragmentCreated(rentalMode: RentalMode) {

        currentMode = rentalMode

        viewModelScope.launch {
            currency = dataStoreManager.getCurrency()
            exRate = dataStoreManager.getExRate()
            userID = dataStoreManager.getUserID()

            doAction.set(Action(TOGGLE_LOADING, bool = true))

            bookingsRef
                .orderByChild(getOrderChild())
                .equalTo(userID)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        doAction.set(Action(TOGGLE_LOADING, bool = false))

                        val currentTime = System.currentTimeMillis()

                        bookingsList = snapshot.children.map { childSnapshot ->
                            val bModel = BookingModel.createFromSnapshot(childSnapshot, exRate, currency) ?: return
                            updateBookingStatus(bModel)
                        }.sortedBy { it.getCheckInLong() - currentTime }.toMutableList()

                        val finished = bookingsList
                            .filter { it.getPaymentStatusEnum() == BookingStatus.FINISHED }

                        bookingsList.addAll(finished)

                        finished.forEach { finishedModel ->
                            bookingsList.remove(bookingsList.firstOrNull { finishedModel == it })
                        }

                        getApartmentDetails()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        doAction.set(Action(TOGGLE_LOADING, bool = false))
                    }
                })
        }
    }

    private fun getApartmentDetails() {
        var count = 0
        val totalCount = bookingsList.size

        bookingsList.forEachIndexed { index, booking ->
            aptsRef.child(booking.apartmentID ?: "")
                .addListenerForSingleValueEvent(object : SimpleEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)

                        val apt = ApartmentHomePost.createFromSnapshot(snapshot, currency, exRate)?: return

                        bookingsList[index].aImagesList = (0..apt.imageCount).map { Uri.EMPTY }.toMutableList()

                        bookingsList[index].apply {
                            aAcreage = apt.acreage
                            aCity = apt.city
                            aCountry = apt.country
                            aFurniture = apt.isFurnished
                            aLocation = apt.address
                            aRating = apt.calculateAverageRating().toFloat()
                            aRoomCount = apt.roomAmount
                            aTitle = apt.title
                        }
                        getImageLinks(apt, index)

                        count++
                        if (count == totalCount) adapter.update()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        super.onCancelled(error)
                        count++
                        if (count == totalCount) adapter.update()
                    }
                })
        }
    }

    fun determineFragmentTitle() = if (currentMode == RentalMode.TENANT_MODE)
        R.string.your_bookings
    else R.string.your_rentals

    private fun getOrderChild() = if (currentMode == RentalMode.TENANT_MODE) "tenantID"
    else "landlordID"

    private fun updateBookingStatus(bModel: BookingModel): BookingModel {
        val checkInDate = bModel.getCheckInLong()
        val checkOutDate = bModel.checkOutDate ?: 0L
        val currentTime = System.currentTimeMillis()

        val status = when {
            (currentTime < checkInDate) -> {
                if (bModel.isPaymentCreditCard()) BookingStatus.PAID
                else BookingStatus.BOOKED
            }
            (currentTime in checkInDate..checkOutDate) -> BookingStatus.IN_PROGRESS
            else -> BookingStatus.FINISHED
        }

        val updatedModel = bModel.copy(paymentStatus = status.ordinal)

//        bookingsRef.child(bModel.bookingID ?: "")
//            .setValue(updatedModel)
        return updatedModel
    }

    private fun getImageLinks(aModel: ApartmentHomePost, index: Int) {
        Log.d("DEBUG", "getImageLinks: ${aModel.apartmentPostID}, $index")
        (0..aModel.imageCount).forEach { pos ->
            storageRef.child(aModel.timeStamp.toString())
                .child(pos.toString()).downloadUrl.addOnSuccessListener { downloadURL ->
                    bookingsList[index].aImagesList!![pos] = downloadURL
                    doAction.set(Action(UPDATE_IMAGE, pos = index))
                }.addOnFailureListener {
                    Log.d("DEBUG", "getImageLinks FAILURE: ${aModel.apartmentPostID}, $index, msg: ${it.message}")
                }
        }
    }

    fun notifyRecyclerResize() {
        doAction.set(Action(NOTIFY_RECYCLER_RESIZE))
    }

    enum class ActionType {
        TOGGLE_LOADING,
        UPDATE_IMAGE,
        NOTIFY_RECYCLER_RESIZE,
        OPEN_IMAGE_AT,
        CHANGE_TITLE
    }

    data class Action(
        val actionType: ActionType,
        val bool: Boolean? = null,
        val listPos: Int? = null,
        val pos: Int? = null,
        @StringRes val resID: Int? = null
    )

    override fun getCurrentListByID(id: String?): MutableList<Uri> {
        return bookingsList.find { it.bookingID == id }?.aImagesList ?: mutableListOf()
    }

    override fun getCurrentListByPos(pos: Int): MutableList<Uri> {
        return bookingsList[pos].aImagesList ?: mutableListOf()
    }

    override fun onMediaImageClick(id: String?, position: Int?) {
        val listIndex = bookingsList.indexOfFirst { it.bookingID == id }
        doAction.set(Action(OPEN_IMAGE_AT, listPos = listIndex, pos = position))
    }
}