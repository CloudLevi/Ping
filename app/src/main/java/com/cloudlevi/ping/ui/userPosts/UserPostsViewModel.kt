package com.cloudlevi.ping.ui.userPosts

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.data.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import com.cloudlevi.ping.ui.userPosts.UserPostsEvent.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPostsViewModel @Inject constructor(
    private val dataStoreManager: PreferencesManager
):ViewModel() {

    var homePostsLiveData = MutableLiveData<ArrayList<ApartmentHomePost>>()
    private val databaseApartments = Firebase.database.reference.child("apartments")
    private var currentUserID = ""
    private var currentUserLists = ArrayList<ApartmentHomePost>()

    private val userPostsEventChannel = Channel<UserPostsEvent>()
    val userPostsEvent = userPostsEventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            currentUserID = dataStoreManager.getUserID()
        }
    }

    fun getPosts(userModel: User) {
        if (userModel.userID != currentUserID)
            displayUserInfo(true, userModel)

        databaseApartments.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUserLists.clear()
                for (post in snapshot.children){
                    val currentPost = post.getValue(ApartmentHomePost::class.java)
                    if (currentPost?.landLordID == userModel.userID!!)
                        currentUserLists.add(currentPost)
                }
                homePostsLiveData.value = currentUserLists
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("TAG", "Error: ${error.message}")
                sendToastMessage("Error loading posts")
            }

        })
    }

    private fun sendToastMessage(message: String) = viewModelScope.launch {
        userPostsEventChannel.send(SendToastMessage(message))
    }

    private fun displayUserInfo(status: Boolean, userModel: User) = viewModelScope.launch {
        userPostsEventChannel.send(DisplayUserInfo(status, userModel))
    }
}

sealed class UserPostsEvent{
    data class SendToastMessage(val message: String): UserPostsEvent()
    data class DisplayUserInfo(val status: Boolean, val userModel: User): UserPostsEvent()
}