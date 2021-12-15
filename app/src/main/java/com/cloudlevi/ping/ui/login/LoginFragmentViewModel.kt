package com.cloudlevi.ping.ui.login

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.ext.SimpleEventListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.cloudlevi.ping.ui.login.LoginFragmentEvent.*
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginFragmentViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
): ViewModel() {

    lateinit var googleAcct: GoogleSignInAccount

    private val loginFragmentEventChannel = Channel<LoginFragmentEvent>()
    val loginFragmentEvent =loginFragmentEventChannel.receiveAsFlow()

    lateinit var mGoogleSignInClient: GoogleSignInClient
    private val firebaseAuth= FirebaseAuth.getInstance()
    private val databaseUsersRef = FirebaseDatabase.getInstance().reference.child("users")
    lateinit var gso: GoogleSignInOptions
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

    fun setupGSO(activity: FragmentActivity, c: Context){
        gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(c.getString(R.string.google_client_id))
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(activity,gso)

    }

    fun onLoginCLick(){
        if (loginText.isEmpty() || passwordText.isEmpty()){
            sendToastMessage("Please fill in the blanks!")
            if (loginText.isEmpty()) requestErrorField(REQUEST_ERROR_EMAIL_FIELD)
            if (passwordText.isEmpty()) requestErrorField(REQUEST_ERROR_PASSWORD_FIELD)
        }
        else if (!loginText.contains("@")){
            sendToastMessage("The email is badly formatted.")
            requestErrorField(REQUEST_ERROR_EMAIL_FIELD)
        }
        else if (passwordText.length < 6) {
            sendToastMessage("The password should be 6 characters or longer.")
            requestErrorField(REQUEST_ERROR_PASSWORD_FIELD)
        }
        else{
            changeProgress(View.VISIBLE)
            firebaseAuth.signInWithEmailAndPassword(loginText, passwordText)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        changeProgress(View.GONE)
                        setUserInfoAndLoginSignIn(firebaseAuth.currentUser!!.email ?: "", firebaseAuth.currentUser!!.displayName ?: "")
                    }
                }
                .addOnFailureListener { e ->
                    sendToastMessage(e.message.toString())
                    changeProgress(View.GONE)
            }
        }
    }

    fun onGoogleSignInClick(){
        startGoogleSignIn()
    }

    fun onGoogleSignInComplete(accountGoogle: GoogleSignInAccount){
        googleAcct = accountGoogle
        val credential= GoogleAuthProvider.getCredential(googleAcct.idToken,null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener{task ->
            if(task.isSuccessful){
                changeProgress(View.GONE)
                val user = FirebaseAuth.getInstance().currentUser!!

                setGoogleLoggedIn()
                setUserInfoAndLoginSignIn(googleAcct.email?: "", user.displayName?: "")
                checkIfUserExists(user.uid)
            }
            else {
                changeProgress(View.GONE)
                sendToastMessage(task.exception.toString())
            }
        }
    }

    fun onSignUpClick(){ viewModelScope.launch {
            navigateToSignUpScreen()
        }
    }

    private fun setGoogleLoggedIn() = viewModelScope.launch {
        preferencesManager.setLoggedThroughGoogle(true)
    }

    private fun registerGoogleUser(userID: String?, userEmail: String?, displayName: String?, userName: String?){
        currentUser.userID = userID
        currentUser.email = userEmail
        currentUser.displayName = displayName
        currentUser.username = userName

        databaseUsersRef.child(currentUser.userID?: "empty").setValue(currentUser)
    }

    private fun sendToastMessage(text: String){ viewModelScope.launch {
        loginFragmentEventChannel.send(ShowToastMessage(text))
        }
    }

    private fun checkIfUserExists(userID: String?){
        var userExists = false

        if (userID != null){
            databaseUsersRef.addListenerForSingleValueEvent(object: SimpleEventListener(){
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (currentUser in snapshot.children){
                        val userModel: User = currentUser.getValue(User::class.java)?: return
                        userExists = userModel.userID == userID
                    }
                    if(!userExists) registerGoogleUser(userID, googleAcct.email, googleAcct.displayName, googleAcct.email?.substringBefore("@"))
                }
            })
        }
    }

    private fun requestErrorField(request_id: Int){ viewModelScope.launch {
        loginFragmentEventChannel.send(RequestErrorField(request_id))
    }
    }

    private fun navigateToSignUpScreen(){ viewModelScope.launch {
            loginFragmentEventChannel.send(NavigateToSignUpScreen)
        }
    }

    private fun navigateToHomeScreen(){ viewModelScope.launch {
        loginFragmentEventChannel.send(NavigateToHomeScreen)
        }
    }

    private fun startGoogleSignIn(){ viewModelScope.launch {
        loginFragmentEventChannel.send(StartGoogleSignIn(mGoogleSignInClient.signInIntent))
        }
    }

    private fun setUserInfoAndLoginSignIn(email: String, displayName: String){ viewModelScope.launch {
            preferencesManager.setUserID(firebaseAuth.currentUser!!.uid)
            preferencesManager.setUserEmail(email)
            preferencesManager.setUserDisplayName(displayName)
            navigateToHomeScreen()
        }
    }

    private fun changeProgress(status: Int) = viewModelScope.launch {
        loginFragmentEventChannel.send(ChangeProgress(status))
    }

//    fun checkIfLoggedIn(){
//        val currentUser = firebaseAuth.currentUser
//        if(currentUser != null) setUserInfoAndLoginSignIn(currentUser.email, currentUser.displayName)
//    }

}


sealed class LoginFragmentEvent{
    data class ShowToastMessage(val message: String): LoginFragmentEvent()
    data class StartGoogleSignIn(val intent: Intent): LoginFragmentEvent()
    data class RequestErrorField(val request_id: Int): LoginFragmentEvent()
    data class ChangeProgress(val status: Int): LoginFragmentEvent()
    object NavigateToSignUpScreen: LoginFragmentEvent()
    object NavigateToHomeScreen: LoginFragmentEvent()
}