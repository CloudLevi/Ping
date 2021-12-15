package com.cloudlevi.ping.ui.myprofile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.api.ExchangeApiService
import com.cloudlevi.ping.data.ExchangeModel
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.ext.ActionLiveData
import com.cloudlevi.ping.toCurrencySymbol
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.cloudlevi.ping.ui.myprofile.MyProfileFragmentEvent.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class MyProfileFragmentViewModel @Inject constructor(
    private val datastoreManager: PreferencesManager,
    private val exchangeApi: ExchangeApiService
) : ViewModel() {

    val action = ActionLiveData<Action>()

    private val myProfileFragmentEventChannel = Channel<MyProfileFragmentEvent>()
    val myProfileFragmentEvent = myProfileFragmentEventChannel.receiveAsFlow()

    private val fileStorageReference = FirebaseStorage.getInstance().getReference("ProfileImages")

    private var auth = Firebase.auth
    var loggedThroughGoogle = false
    private val databaseUsers = Firebase.database.reference.child("users")
    private var storageInstance = FirebaseStorage.getInstance()
    private lateinit var profileImageReference: StorageReference

    var imageUriLiveData = MutableLiveData<Uri>()

    private var userID = ""
    var displayName: String = ""

    fun fragmentCreate() = viewModelScope.launch {
        loggedThroughGoogle = datastoreManager.getLoggedThroughGoogle()

        displayName = datastoreManager.getUserDisplayName()
        myProfileFragmentEventChannel.send(UpdateUserName("Hello, $displayName"))
        userID = datastoreManager.getUserID()

        profileImageReference = storageInstance.reference.child("ProfileImages").child(userID)
        profileImageReference.downloadUrl.addOnSuccessListener {
            imageUriLiveData.value = it
        }
    }

    fun onLogoutButtonClicked() {
        logoutUser()
    }

    private fun logoutUser() {
        viewModelScope.launch {
            auth.signOut()
            myProfileFragmentEventChannel.send(NavigateToLoginScreen)
        }
    }

    fun handleFinishedImageIntent(uri: Uri?, byteArrayData: ByteArray) {
        val uploadReference = fileStorageReference.child(userID)
        uploadReference
            .putBytes(byteArrayData)
            .addOnSuccessListener { task ->
                fileStorageReference.child(userID).downloadUrl
                    .addOnSuccessListener { uri -> saveDownloadURL(uri) }

                profileImageUpdated(uri)
            }
            .addOnFailureListener {
                sendToastMessage("Network Error")
            }
    }

    private fun saveDownloadURL(uri: Uri) {
        databaseUsers.child(userID).child("imageUrl").setValue(uri.toString())
    }

    fun onApplyClicked() {

        viewModelScope.launch {
            datastoreManager.setUserDisplayName(displayName)

            val profileUpdate = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            FirebaseAuth.getInstance().currentUser?.updateProfile(profileUpdate)
                ?.addOnSuccessListener {
                    databaseUsers.child(userID).child("displayName")
                        .setValue(displayName)
                        .addOnSuccessListener {
                            displayNameChanged()
                        }
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
        imageUriLiveData.value = uri ?: Uri.EMPTY
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

    fun getExchangeRate(toCurrency: String) {
        action.set(Action(ActionType.TOGGLE_LOADING, true))
        val call = exchangeApi.getExchangeRate("USD", toCurrency, 1.0)

        call.enqueue(object : Callback<ExchangeModel> {
            override fun onResponse(call: Call<ExchangeModel>, response: Response<ExchangeModel>) {
                if (response.isSuccessful) {
                    val double = response.body()?.rates?.get(toCurrency)?.rate?: 1.0
                    saveCurrency(toCurrency)
                    saveExRate(double)
                    action.set(Action(ActionType.CURRENCY_RECEIVED, string = toCurrency.toCurrencySymbol()))
                } else {
                    Log.d("TAG", "response failed with code: ${response.code()} and body: ${response.errorBody().toString()}")
                    action.set(Action(ActionType.CURRENCY_CALL_FAILED))
                }
            }

            override fun onFailure(call: Call<ExchangeModel>, t: Throwable) {
                Log.d("TAG", "response failed with message: ${t.message}")
                action.set(Action(ActionType.CURRENCY_CALL_FAILED))
            }

        })
    }

    private fun saveCurrency(currency: String) = viewModelScope.launch {
        Log.d("TAG", "saveCurrency: $currency")
        datastoreManager.setCurrency(currency)
    }

    private fun saveExRate(exRate: Double) = viewModelScope.launch {
        Log.d("TAG", "saveExRate: $exRate")
        datastoreManager.setExchangeRate(exRate)
    }

    enum class ActionType {
        TOGGLE_LOADING,
        CURRENCY_RECEIVED,
        CURRENCY_CALL_FAILED
    }
    data class Action(val type: ActionType, val bool: Boolean? = null, val double: Double? = null, val string: String? = null)
}

sealed class MyProfileFragmentEvent {
    object NavigateToLoginScreen : MyProfileFragmentEvent()
    data class UpdateUserName(val userName: String) : MyProfileFragmentEvent()
    object DisplayNameChanged : MyProfileFragmentEvent()
    data class ProfileImageUpdated(val uri: Uri?) : MyProfileFragmentEvent()
    data class SendDisplayNameToastMessage(val message: String) : MyProfileFragmentEvent()
    data class SendToastMessage(val message: String) : MyProfileFragmentEvent()
}