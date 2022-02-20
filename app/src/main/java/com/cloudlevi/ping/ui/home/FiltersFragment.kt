package com.cloudlevi.ping.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.cloudlevi.ping.*
import com.cloudlevi.ping.databinding.FragmentFiltersBinding
import com.cloudlevi.ping.ext.makeGone
import com.cloudlevi.ping.ext.makeVisible
import com.google.android.material.slider.RangeSlider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FiltersFragment : BaseFragment<FragmentFiltersBinding>
    (R.layout.fragment_filters, true), FilterListener {

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: FiltersViewModel by viewModels()
    private lateinit var binding: FragmentFiltersBinding

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentFiltersBinding =
        FragmentFiltersBinding::inflate

    private val radioGroupListener = RadioChangeListener()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentFiltersBinding.inflate(inflater, container, false)

        viewModel.action.observe(viewLifecycleOwner) {
            val data = it.getDataSafely() ?: return@observe
            doAction(data)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            setSliderValues()

            //Set max values for sliders
            if (homeViewModel.totalMaxFloor == 0f) {
                floorRangeTV.makeGone()
                floorRangeSlider.makeGone()
            } else {
                floorRangeTV.makeVisible()
                floorRangeSlider.makeVisible()
                floorRangeSlider.valueTo = homeViewModel.totalMaxFloor
            }

            if (homeViewModel.totalMaxRooms == 0f) {
                roomsRangeTV.makeGone()
                roomRangeSlider.makeGone()
            } else {
                roomsRangeTV.makeVisible()
                roomRangeSlider.makeVisible()
                roomRangeSlider.valueTo = homeViewModel.totalMaxRooms
            }

            if (homeViewModel.totalMaxPrice == 0f) {
                priceRangeTV.makeGone()
                priceRangeSlider.makeGone()
            } else {
                priceRangeTV.makeVisible()
                priceRangeSlider.makeVisible()
                priceRangeSlider.valueTo = homeViewModel.totalMaxPrice
            }

            //Set all the parameters from homeviewmodel
            getParametersFromHomeViewModel()
            setFilterValuesOnUI()

            applyButton.setOnClickListener {

                setParametersToHomeViewModel()

                val action = FiltersFragmentDirections.actionFiltersFragmentToHomeFragment()
                action.boolSearch = true
                findNavController().navigate(action)
            }

            aptTypeGroup.setOnCheckedChangeListener(radioGroupListener)
            furnitureGroup.setOnCheckedChangeListener(radioGroupListener)

            rentAll.setOnCheckedChangeListener { compoundButton, isChecked ->
                viewModel.checkedChanged(PRICE_TYPE_ALL)
                if (isChecked) {
                    rentDaily.isChecked = false
                    rentWeekly.isChecked = false
                    rentMonthly.isChecked = false
                }
            }
            rentDaily.setOnCheckedChangeListener { compoundButton, b ->
                viewModel.checkedChanged(PRICE_TYPE_PER_DAY)
                if (b) rentAll.isChecked = false
            }
            rentWeekly.setOnCheckedChangeListener { compoundButton, b ->
                viewModel.checkedChanged(PRICE_TYPE_PER_WEEK)
                if (b) rentAll.isChecked = false
            }
            rentMonthly.setOnCheckedChangeListener { compoundButton, b ->
                viewModel.checkedChanged(PRICE_TYPE_PER_MONTH)
                if (b) rentAll.isChecked = false
            }

            cancelButton.setOnClickListener {
                val action = FiltersFragmentDirections.actionFiltersFragmentToHomeFragment()
                action.boolSearch = false
                findNavController().navigate(action)
            }

            ratingRangeSlider.addOnSliderTouchListener(RangeSliderListener(SLIDER_TYPE_RATING))
            floorRangeSlider.addOnSliderTouchListener(RangeSliderListener(SLIDER_TYPE_FLOOR))
            roomRangeSlider.addOnSliderTouchListener(RangeSliderListener(SLIDER_TYPE_ROOMS))
            priceRangeSlider.addOnSliderTouchListener(RangeSliderListener(SLIDER_TYPE_PRICE))

            clearFiltersButton.setOnClickListener {
                homeViewModel.apply {
                    viewModel.resetFilters(totalMaxFloor, totalMaxRooms, totalMaxPrice)
                }
            }
        }
    }


    private fun setParametersToHomeViewModel() {
        homeViewModel.apply {
            currentSortType = viewModel.sort_type
            currentApt_type = viewModel.apt_type
            currentFurniture_type = viewModel.furniture_type
            currentRent_type = viewModel.rent_type

            currentMinRating = viewModel.minRating
            currentMaxRating = viewModel.maxRating

            currentMinFloor = viewModel.minFloor
            currentMaxFloor = viewModel.maxFloor

            currentMinRooms = viewModel.minRooms
            currentMaxRooms = viewModel.maxRooms

            currentMinPrice = viewModel.minPrice
            currentMaxPrice = viewModel.maxPrice

            Log.d(
                "TAG", "setParametersToHomeViewModel: " +
                        "\ncurrentSortType $currentSortType" +
                        "\ncurrentApt_type $currentApt_type" +
                        "\ncurrentFurniture_type $currentFurniture_type" +
                        "\ncurrentRent_type $currentRent_type" +
                        "\ncurrentMinRating $currentMinRating" +
                        "\ncurrentMaxRating $currentMaxRating" +
                        "\ncurrentMinFloor $currentMinFloor" +
                        "\ncurrentMaxFloor $currentMaxFloor" +
                        "\ncurrentMinRooms $currentMinRooms" +
                        "\ncurrentMaxRooms $currentMaxRooms" +
                        "\ncurrentMinPrice $currentMinPrice" +
                        "\ncurrentMaxPrice $currentMaxPrice"
            )
        }
    }

    private fun getParametersFromHomeViewModel() {
        viewModel.apt_type = homeViewModel.currentApt_type
        viewModel.furniture_type = homeViewModel.currentFurniture_type
        viewModel.rent_type = homeViewModel.currentRent_type
        viewModel.sort_type = homeViewModel.currentSortType

        viewModel.minRating = homeViewModel.currentMinRating
        viewModel.maxRating = homeViewModel.currentMaxRating

        viewModel.minFloor = homeViewModel.currentMinFloor
        viewModel.maxFloor = homeViewModel.currentMaxFloor

        viewModel.minRooms = homeViewModel.currentMinRooms
        viewModel.maxRooms = homeViewModel.currentMaxRooms

        viewModel.minPrice = homeViewModel.currentMinPrice
        viewModel.maxPrice = homeViewModel.currentMaxPrice

        Log.d(
            "TAG", "getParametersFromHomeViewModel: " +
                    "\ncurrentSortType ${viewModel.sort_type}" +
                    "\ncurrentApt_type ${viewModel.apt_type}" +
                    "\ncurrentFurniture_type ${viewModel.furniture_type}" +
                    "\ncurrentRent_type ${viewModel.rent_type}" +
                    "\ncurrentMinRating ${viewModel.minRating}" +
                    "\ncurrentMaxRating ${viewModel.maxRating}" +
                    "\ncurrentMinFloor ${viewModel.minFloor}" +
                    "\ncurrentMaxFloor ${viewModel.maxFloor}" +
                    "\ncurrentMinRooms ${viewModel.minRooms}" +
                    "\ncurrentMaxRooms ${viewModel.maxRooms}" +
                    "\ncurrentMinPrice ${viewModel.minPrice}" +
                    "\ncurrentMaxPrice ${viewModel.maxPrice}"
        )
    }

    private fun setFilterValuesOnUI() {
        binding.apply {

            when (viewModel.apt_type) {
                APT_TYPE_ALL -> aptTypeAll.isChecked = true
                APT_TYPE_FLAT -> aptTypeFlat.isChecked = true
                APT_TYPE_HOUSE -> aptTypeHouse.isChecked = true
            }

            when (viewModel.furniture_type) {
                APT_FURNISHED_ALL -> furnitureAll.isChecked = true
                APT_FURNISHED_YES -> furnitureYes.isChecked = true
                APT_FURNISHED_NO -> furnitureNo.isChecked = true
            }

            for (item in viewModel.rent_type) {
                when (item) {
                    PRICE_TYPE_ALL -> rentAll.isChecked = true
                    PRICE_TYPE_PER_DAY -> {
                        rentAll.isChecked = false
                        rentDaily.isChecked = true
                    }
                    PRICE_TYPE_PER_WEEK -> {
                        rentAll.isChecked = false
                        rentWeekly.isChecked = true
                    }
                    PRICE_TYPE_PER_MONTH -> {
                        rentAll.isChecked = false
                        rentMonthly.isChecked = true
                    }
                }
            }

            setSliderValues()
        }
    }

    private fun setSliderValues() {
        binding.apply {
            val ratings =
                viewModel.getRatings(ratingRangeSlider.valueFrom, ratingRangeSlider.valueTo)
            val floors = viewModel.getFloors(floorRangeSlider.valueFrom, floorRangeSlider.valueTo)
            val rooms = viewModel.getRooms(roomRangeSlider.valueFrom, roomRangeSlider.valueTo)
            val prices = viewModel.getPrice(priceRangeSlider.valueFrom, priceRangeSlider.valueTo)

            ratingRangeSlider.setValues(ratings.first, ratings.second)
            floorRangeSlider.setValues(floors.first, floors.second)
            roomRangeSlider.setValues(rooms.first, rooms.second)
            priceRangeSlider.setValues(prices.first, prices.second)
        }
    }

    override fun sortTypeSelected(sort_type: Int) {
        viewModel.sort_type = sort_type
    }

    inner class RangeSliderListener(private val sliderType: Int) :
        RangeSlider.OnSliderTouchListener {
        override fun onStartTrackingTouch(slider: RangeSlider) {
        }

        override fun onStopTrackingTouch(slider: RangeSlider) {
            val minValue = slider.values[0]
            val maxValue = slider.values[1]

            when (sliderType) {
                SLIDER_TYPE_RATING -> {
                    viewModel.minRating = minValue
                    viewModel.maxRating = maxValue
                }
                SLIDER_TYPE_FLOOR -> {
                    viewModel.minFloor = minValue
                    viewModel.maxFloor = maxValue
                }
                SLIDER_TYPE_ROOMS -> {
                    viewModel.minRooms = minValue
                    viewModel.maxRooms = maxValue
                }
                SLIDER_TYPE_PRICE -> {
                    viewModel.minPrice = minValue
                    viewModel.maxPrice = maxValue
                }
            }
        }


    }

    private fun doAction(a: FiltersViewModel.Action) {
        when (a.type) {
            FiltersViewModel.ActionType.UPDATE_UI -> setFilterValuesOnUI()
        }
    }

    inner class RadioChangeListener : RadioGroup.OnCheckedChangeListener {
        override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {
            when (p1) {
                binding.aptTypeFlat.id -> viewModel.apt_type = APT_TYPE_FLAT
                binding.aptTypeHouse.id -> viewModel.apt_type = APT_TYPE_HOUSE
                binding.aptTypeAll.id -> viewModel.apt_type = APT_TYPE_ALL

                binding.furnitureAll.id -> viewModel.furniture_type = APT_FURNISHED_ALL
                binding.furnitureYes.id -> viewModel.furniture_type = APT_FURNISHED_YES
                binding.furnitureNo.id -> viewModel.furniture_type = APT_FURNISHED_NO
            }
        }

    }
}