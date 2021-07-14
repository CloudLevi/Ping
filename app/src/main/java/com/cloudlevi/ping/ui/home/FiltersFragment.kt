package com.cloudlevi.ping.ui.home

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudlevi.ping.*
import com.cloudlevi.ping.databinding.FragmentFiltersBinding
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import com.cloudlevi.ping.ui.home.FiltersFragmentEvent.*

@AndroidEntryPoint
class FiltersFragment: Fragment(R.layout.fragment_filters), FilterListener {

    private val homeViewModel: HomeFragmentViewModel by activityViewModels()
    private val viewModel: FiltersViewModel by viewModels()
    private lateinit var binding: FragmentFiltersBinding
    private lateinit var adapter: FilterSortAdapter

    private val radioGroupListener = RadioChangeListener()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFiltersBinding.bind(view)

        binding.apply{

            setSliderValues()

            //Set max values for sliders
            floorRangeSlider.valueTo = homeViewModel.totalMaxFloor
            roomRangeSlider.valueTo = homeViewModel.totalMaxRooms
            priceRangeSlider.valueTo = homeViewModel.totalMaxPrice

            //Set all the parameters from homeviewmodel
            getParametersFromHomeViewModel()
            setFilterValuesOnUI()

            adapter = FilterSortAdapter(arrayListOf("Cheaper first", "More expensive first", "Newest first", "Higher rated first"), this@FiltersFragment, viewModel.sort_type)
            sortRecycler.adapter = adapter
            sortRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            sortRecycler.setHasFixedSize(true)

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
                if (isChecked){
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

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.filtersFragmentEvent.collect { event ->
                when(event){
                    UpdateUI -> setFilterValuesOnUI()
                }
            }
        }
    }


    private fun setParametersToHomeViewModel(){
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
        }
    }
    private fun getParametersFromHomeViewModel(){
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
    }
    private fun setFilterValuesOnUI(){
        binding.apply{

            adapter = FilterSortAdapter(arrayListOf("Cheaper first", "More expensive first", "Newest first", "Higher rated first"), this@FiltersFragment, viewModel.sort_type)
            adapter.notifyDataSetChanged()

            when(viewModel.apt_type){
                APT_TYPE_ALL -> aptTypeAll.isChecked = true
                APT_TYPE_FLAT -> aptTypeFlat.isChecked = true
                APT_TYPE_HOUSE -> aptTypeHouse.isChecked = true
            }

            when(viewModel.furniture_type){
                APT_FURNISHED_ALL -> furnitureAll.isChecked = true
                APT_FURNISHED_YES -> furnitureYes.isChecked = true
                APT_FURNISHED_NO -> furnitureNo.isChecked = true
            }

            for (item in viewModel.rent_type){
                when(item){
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
    private fun setSliderValues(){
        binding.apply {
            ratingRangeSlider.setValues(viewModel.minRating, viewModel.maxRating)
            floorRangeSlider.setValues(viewModel.minFloor, viewModel.maxFloor)
            roomRangeSlider.setValues(viewModel.minRooms, viewModel.maxRooms)
            priceRangeSlider.setValues(viewModel.minPrice, viewModel.maxPrice)
        }
    }

    override fun sortTypeSelected(sort_type: Int) {
        viewModel.sort_type = sort_type
    }

    inner class RangeSliderListener(private val sliderType: Int): RangeSlider.OnSliderTouchListener{
        override fun onStartTrackingTouch(slider: RangeSlider) {
        }

        override fun onStopTrackingTouch(slider: RangeSlider) {
            val minValue = slider.values[0]
            val maxValue = slider.values[1]

            when(sliderType){
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
    inner class RadioChangeListener: RadioGroup.OnCheckedChangeListener{
        override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {
            when(p1){
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