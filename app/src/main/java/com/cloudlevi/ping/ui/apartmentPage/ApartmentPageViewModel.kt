package com.cloudlevi.ping.ui.apartmentPage

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.data.RatedPost
import com.cloudlevi.ping.data.User
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
import javax.inject.Inject

@HiltViewModel
class ApartmentPageViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
): ViewModel() {

    private val apartmentPageEventChannel = Channel<ApartmentPageEvent>()
    val apartmentPageEvent = apartmentPageEventChannel.receiveAsFlow()

    private var databaseRef = Firebase.database.reference.child("apartments")
    private var databaseUserRef = Firebase.database.reference.child("users")
    private var storageInstance = FirebaseStorage.getInstance()
    private var currentUserModel = User()
    private lateinit var storageRef: StorageReference
    private var currentUserID: String = ""
    var apartmentID: String = ""

    var currentApartmentModel = ApartmentHomePost()
    set(value) {
        field = value
        apartmentModelLiveData.value = value
        getLandLordInfo(value)
    }
    var currentLandLordLiveData = MutableLiveData<User>()

    var imageUrlList = HashMap<Int, String>()
    val imageUrlListLiveData = MutableLiveData<HashMap<Int, String>>()

    val apartmentModelLiveData = MutableLiveData<ApartmentHomePost>()

    init {
        viewModelScope.launch {
            currentUserID = preferencesManager.getUserID()
            databaseUserRef.child(currentUserID).addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUserModel = snapshot.getValue(User::class.java)?: User()
                    for (item in snapshot.child("rated_posts").children){
                        currentUserModel.rateList.add(item.getValue(RatedPost::class.java)?: RatedPost())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "Error: ${error.message}")
                }
            })
        }
    }

    fun onFragmentCreated(apartmentID: String){

        changeProgressStatus(View.VISIBLE)

        this.apartmentID = apartmentID

        databaseRef.child("/$apartmentID").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(apartment: DataSnapshot) {
                currentApartmentModel = apartment.getValue(ApartmentHomePost::class.java)?: ApartmentHomePost()
                checkIfRatingClickable()

                storageRef = storageInstance.reference.child("ApartmentUploads").child(currentApartmentModel.timeStamp.toString())
                val userProfileImageRef = storageInstance.reference.child("ProfileImages").child(currentApartmentModel.landLordID)
                userProfileImageRef.downloadUrl.addOnSuccessListener {
                    currentLandLordLiveData.value = currentLandLordLiveData.value?.copy(imageUrl = it.toString())
                }

                getImageLinks(currentApartmentModel)

            }

            override fun onCancelled(error: DatabaseError) {
                changeProgressStatus(View.GONE)
                sendToastMessage(error.message)
            }
        })
    }

    fun getUserModel(): User{
        return if (currentLandLordLiveData.value == null)
            User()
        else currentLandLordLiveData.value as User
    }

    private fun getLandLordInfo(apartmentModel: ApartmentHomePost){
        databaseUserRef.child(apartmentModel.landLordID).addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(landLordInfo: DataSnapshot) {
                val landLordModel = landLordInfo.getValue(User::class.java)!!
                currentLandLordLiveData.value = landLordModel
            }

            override fun onCancelled(error: DatabaseError) {
                sendToastMessage("Failed to load landlord information")
            }

        })
    }

    private fun changeProgressStatus(status: Int) = viewModelScope.launch {
        apartmentPageEventChannel.send(ChangeProgressStatus(status))
    }

    private fun sendToastMessage(message: String) = viewModelScope.launch {
        apartmentPageEventChannel.send(SendToastMessage(message))
    }

    private fun checkIfRatingClickable() = viewModelScope.launch {
        if (preferencesManager.getUserID() != currentApartmentModel.landLordID)
            apartmentPageEventChannel.send(ChangeRatingClickable(true))
    }

    private fun getImageLinks(apartmentModel: ApartmentHomePost){
        var count = 0

        for (currentChildID in 0..apartmentModel.imageCount){

                storageRef.child(currentChildID.toString()).downloadUrl.addOnSuccessListener { downloadURL ->
                    imageUrlList[currentChildID] = downloadURL.toString()
                    count += 1
                    if (count > apartmentModel.imageCount) {
                        imageUrlListLiveData.value = imageUrlList
                        changeProgressStatus(View.GONE)
                    }

                }.addOnFailureListener {
                    sendToastMessage(it.message.toString())
                    changeProgressStatus(View.GONE)
                }
        }
    }

    fun ratingChanged(newRating: Float) {
        for (item in currentUserModel.rateList){
            if (item.post_id == currentApartmentModel.apartmentPostID){
                val ratingDifference = newRating - item.rate
                currentApartmentModel.ratingTotal += ratingDifference
                currentApartmentModel.rating = currentApartmentModel.ratingTotal/currentApartmentModel.ratingQuantity

                val newUserRating = item.copy(rate = newRating)

                databaseRef.child(apartmentID).setValue(currentApartmentModel)
                databaseUserRef.child(currentUserID).child("rated_posts").child(item.post_id).setValue(newUserRating)
                return
            }
        }
        //First rating of current user
        currentApartmentModel.ratingQuantity += 1
        currentApartmentModel.ratingTotal += newRating
        currentApartmentModel.rating = currentApartmentModel.ratingTotal/currentApartmentModel.ratingQuantity

        val newUserRating = RatedPost(post_id = currentApartmentModel.apartmentPostID, currentUserID, newRating)

        databaseRef.child(apartmentID).setValue(currentApartmentModel)
        databaseUserRef.child(currentUserID).child("rated_posts").child(currentApartmentModel.apartmentPostID).setValue(newUserRating)
    }
}

sealed class ApartmentPageEvent{
    data class SendToastMessage(val message: String): ApartmentPageEvent()
    data class ChangeRatingClickable(val status: Boolean): ApartmentPageEvent()
    data class ChangeProgressStatus(val status: Int): ApartmentPageEvent()
}