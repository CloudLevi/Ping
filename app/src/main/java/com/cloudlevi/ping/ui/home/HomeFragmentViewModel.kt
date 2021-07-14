package com.cloudlevi.ping.ui.home

import android.content.ContentValues.TAG
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.db.PingDao
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.cloudlevi.ping.ui.home.HomeFragmentEvent.*
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeFragmentViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle,
    private val pingDao: PingDao
):ViewModel() {

    lateinit var userEmail: String
    lateinit var userDisplayName: String
    val apartmentsList = ArrayList<ApartmentHomePost>()
    private val database = Firebase.database.reference.child("apartments")
    var listType: Int = HOMEFRAGMENT_LISTVIEW
    val homeLiveData = MutableLiveData<ArrayList<ApartmentHomePost>>()

    var currentSortType = -1
    var currentApt_type: Int = APT_TYPE_ALL
    var currentFurniture_type: Int = APT_FURNISHED_ALL
    var currentRent_type: ArrayList<Int> = arrayListOf(PRICE_TYPE_ALL)
    var currentMinRating: Float = 0F
    var currentMaxRating: Float = 5F

    var currentMinFloor: Float = 0F
    var currentMinRooms: Float = 0F
    var currentMinPrice: Float = 0F


    var currentMaxFloor = 0F
    var currentMaxRooms = 0F
    var currentMaxPrice = 0F

    var totalMaxFloor = 0F
    var totalMaxRooms = 0F
    var totalMaxPrice = 0F

    var searchText = state.get<String>("searchText")?: ""
        set(value) {
            field = value
            queryListener.queryText = value
            state.set("searchText", value)
        }

    private val streamValueEventListener = StreamValueEventListener()
    private var queryListener = QueryValueEventListener("")

    private val homeFragmentEventChannel = Channel<HomeFragmentEvent>()
    val homeFragmentEvent = homeFragmentEventChannel.receiveAsFlow()


    init {
        viewModelScope.launch {
            listType = preferencesManager.getListType()
        }
    }

    fun observeApartmentsList(){
        changeProgressStatus(View.VISIBLE)

        database.addValueEventListener(streamValueEventListener)
    }

    fun applySearchWithFilters(){
        database.removeEventListener(streamValueEventListener)

        when(currentSortType){
            -1 -> {
                queryListener.reverse = false
                database.orderByChild("description")
                    .limitToLast(100)
                    .addListenerForSingleValueEvent(queryListener)
            }

            CHEAPEST_FIRST_CHIP -> {
                queryListener.reverse = false
                database.orderByChild("price")
                    .limitToLast(100)
                    .addListenerForSingleValueEvent(queryListener)
            }

            EXPENSIVE_FIRST_CHIP -> {
                queryListener.reverse = true
                database.orderByChild("price")
                    .limitToLast(100)
                    .addListenerForSingleValueEvent(queryListener)
            }

            NEWEST_FIRST_CHIP -> {
                queryListener.reverse = true
                database.orderByChild("timestamp")
                    .limitToLast(100)
                    .addListenerForSingleValueEvent(queryListener)
            }

            HIGHER_RATED_FIRST_CHIP -> {
                queryListener.reverse = true
                database.orderByChild("rating")
                    .limitToLast(100)
                    .addListenerForSingleValueEvent(queryListener)
            }
        }

    }

    fun onFilterClicked(){
    }

    fun listTypeChanged(){
        when(listType){
            HOMEFRAGMENT_LISTVIEW -> listType = HOMEFRAGMENT_GRIDVIEW
            HOMEFRAGMENT_GRIDVIEW -> listType = HOMEFRAGMENT_LISTVIEW
        }

        viewModelScope.launch {
            preferencesManager.setListType(listType)
        }
    }

    fun getUserInfoFromDataStore(){
        viewModelScope.launch {
            userEmail = preferencesManager.getUserEmail()
            userDisplayName = preferencesManager.getUserDisplayName()
        }
    }

    private fun sendToastMessage(message: String) = viewModelScope.launch {
        homeFragmentEventChannel.send(SendToastMessage(message))
    }

    private fun sendChipSelected(chipID: Int, oldSortChip: Int) = viewModelScope.launch {
        homeFragmentEventChannel.send(SendChipSelected(chipID, oldSortChip))
    }

    private fun changeProgressStatus(status: Int) = viewModelScope.launch {
        homeFragmentEventChannel.send(ChangeProgressStatus(status))
    }

    inner class QueryValueEventListener(var queryText: String, var reverse: Boolean = false): ValueEventListener {
        override fun onDataChange(aptsList: DataSnapshot) {
            apartmentsList.clear()

            for (apartment in aptsList.children) {
                val apartmentHomePost = apartment.getValue(ApartmentHomePost::class.java)!!
                val isFurnished = if (apartmentHomePost.isFurnished) APT_FURNISHED_YES else APT_FURNISHED_NO

                var addApartment = true

                if (
                    apartmentHomePost.title.contains(queryText, ignoreCase = true)
                    || apartmentHomePost.description.contains(queryText, ignoreCase = true)
                    || queryText.isNullOrEmpty()
                ) {

                    //Filter by apartment type
                    if (currentApt_type != APT_TYPE_ALL && apartmentHomePost.aptType != currentApt_type)
                        addApartment = false
                    //Filter by furniture
                    if (currentFurniture_type != APT_FURNISHED_ALL && isFurnished != currentFurniture_type)
                        addApartment = false
                    //Filter rent type
                    if (!currentRent_type.contains(PRICE_TYPE_ALL) && !currentRent_type.contains(apartmentHomePost.priceType))
                        addApartment = false
                    //Filter by rating
                    if (apartmentHomePost.rating !in currentMinRating..currentMaxRating)
                        addApartment = false
                    //Filter by floor
                    if (apartmentHomePost.aptFloor.toFloat() !in currentMinFloor..currentMaxFloor)
                        addApartment = false
                    //Filter by rooms
                    if (apartmentHomePost.roomAmount.toFloat() !in currentMinRooms..currentMaxRooms)
                        addApartment = false
                    //Filter by price
                    if (apartmentHomePost.price.toFloat() !in currentMinPrice..currentMaxPrice)
                        addApartment = false

                    //If all requirements are met, add the post
                    if (addApartment) apartmentsList.add(apartmentHomePost)
                }
            }
            if (reverse)
                apartmentsList.reverse()
            changeProgressStatus(View.GONE)
            homeLiveData.value = apartmentsList

        }

        override fun onCancelled(error: DatabaseError) {
            sendToastMessage(error.message)
            changeProgressStatus(View.GONE)
        }
    }

    inner class StreamValueEventListener: ValueEventListener {
        override fun onDataChange(aptsList: DataSnapshot) {
            apartmentsList.clear()
            for (apartment in aptsList.children) {
                val apartmentHomePost = apartment.getValue(ApartmentHomePost::class.java)
                apartmentsList.add(apartmentHomePost!!)
            }
            changeProgressStatus(View.GONE)
            getMaxValues()
            homeLiveData.value = apartmentsList

        }

        override fun onCancelled(error: DatabaseError) {
            sendToastMessage(error.message)
            changeProgressStatus(View.GONE)
        }
    }

    private fun getMaxValues(){
        var localMaxFloor = 0
        var localMaxRooms = 0
        var localMaxPrice = 0

        for (item in apartmentsList){
            if (item.aptFloor >= localMaxFloor) localMaxFloor = item.aptFloor
            if (item.roomAmount >= localMaxRooms) localMaxRooms = item.roomAmount
            if (item.price >= localMaxPrice) localMaxPrice = item.price
        }

        currentMaxFloor = localMaxFloor.toFloat()
        currentMaxRooms = localMaxRooms.toFloat()
        currentMaxPrice = localMaxPrice.toFloat()

        totalMaxFloor = currentMaxFloor
        totalMaxRooms = currentMaxRooms
        totalMaxPrice = currentMaxPrice
    }
}


sealed class HomeFragmentEvent{
    data class ChangeProgressStatus(val status: Int): HomeFragmentEvent()
    data class SendToastMessage(val message: String): HomeFragmentEvent()
    data class SendChipSelected(val chipID: Int, val chipOldID: Int): HomeFragmentEvent()
}

interface FilterListener{
    fun sortTypeSelected(sort_type: Int)
}