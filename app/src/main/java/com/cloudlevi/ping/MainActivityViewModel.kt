package com.cloudlevi.ping

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.data.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val dataStoreManager: PreferencesManager
):ViewModel() {

    fun setUserData(userID: String, userEmail: String, userDisplayName: String) = viewModelScope.launch {
        dataStoreManager.apply {
            setUserID(userID)
            setUserEmail(userEmail)
            setUserDisplayName(userDisplayName)
        }
    }
}