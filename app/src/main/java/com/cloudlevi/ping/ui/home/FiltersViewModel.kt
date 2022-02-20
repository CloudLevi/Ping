package com.cloudlevi.ping.ui.home

import androidx.lifecycle.ViewModel
import com.cloudlevi.ping.APT_FURNISHED_ALL
import com.cloudlevi.ping.APT_TYPE_ALL
import com.cloudlevi.ping.PRICE_TYPE_ALL
import com.cloudlevi.ping.ext.ActionLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FiltersViewModel @Inject constructor(
) : ViewModel() {

    val action = ActionLiveData<Action>()

    var sort_type: Int = -1
    var apt_type: Int = APT_TYPE_ALL
    var furniture_type: Int = APT_FURNISHED_ALL
    var rent_type: ArrayList<Int> = arrayListOf(PRICE_TYPE_ALL)

    var minRating = 0F
    var maxRating = 5F

    var minFloor: Float = 0F
    var maxFloor = 0F

    var minRooms: Float = 0F
    var maxRooms = 0F

    var minPrice: Float = 0F
    var maxPrice = 0F

    fun checkedChanged(id: Int) {
        if (rent_type.contains(id)) rent_type.remove(id)
        else rent_type.add(id)
    }

    fun getRatings(sliderMin: Float, sliderMax: Float): Pair<Float, Float> {
        val range = sliderMin..sliderMax
        if (minRating !in range || maxRating !in range) {
            minRating = sliderMin
            maxRating = sliderMax
        }
        return Pair(minRating, maxRating)
    }

    fun getFloors(sliderMin: Float, sliderMax: Float): Pair<Float, Float> {
        val range = sliderMin..sliderMax
        if (minFloor !in range || maxFloor !in range) {
            minFloor = sliderMin
            maxFloor = sliderMax
        }
        return Pair(minFloor, maxFloor)
    }

    fun getRooms(sliderMin: Float, sliderMax: Float): Pair<Float, Float> {
        val range = sliderMin..sliderMax
        if (minRooms !in range || maxRooms !in range) {
            minRooms = sliderMin
            maxRooms = sliderMax
        }
        return Pair(minRooms, maxRooms)
    }

    fun getPrice(sliderMin: Float, sliderMax: Float): Pair<Float, Float> {
        val range = sliderMin..sliderMax
        if (minPrice !in range || maxPrice !in range) {
            minPrice = sliderMin
            maxPrice = sliderMax
        }
        return Pair(minPrice, maxPrice)
    }

    fun resetFilters(totalMaxFloor: Float, totalMaxRooms: Float, totalMaxPrice: Float) {
        sort_type = -1
        apt_type = APT_TYPE_ALL
        furniture_type = APT_FURNISHED_ALL
        rent_type = arrayListOf(PRICE_TYPE_ALL)
        minRating = 0F
        maxRating = 5F
        minFloor = 0F
        maxFloor = totalMaxFloor
        minRooms = 0F
        maxRooms = totalMaxRooms
        minPrice = 0F
        maxPrice = totalMaxPrice

        updateUI()
    }

    private fun updateUI() = action.set(Action(ActionType.UPDATE_UI))

    data class Action(val type: ActionType)

    enum class ActionType {
        UPDATE_UI
    }
}