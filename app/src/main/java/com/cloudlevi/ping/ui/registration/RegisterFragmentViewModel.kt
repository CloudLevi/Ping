package com.cloudlevi.ping.ui.registration

import android.content.ContentValues.TAG
import android.util.Log
import android.view.View
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.REQUEST_ERROR_CONFIRM_PASSWORD_FIELD
import com.cloudlevi.ping.REQUEST_ERROR_EMAIL_FIELD
import com.cloudlevi.ping.REQUEST_ERROR_PASSWORD_FIELD
import com.cloudlevi.ping.REQUEST_ERROR_USERNAME_FIELD
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.ui.registration.RegisterFragmentEvent.*
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterFragmentViewModel @Inject constructor(
    private val state: SavedStateHandle
): ViewModel() {

    private var auth = Firebase.auth
    private var database = Firebase.database.reference
    private var currentUser = User()

    private val profileUpdates = UserProfileChangeRequest.Builder()
        .setDisplayName("User")
        .build()

    private val registerFragmentEventChannel = Channel<RegisterFragmentEvent>()
    val registerFragmentEvent = registerFragmentEventChannel.receiveAsFlow()

    var registerUserName = state.get<String>("registerUserName") ?: ""
        set(value) {
            field = value
            state.set("registerUserName", value)
        }

    var registerEmail = state.get<String>("registerEmail") ?: ""
    set(value) {
        field = value
        state.set("registerEmail", value)
    }

    var registerPassword = state.get<String>("registerPassword") ?: ""
        set(value) {
            field = value
            state.set("registerPassword", value)
        }

    var registerConfirmPassword: String = ""

    fun onRegisterClick(){
        if (registerEmail.isEmpty() || registerPassword.isEmpty() || registerUserName.isEmpty() || registerConfirmPassword.isEmpty()){
            sendToastMessage("Please fill in the blanks!")
            if (registerEmail.isEmpty()) requestErrorField(REQUEST_ERROR_EMAIL_FIELD)
            if (registerPassword.isEmpty()) requestErrorField(REQUEST_ERROR_PASSWORD_FIELD)
            if (registerConfirmPassword.isEmpty()) requestErrorField(REQUEST_ERROR_CONFIRM_PASSWORD_FIELD)
            if (registerUserName.isEmpty()) requestErrorField(REQUEST_ERROR_USERNAME_FIELD)
        }
        else if (!registerEmail.contains("@")){
            sendToastMessage("The email is badly formatted.")
            requestErrorField(REQUEST_ERROR_EMAIL_FIELD)
        }
        else if (registerPassword.trim().length < 6) {
            sendToastMessage("The password should be 6 characters or longer.")
            requestErrorField(REQUEST_ERROR_PASSWORD_FIELD)
        }
        else if (registerPassword != registerConfirmPassword) {
            sendToastMessage("The passwords do not match!")
            requestErrorField(REQUEST_ERROR_CONFIRM_PASSWORD_FIELD)
        }
        else registerIfUsernameUnique()

    }

    private fun sendToastMessage(text: String){ viewModelScope.launch {
        registerFragmentEventChannel.send(SendToastMessage(text))
        }
    }
    private fun navigateToLoginScreen(){ viewModelScope.launch {
            registerFragmentEventChannel.send(NavigateToLoginScreen)
        }
    }

    private fun requestErrorField(request_id: Int){ viewModelScope.launch {
        registerFragmentEventChannel.send(RequestErrorField(request_id))
    }

    }

    private fun registerUserWithEmailAndPassword(){
        auth.createUserWithEmailAndPassword(registerEmail, registerPassword)
            .addOnCompleteListener { task ->
                changeProgress(View.GONE)
                if(task.isSuccessful){
                    val user = auth.currentUser!!
                    currentUser.email = user.email
                    currentUser.userID = user.uid
                    currentUser.displayName = "User"
                    user.updateProfile(profileUpdates)
                    currentUser.username = registerUserName
                    database.child("users").child(user.uid).setValue(currentUser)
                    sendToastMessage("User successfully created!")

                    navigateToLoginScreen()
                }
            }
            .addOnFailureListener { exception ->
                changeProgress(View.GONE)
                sendToastMessage(exception.message.toString())
            }
    }

    private fun registerIfUsernameUnique(){

        changeProgress(View.VISIBLE)

        val databaseUsersReference = database.child("users")
        var usernameExists = false

        databaseUsersReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    if (dataSnapshot.child("username").value.toString() == registerUserName) usernameExists = true
                }

                if (usernameExists) sendToastMessage("This username already exists. Please choose a new one.")
                else registerUserWithEmailAndPassword()
            }

            override fun onCancelled(error: DatabaseError) {
                sendToastMessage("Network error. Please try again later")
            }

        })

    }

    private fun changeProgress(status: Int) = viewModelScope.launch {
        registerFragmentEventChannel.send(ChangeProgress(status))
    }
}

sealed class RegisterFragmentEvent{
    data class SendToastMessage(val message: String): RegisterFragmentEvent()
    data class RequestErrorField(val request_id: Int): RegisterFragmentEvent()
    data class ChangeProgress(val status: Int): RegisterFragmentEvent()
    object NavigateToLoginScreen: RegisterFragmentEvent()
}