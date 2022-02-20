package com.cloudlevi.ping.ui.login

import android.view.View
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.ext.ActionLiveData
import com.cloudlevi.ping.ext.SimpleEventListener
import com.cloudlevi.ping.ext.isValidEmail
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.cloudlevi.ping.ui.login.LoginFragmentEvent.*
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginFragmentViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    lateinit var googleAcct: GoogleSignInAccount

    private val loginFragmentEventChannel = Channel<LoginFragmentEvent>()
    val loginFragmentEvent = loginFragmentEventChannel.receiveAsFlow()

    val action = ActionLiveData<Action>()

//    lateinit var gso: GoogleSignInOptions
//    lateinit var mGoogleSignInClient: GoogleSignInClient
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val databaseUsersRef = FirebaseDatabase.getInstance().reference.child("users")
    private var currentUser = User()

    var loginText: String = state.get<String>("loginText") ?: ""
        set(value) {
            field = value
            state.set("loginText", value)
        }
    var passwordText: String = state.get<String>("passwordText") ?: ""
        set(value) {
            field = value
            state.set("passwordText", value)
        }

//    fun setupGSO(activity: FragmentActivity, c: Context) {
//        gso =
//            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestIdToken(BuildConfig.G_CLIENT_ID)
//                .requestEmail()
//                .build()
//        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso)
//    }

    fun onLoginClick() {
        if (loginText.isEmpty() || passwordText.isEmpty()) {
            sendToastMessage("Please fill in the blanks!")
            if (loginText.isEmpty()) requestErrorField(REQUEST_ERROR_EMAIL_FIELD)
            if (passwordText.isEmpty()) requestErrorField(REQUEST_ERROR_PASSWORD_FIELD)
        } else if (!loginText.isValidEmail()) {
            sendToastMessage("The email is badly formatted.")
            requestErrorField(REQUEST_ERROR_EMAIL_FIELD)
        } else if (passwordText.length < 6) {
            sendToastMessage("The password should be 6 characters or longer.")
            requestErrorField(REQUEST_ERROR_PASSWORD_FIELD)
        } else {
            changeProgressStatus(View.VISIBLE)
            firebaseAuth.signInWithEmailAndPassword(loginText, passwordText)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        changeProgressStatus(View.GONE)
                        setUserInfoAndLoginSignIn(
                            firebaseAuth.currentUser!!.email ?: "",
                            firebaseAuth.currentUser!!.displayName ?: "",
                            false
                        )
                    }
                }
                .addOnFailureListener { e ->
                    sendToastMessage(e.message.toString())
                    changeProgressStatus(View.GONE)
                }
        }
    }

    fun onGoogleSignInComplete(accountGoogle: GoogleSignInAccount) {
        googleAcct = accountGoogle
        val credential = GoogleAuthProvider.getCredential(googleAcct.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                changeProgressStatus(View.GONE)
                val user = FirebaseAuth.getInstance().currentUser!!

                setUserInfoAndLoginSignIn(googleAcct.email ?: "", user.displayName ?: "", true)
                checkIfUserExists(user.uid)
            } else {
                changeProgressStatus(View.GONE)
                sendToastMessage(task.exception.toString())
            }
        }
    }

    private fun registerGoogleUser(
        userID: String?,
        userEmail: String?,
        displayName: String?,
        userName: String?
    ) {
        currentUser.userID = userID
        currentUser.email = userEmail
        currentUser.displayName = displayName
        currentUser.username = userName
        currentUser.userOnline = true

        databaseUsersRef.child(currentUser.userID ?: "empty").setValue(currentUser)
    }

    private fun sendToastMessage(text: String) {
        viewModelScope.launch {
            loginFragmentEventChannel.send(ShowToastMessage(text))
        }
    }

    private fun checkIfUserExists(userID: String?) {
        var userExists = false

        if (userID != null) {
            databaseUsersRef.addListenerForSingleValueEvent(object : SimpleEventListener() {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (currentUser in snapshot.children) {
                        val userModel: User = currentUser.getValue(User::class.java) ?: return
                        userExists = userModel.userID == userID
                    }
                    if (!userExists) registerGoogleUser(
                        userID,
                        googleAcct.email,
                        googleAcct.displayName,
                        googleAcct.email?.substringBefore("@")
                    )
                }
            })
        }
    }

    private fun requestErrorField(request_id: Int) {
        viewModelScope.launch {
            loginFragmentEventChannel.send(RequestErrorField(request_id))
        }
    }

    private fun setUserInfoAndLoginSignIn(email: String, displayName: String, isLoggedViaGoogle: Boolean) {
        viewModelScope.launch {
            preferencesManager.apply {
                setLoggedThroughGoogle(isLoggedViaGoogle)
                setUserID(firebaseAuth.currentUser!!.uid)
                setUserEmail(email)
                setUserDisplayName(displayName)
            }
            action.set(Action(ActionType.NAVIGATE_TO_HOME_SCREEN))
        }
    }

    private fun changeProgressStatus(status: Int) = viewModelScope.launch {
        loginFragmentEventChannel.send(ChangeProgress(status))
    }

}

data class Action(val type: ActionType)

enum class ActionType {
    NAVIGATE_TO_HOME_SCREEN,
}


sealed class LoginFragmentEvent {
    data class ShowToastMessage(val message: String) : LoginFragmentEvent()
    data class RequestErrorField(val request_id: Int) : LoginFragmentEvent()
    data class ChangeProgress(val status: Int) : LoginFragmentEvent()
}