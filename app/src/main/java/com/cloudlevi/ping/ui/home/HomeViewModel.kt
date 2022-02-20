package com.cloudlevi.ping.ui.home

import android.location.Geocoder
import android.util.Log
import android.view.View
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.*
import com.cloudlevi.ping.ext.ActionLiveData
import com.cloudlevi.ping.ext.getAddress
import com.cloudlevi.ping.ext.getMax
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
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.ceil

@HiltViewModel
class HomeViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    private val database = Firebase.database.reference.child("apartments")
    var listType: Int = HOMEFRAGMENT_LISTVIEW
    private var geocoder: Geocoder? = null

    val action = ActionLiveData<Action>()

    var allApartments = listOf<ApartmentHomePost>()
    var displayedApartments = ArrayList<ApartmentHomePost>()

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

    var sortBy = SortBy.TIME
    var sortOrder = SortOrder.DESCENDING

    var scrollPosY = 0

    var searchText = state.get<String>("searchText") ?: ""
        set(value) {
            field = value
            updateAndFilterCurrentList()
            state.set("searchText", value)
        }

    private var apartmentEventListener = ApartmentEventListener()

    private val homeFragmentEventChannel = Channel<HomeFragmentEvent>()
    val homeFragmentEvent = homeFragmentEventChannel.receiveAsFlow()

    var currency = "USD"
    var exchangeRate = 1.0


    init {
        viewModelScope.launch {
            listType = preferencesManager.getListType()
        }
        geocoder = Geocoder(PingApplication.instance, Locale.getDefault())
        runBlocking {
            currency = preferencesManager.getCurrency()
            exchangeRate = preferencesManager.getExRate()
        }
    }

    fun fragmentCreated(boolSearch: Boolean) {
        runBlocking {
            currency = preferencesManager.getCurrency()
            exchangeRate = preferencesManager.getExRate()
        }

        allApartments.forEach {
            it.applyCurrency(currency, exchangeRate)
        }
        if (allApartments.isNotEmpty() && !boolSearch)
            getMaxValues(allApartments)
        notifyListUpdated(true)
    }

    fun observeApartmentsList() {
        changeProgressStatus(View.VISIBLE)

        database.addValueEventListener(apartmentEventListener)
    }

    fun listTypeChanged() {
        when (listType) {
            HOMEFRAGMENT_LISTVIEW -> listType = HOMEFRAGMENT_GRIDVIEW
            HOMEFRAGMENT_GRIDVIEW -> listType = HOMEFRAGMENT_LISTVIEW
        }

        viewModelScope.launch {
            preferencesManager.setListType(listType)
        }
    }

    private fun sendToastMessage(message: String) = viewModelScope.launch {
        homeFragmentEventChannel.send(SendToastMessage(message))
    }

    private fun changeProgressStatus(status: Int) = viewModelScope.launch {
        homeFragmentEventChannel.send(ChangeProgressStatus(status))
    }

    fun updateSortOrder(mSortBy: SortBy, mSortOrder: SortOrder) {
        sortBy = mSortBy
        sortOrder = mSortOrder

        updateAndFilterCurrentList()
    }

    inner class ApartmentEventListener : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d("HOME", "onDataChange: count: ${snapshot.childrenCount}, ${snapshot.children}")
            allApartments = snapshot.children.mapNotNull { aptSnapshot ->
                ApartmentHomePost.createFromSnapshot(aptSnapshot, currency, exchangeRate)
            }.toList()
            Log.d("HOME", "onDataChange: after mapNotNull: $allApartments")
            getMaxValues(allApartments)

            updateAndFilterCurrentList()
            Log.d("HOME", "onDataChange: after updateAndFilterCurrentList")
        }

        override fun onCancelled(error: DatabaseError) {
            sendToastMessage(error.message)
            changeProgressStatus(View.GONE)
        }

    }

    fun updateAndFilterCurrentList(
        clearFilters: Boolean = false
    ) {
        if (clearFilters) {
            displayedApartments = ArrayList(allApartments)
            notifyListUpdated()
            return
        }

        var list = allApartments.filter { ap ->
            var addApartment = false

            if (searchText.isEmpty() ||
                ap.title.contains(searchText, ignoreCase = true) ||
                ap.description.contains(searchText, ignoreCase = true)
            ) {
                addApartment = ap.matchesType(currentApt_type) &&
                        ap.matchesFurniture(currentFurniture_type) &&
                        ap.matchesRentType(currentRent_type) &&
                        ap.matchesAverageRating(currentMinRating..currentMaxRating) &&
                        ap.matchesFloor(currentMinFloor..currentMaxFloor) &&
                        ap.matchesRooms(currentMinRooms..currentMaxRooms) &&
                        ap.matchesPrice(currentMinPrice..currentMaxPrice)
            }
            addApartment
        }

        if (sortBy != SortBy.NONE && sortOrder != SortOrder.NONE) {
            when (sortBy) {
                SortBy.PRICE -> list = list.sortedBy { it.calculateFairPrice() }
                SortBy.TIME -> list = list.sortedBy { it.timeStamp }
                SortBy.NAME -> list = list.sortedBy { it.title }
                SortBy.RATING -> list = list.sortedBy { it.calculateAverageRating() }
                SortBy.ACREAGE -> list = list.sortedBy { it.acreage }
                SortBy.ROOM_AMOUNT -> list = list.sortedBy { it.roomAmount }
                SortBy.NONE -> {}
            }
            if (sortOrder == SortOrder.DESCENDING) list = list.reversed()
        }

        displayedApartments = ArrayList(list)
        notifyListUpdated()

        return
    }

    private fun notifyListUpdated(bool: Boolean? = null) {
        viewModelScope.launch {
            displayedApartments = ArrayList(displayedApartments.map { aptPost ->
                aptPost.copy(
                    locationString = aptPost.createLatLng()
                        .getAddress(PingApplication.instance, geocoder)
                )
            })
            action.set(Action(ActionType.LIST_UPDATED, bool = bool))
        }
    }

    private fun getMaxValues(list: List<ApartmentHomePost>) {
        currentMaxFloor = list.getMax(1f) { it.aptFloor.toFloat() }
        currentMaxRooms = list.getMax(1f) { it.roomAmount.toFloat() }
        currentMaxPrice = list.getMax(1f) { ceil(it.mGetCalculationPrice()).toFloat() }

        totalMaxFloor = currentMaxFloor
        totalMaxRooms = currentMaxRooms
        totalMaxPrice = currentMaxPrice

        Log.d(
            "HOME",
            "onDataChange: after getMaxValues: totalMaxFloor: $totalMaxFloor;\n totalMaxRooms: $totalMaxRooms;\ntotalMaxPrice:$totalMaxPrice"
        )
    }

    enum class ActionType {
        LIST_UPDATED
    }

    data class Action(val type: ActionType, val bool: Boolean? = null)
}

sealed class HomeFragmentEvent {
    data class ChangeProgressStatus(val status: Int) : HomeFragmentEvent()
    data class SendToastMessage(val message: String) : HomeFragmentEvent()
}

interface FilterListener {
    fun sortTypeSelected(sort_type: Int)
}