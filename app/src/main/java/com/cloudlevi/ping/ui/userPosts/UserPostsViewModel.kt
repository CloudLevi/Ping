package com.cloudlevi.ping.ui.userPosts

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.data.RatingModel
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.ext.ActionLiveData
import com.cloudlevi.ping.ext.SimpleEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.cloudlevi.ping.ui.userPosts.UserPostsEvent.*
import com.google.android.gms.auth.api.signin.internal.Storage
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class UserPostsViewModel @Inject constructor(
    private val dataStoreManager: PreferencesManager
) : ViewModel() {

    val action = ActionLiveData<Action>()

    private val databaseApartments = Firebase.database.reference.child("apartments")
    private val usersRef = Firebase.database.reference.child("users")

    private val storageInstance = FirebaseStorage.getInstance()

    var currentUserID = ""
    var currentUserLists = listOf<ApartmentHomePost>()

    var otherUserModel: User? = null

    var currency = "USD"
    var exRate = 1.0

    init {
        viewModelScope.launch {
            currentUserID = dataStoreManager.getUserID()
        }
    }

    fun fragmentCreate() {
        runBlocking {
            currency = dataStoreManager.getCurrency()
            exRate = dataStoreManager.getExRate()
        }

        currentUserLists.forEach {
            it.applyCurrency(currency, exRate)
        }
        action.set(Action(ActionType.LIST_UPDATED))
    }

    fun getPosts(userModel: User) {
        toggleLoading(true)
        otherUserModel = userModel
        if (userModel.userID != currentUserID)
            displayUserInfo(userModel)

        listenToPosts(userModel.userID ?: "")
    }

    fun getPosts(userID: String) {
        toggleLoading(true)
        getUserModel(userID)
    }

    private fun getUserModel(userID: String) {
        usersRef.child(userID).addListenerForSingleValueEvent(object : SimpleEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                super.onDataChange(snapshot)
                otherUserModel = User.createFromSnapshot(snapshot, storageInstance.reference)

                displayUserInfo(otherUserModel)
                listenToPosts(userID)
            }
        })
    }

    private fun listenToPosts(userID: String) {
        databaseApartments
            .orderByChild("landLordID")
            .equalTo(userID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUserLists = snapshot.children.mapNotNull { childSnapshot ->
                        ApartmentHomePost.createFromSnapshot(childSnapshot, currency, exRate)
                    }
                    action.set(Action(ActionType.LIST_UPDATED))
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("TAG", "Error: ${error.message}")
                    sendToastMessage("Error loading posts")
                }
            })
    }

    fun getOtherUserImageRef(): StorageReference? {
        val imgRef = otherUserModel?.imageRefString?: return null
        return storageInstance.getReferenceFromUrl(imgRef)
    }

    private fun sendToastMessage(message: String) =
        action.set(Action(ActionType.SEND_TOAST, string = message))

    private fun displayUserInfo(userModel: User?) =
        action.set(Action(ActionType.DISPLAY_USER_INFO, userModel = userModel))

    private fun toggleLoading(isLoading: Boolean) =
        action.set(Action(ActionType.TOGGLE_LOADING, bool = isLoading))

    enum class ActionType {
        TOGGLE_LOADING,
        LIST_UPDATED,
        SEND_TOAST,
        DISPLAY_USER_INFO
    }

    data class Action(
        val type: ActionType,
        val bool: Boolean? = null,
        val string: String? = null,
        val userModel: User? = null
    )
}

sealed class UserPostsEvent {
    data class SendToastMessage(val message: String) : UserPostsEvent()
    data class DisplayUserInfo(val status: Boolean, val userModel: User) : UserPostsEvent()
}