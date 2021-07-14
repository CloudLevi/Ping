package com.cloudlevi.ping.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cloudlevi.ping.HOMEFRAGMENT_LISTVIEW
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.preferencesDatastore by preferencesDataStore(("user_preferences"))

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.preferencesDatastore

    suspend fun setUserID(userID: String){
        dataStore.edit { preferences -> preferences[PreferencesKeys.USER_ID] = userID}
    }

    suspend fun getUserID(): String =
        dataStore.data.first()[PreferencesKeys.USER_ID] ?: ""

    suspend fun setUserEmail(email: String){
        dataStore.edit { preferences -> preferences[PreferencesKeys.USER_EMAIL] = email}
    }
    suspend fun getUserEmail(): String =
        dataStore.data.first()[PreferencesKeys.USER_EMAIL] ?: ""

    suspend fun setUserDisplayName(displayName: String){
        dataStore.edit { preferences -> preferences[PreferencesKeys.USER_DISPLAY_NAME] = displayName}
    }
    suspend fun getUserDisplayName(): String =
        dataStore.data.first()[PreferencesKeys.USER_DISPLAY_NAME] ?: ""

    suspend fun setListType(listType: Int){
        dataStore.edit { preferences -> preferences[PreferencesKeys.LIST_TYPE] = listType }
    }

    suspend fun getListType(): Int =
        dataStore.data.first()[PreferencesKeys.LIST_TYPE] ?: HOMEFRAGMENT_LISTVIEW

    suspend fun setLoggedThroughGoogle(status: Boolean){
        dataStore.edit { preferences -> preferences[PreferencesKeys.LOGGED_THROUGH_GOOGLE] = status}
    }

    suspend fun getLoggedThroughGoogle(): Boolean =
        dataStore.data.first()[PreferencesKeys.LOGGED_THROUGH_GOOGLE] ?: false


    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val LIST_TYPE = intPreferencesKey("homepage_list_type")
        val LOGGED_THROUGH_GOOGLE = booleanPreferencesKey("logged_through_google")
    }
}