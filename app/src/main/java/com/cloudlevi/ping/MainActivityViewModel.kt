package com.cloudlevi.ping

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.data.PreferencesManager
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val dataStoreManager: PreferencesManager
) : ViewModel() {

    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

    fun setUserData(userID: String, userEmail: String, userDisplayName: String) =
        viewModelScope.launch {
            dataStoreManager.apply {
                setUserID(userID)
                setUserEmail(userEmail)
                setUserDisplayName(userDisplayName)
            }
        }

    fun getSelectedCurrency() = runBlocking {
        return@runBlocking dataStoreManager.getCurrency()
    }

    fun getExRate() = runBlocking {
        return@runBlocking dataStoreManager.getExRate()
    }

    fun setUserOnline(isOnline: Boolean) {
        viewModelScope.launch {
            val userID = dataStoreManager.getUserID()
            Log.d("TAG", "setUserOnline: $isOnline, userID: $userID")
            if(userID.trim().isNotEmpty())
                usersRef.child(userID).child("userOnline").setValue(isOnline)
        }
    }
}