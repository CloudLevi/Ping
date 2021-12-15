package com.cloudlevi.ping.ui.apartmentPage

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.data.*
import com.cloudlevi.ping.ext.ActionLiveData
import com.cloudlevi.ping.ext.SimpleEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.cloudlevi.ping.ui.apartmentPage.ApartmentPageEvent.*
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ApartmentPageViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val apartmentPageEventChannel = Channel<ApartmentPageEvent>()
    val apartmentPageEvent = apartmentPageEventChannel.receiveAsFlow()

    val action = ActionLiveData<Action>()

    private var apartmentsRef = Firebase.database.reference.child("apartments")
    private var databaseUserRef = Firebase.database.reference.child("users")
    private var bookingsRef = Firebase.database.reference.child("bookings")
    private var storageInstance = FirebaseStorage.getInstance()
    private var currentUserModel = User()
    private lateinit var storageRef: StorageReference
    var currentUserID: String = ""
    var apartmentID: String = ""

    lateinit var reviewAdapter: ReviewAdapter

    var currentApartmentModel = ApartmentHomePost()
        set(value) {
            field = value
            apartmentModelLiveData.value = value
            getLandLordInfo(value)
        }
    var currentLandLordLiveData = MutableLiveData<User>()

    var imageUrlList = HashMap<Int, Uri>()
    val imageUrlListLiveData = MutableLiveData<HashMap<Int, Uri>>()

    val apartmentModelLiveData = MutableLiveData<ApartmentHomePost>()

    init {
        runBlocking {
            currentUserID = preferencesManager.getUserID()
        }

        viewModelScope.launch {
            databaseUserRef.child(currentUserID).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUserModel = snapshot.getValue(User::class.java) ?: User()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "Error: ${error.message}")
                }
            })
        }
    }

    fun onFragmentCreated(apHomePost: ApartmentHomePost) {

        changeProgressStatus(View.VISIBLE)
        currentApartmentModel = apHomePost

        reviewAdapter = ReviewAdapter(this)

        observePostRating()

        this.apartmentID = apHomePost.apartmentPostID
        checkIfRatingClickable()

        storageRef = storageInstance.reference
            .child("ApartmentUploads")
            .child(currentApartmentModel.timeStamp.toString())

        val userProfileImageRef = storageInstance.reference
            .child("ProfileImages")
            .child(currentApartmentModel.landLordID)

        toggleBooking(currentUserID != currentApartmentModel.landLordID)


        userProfileImageRef.downloadUrl.addOnSuccessListener {
            currentLandLordLiveData.value =
                currentLandLordLiveData.value?.copy(imageUrl = it.toString())
        }

        checkBookings()

        getImageLinks(currentApartmentModel)
    }

    fun getUserModel(): User {
        return if (currentLandLordLiveData.value == null)
            User()
        else currentLandLordLiveData.value as User
    }

    private fun checkBookings() {
        bookingsRef.orderByChild("tenantID")
            .equalTo(currentUserID)
            .addListenerForSingleValueEvent(object : SimpleEventListener() {
                override fun onDataChange(snapshot: DataSnapshot) {
                    super.onDataChange(snapshot)

                    val bookingList = snapshot.children.mapNotNull {
                        BookingModel.createFromSnapshot(it)
                    }

                    Log.d(TAG, "onDataChange: bookingList: $bookingList")

                    val currentApartmentBooking =
                        bookingList.find {
                            it.tenantID == currentUserID &&
                            it.apartmentID == currentApartmentModel.apartmentPostID
                        }

                    Log.d(TAG, "onDataChange: currentApartmentBooking: $currentApartmentBooking")

                    action.set(Action(ActionType.RATING_VISIBILITY, bool = currentApartmentBooking != null))
                }
            })
    }

    private fun getLandLordInfo(apartmentModel: ApartmentHomePost) {
        databaseUserRef.child(apartmentModel.landLordID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(landLordInfo: DataSnapshot) {
                    val landLordModel = landLordInfo.getValue(User::class.java)!!
                    currentLandLordLiveData.value = landLordModel
                }

                override fun onCancelled(error: DatabaseError) {
                    sendToastMessage("Failed to load landlord information")
                }

            })
    }

    fun isRatingVisible() = currentUserID != currentApartmentModel.landLordID

    private fun changeProgressStatus(status: Int, checkRating: Boolean = false) =
        viewModelScope.launch {
            apartmentPageEventChannel.send(ChangeProgressStatus(status, checkRating))
        }

    private fun sendToastMessage(message: String) = viewModelScope.launch {
        apartmentPageEventChannel.send(SendToastMessage(message))
    }

    private fun checkIfRatingClickable() = viewModelScope.launch {
        if (preferencesManager.getUserID() != currentApartmentModel.landLordID)
            apartmentPageEventChannel.send(ChangeRatingClickable(true))
    }

    private fun getImageLinks(apartmentModel: ApartmentHomePost) {
        var count = 0

        for (currentChildID in 0..apartmentModel.imageCount) {

            storageRef.child(currentChildID.toString()).downloadUrl.addOnSuccessListener { downloadURL ->
                imageUrlList[currentChildID] = downloadURL
                count += 1
                if (count > apartmentModel.imageCount) {
                    imageUrlListLiveData.value = imageUrlList
                    currentApartmentModel.imagesList = imageUrlList.values.toMutableList()
                    changeProgressStatus(View.GONE, true)
                }

            }.addOnFailureListener {
                sendToastMessage(it.message.toString())
                changeProgressStatus(View.GONE, true)
            }
        }
    }

    fun getCurrentUserRating() =
        currentApartmentModel.findReviewForID(currentUserID)?.rating ?: 0.0

    fun getCurrentUserComment() =
        currentApartmentModel.findReviewForID(currentUserID)?.comment ?: ""

    private fun observePostRating() {
        apartmentsRef
            .child(currentApartmentModel.apartmentPostID)
            .child("ratings")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val ratings = snapshot.children.map { child ->
                        child.getValue(RatingModel::class.java) ?: return
                    }

                    action.set(
                        Action(
                            ActionType.TOGGLE_REVIEWS_VISIBILITY,
                            bool = !ratings.isNullOrEmpty()
                        )
                    )

                    currentApartmentModel.ratingsList = ratings.toMutableList()
                    reviewAdapter.update()

                    parseRatingImages()

                    Log.d(
                        TAG,
                        "onDataChange: updateRating: ${currentApartmentModel.calculateAverageRating()}"
                    )
                    action.set(
                        Action(
                            ActionType.UPDATE_RATING,
                            avg = currentApartmentModel.calculateAverageRating(),
                            currentApartmentModel.reviewsCount()
                        )
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "onCancelled: ${error.message}")
                }

            })
    }

    fun ratingChanged(rating: Float, comment: String) {
        val ratingModel = RatingModel(
            currentUserID,
            currentUserModel.displayName,
            currentUserModel.imageUrl,
            currentApartmentModel.apartmentPostID,
            rating.toDouble(),
            comment,
            System.currentTimeMillis()
        )

        val ratingsRef = apartmentsRef
            .child(currentApartmentModel.apartmentPostID)
            .child("ratings")

        ratingsRef.child(currentUserID).setValue(ratingModel)
    }

    fun deleteReview() {
        apartmentsRef
            .child(currentApartmentModel.apartmentPostID)
            .child("ratings")
            .child(currentUserID).removeValue()
    }

    fun reviewClicked(review: RatingModel?) {
        review ?: return
        if (review.userID == currentUserID)
            action.set(Action(ActionType.CURRENT_REVIEW_CLICK))
        else action.set(Action(ActionType.OTHER_REVIEW_CLICK, string = review.userID))
    }

    private fun toggleBooking(isVisible: Boolean) = viewModelScope.launch {
        apartmentPageEventChannel.send(ToggleBookVisibility(isVisible))
    }

    private fun parseRatingImages() {
        currentApartmentModel.ratingsList.forEachIndexed { index, rModel ->
            rModel.userID ?: return
            databaseUserRef.child(rModel.userID).addListenerForSingleValueEvent(
                object : SimpleEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)
                        val user = snapshot.getValue(User::class.java)
                        Log.d(TAG, "onDataChange: $user")
                        action.set(
                            Action(
                                ActionType.UPDATE_RATING_IMAGE,
                                string = user?.imageUrl,
                                pos = index
                            )
                        )
                    }
                }
            )
        }
    }

    enum class ActionType {
        UPDATE_RATING,
        UPDATE_RATING_IMAGE,
        TOGGLE_REVIEWS_VISIBILITY,
        CURRENT_REVIEW_CLICK,
        OTHER_REVIEW_CLICK,
        RATING_VISIBILITY
    }

    data class Action(
        val type: ActionType? = null,
        val avg: Double? = null,
        val count: Int? = null,
        val string: String? = null,
        val pos: Int? = null,
        val bool: Boolean? = null
    )
}

sealed class ApartmentPageEvent {
    data class SendToastMessage(val message: String) : ApartmentPageEvent()
    data class ChangeRatingClickable(val status: Boolean) : ApartmentPageEvent()
    data class ChangeProgressStatus(val status: Int, val checkRating: Boolean = false) :
        ApartmentPageEvent()

    data class ToggleBookVisibility(val isVisible: Boolean) : ApartmentPageEvent()
}