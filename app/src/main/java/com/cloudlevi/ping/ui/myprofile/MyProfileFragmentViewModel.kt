package com.cloudlevi.ping.ui.myprofile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.data.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.cloudlevi.ping.ui.myprofile.MyProfileFragmentEvent.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyProfileFragmentViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
): ViewModel() {

    private val myProfileFragmentEventChannel = Channel<MyProfileFragmentEvent>()
    val myProfileFragmentEvent = myProfileFragmentEventChannel.receiveAsFlow()

    private val fileStorageReference = FirebaseStorage.getInstance().getReference("ProfileImages")

    private var auth = Firebase.auth
    private var database = Firebase.database.reference
    var loggedThroughGoogle = false
    private val databaseApartments = Firebase.database.reference.child("apartments")
    private val databaseUsers = Firebase.database.reference.child("users")
    private var storageInstance = FirebaseStorage.getInstance()
    private lateinit var profileImageReference: StorageReference

    var imageUriLiveData = MutableLiveData<Uri>()

    private var userID = ""
    var displayName: String = ""

    private var currentUserLists = arrayListOf<ApartmentHomePost>()

    fun fragmentCreate() = viewModelScope.launch {
        loggedThroughGoogle = preferencesManager.getLoggedThroughGoogle()

        displayName = preferencesManager.getUserDisplayName()
        myProfileFragmentEventChannel.send(UpdateUserName("Hello, $displayName"))
        userID = preferencesManager.getUserID()

        profileImageReference = storageInstance.reference.child("ProfileImages").child(userID)
        profileImageReference.downloadUrl.addOnSuccessListener {
            imageUriLiveData.value = it
        }
    }

    fun onLogoutButtonClicked(){
        logoutUser()
    }

    private fun logoutUser(){ viewModelScope.launch {
            auth.signOut()
            myProfileFragmentEventChannel.send(NavigateToLoginScreen)
        }
    }

    fun onMyPostsClicked() {
        databaseApartments.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for (post in snapshot.children){
                    val currentPost = post.getValue(ApartmentHomePost::class.java)
                    if (currentPost?.landLordID == userID)
                        currentUserLists.add(currentPost)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", "Error: ${error.message}")
            }

        })
    }

    fun handleFinishedImageIntent(uri: Uri?, byteArrayData: ByteArray) {
    val uploadReference = fileStorageReference.child(userID)
        uploadReference
            .putBytes(byteArrayData)
            .addOnSuccessListener { task ->
                profileImageUpdated(uri)
            }
            .addOnFailureListener {
                sendToastMessage("Network Error")
            }
    }


    fun onApplyClicked() {

        viewModelScope.launch {
            preferencesManager.setUserDisplayName(displayName)

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdate)?.addOnSuccessListener {
                databaseUsers.child(userID).child("displayName")
                    .setValue(displayName)
                    .addOnSuccessListener {
                        displayNameChanged() }
                    .addOnFailureListener {
                        sendDisplayNameToastMessage("Network Error")
                    }
            }
        }
    }

    private fun displayNameChanged() = viewModelScope.launch {
        myProfileFragmentEventChannel.send(DisplayNameChanged)
    }

    private fun profileImageUpdated(uri: Uri?) = viewModelScope.launch {
        imageUriLiveData.value = uri?: Uri.EMPTY
        //myProfileFragmentEventChannel.send(ProfileImageUpdated(uri))
    }

    private fun sendDisplayNameToastMessage(message: String) = viewModelScope.launch {
        myProfileFragmentEventChannel.send(SendDisplayNameToastMessage(message))
    }

    private fun sendToastMessage(message: String) = viewModelScope.launch {
        myProfileFragmentEventChannel.send(SendToastMessage(message))
    }

    fun getUserModel(): User {
        return User(userID = userID)
    }
}

sealed class MyProfileFragmentEvent{
    object NavigateToLoginScreen: MyProfileFragmentEvent()
    data class UpdateUserName(val userName: String): MyProfileFragmentEvent()
    object DisplayNameChanged: MyProfileFragmentEvent()
    data class ProfileImageUpdated(val uri: Uri?): MyProfileFragmentEvent()
    data class SendDisplayNameToastMessage(val message: String): MyProfileFragmentEvent()
    data class SendToastMessage(val message: String): MyProfileFragmentEvent()
}