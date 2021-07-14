package com.cloudlevi.ping.ui.apartmentPage

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.cloudlevi.ping.ui.apartmentPage.ApartmentPageEvent.*
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.databinding.FragmentApartmentPageBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ApartmentPageFragment: Fragment(R.layout.fragment_apartment_page) {

    private lateinit var binding: FragmentApartmentPageBinding
    private val viewModel: ApartmentPageViewModel by viewModels()
    private lateinit var viewPagerAdapter: ApartmentPageSliderAdapter

    private var emptyTouchListener = EmptyTouchListener()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentApartmentPageBinding.bind(view)

        binding.apply {

            //Disable rating bar
            ratingBar.setIsIndicator(true)

            viewPagerAdapter = ApartmentPageSliderAdapter(this@ApartmentPageFragment, hashMapOf<Int, String>())
            imageSlider.adapter = viewPagerAdapter

            if(arguments != null) {
                viewModel.onFragmentCreated(ApartmentPageFragmentArgs.fromBundle(requireArguments()).apartmentID)
                if (ApartmentPageFragmentArgs.fromBundle(requireArguments()).fromUserLists){
                    landLordName.visibility = View.GONE
                    landLordUserName.visibility = View.GONE
                    profileImage.visibility = View.GONE
                }
            }

            viewModel.imageUrlListLiveData.observe(viewLifecycleOwner){ imageList ->
                //viewPagerAdapter = ApartmentPageSliderAdapter(imageList, requireContext())
                viewPagerAdapter.submitList(imageList)

                TabLayoutMediator(tabLayout, imageSlider){ tab, position ->
                    tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.circle_slider)
                }.attach()
            }

            viewModel.apartmentModelLiveData.observe(viewLifecycleOwner){ apartmentModel ->
                applyAllText(apartmentModel)
            }

            viewModel.currentLandLordLiveData.observe(viewLifecycleOwner){ currentLandLord ->
                val userNameText = "@${currentLandLord?.username}"
                binding.landLordName.text = currentLandLord.displayName
                binding.landLordUserName.text = userNameText

                Glide.with(requireContext())
                    .load(currentLandLord.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.progress_animation_small)
                    .into(profileImage)
            }

            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                viewModel.apartmentPageEvent.collect { event ->
                    when(event){
                        is SendToastMessage -> sendToastMessage(event.message)
                        is ChangeProgressStatus -> changeProgressStatus(event.status)
                        is ChangeRatingClickable -> ratingBar.setIsIndicator(!event.status)
                    }
                }
            }

            ratingBar.setOnRatingBarChangeListener { ratingBar, float, fromUser ->
                if (fromUser)
                    viewModel.ratingChanged(float)
            }

            profileImage.setOnClickListener {
                val action = ApartmentPageFragmentDirections
                    .actionApartmentPageFragmentToUserPostsFragment(viewModel.getUserModel())
                findNavController().navigate(action)
            }

        }
    }

    private fun sendToastMessage(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

    private fun changeProgressStatus(status: Int){
        binding.apply {
            progressBar.visibility = status

            when(status){
                View.VISIBLE -> mainConstraintLayout.foreground = ContextCompat.getDrawable(requireContext(), R.color.black_transparent)
                View.GONE -> mainConstraintLayout.foreground = null
            }
        }
    }

    private fun applyAllText(apartmentModel: ApartmentHomePost){
        binding.apply {
            val priceString = "${apartmentModel.price}$"
            val paymentTypeString = "Payment type: " + if (apartmentModel.priceType == PRICE_TYPE_PER_DAY) "Daily"
            else if (apartmentModel.priceType == PRICE_TYPE_PER_WEEK) "Weekly"
            else "Monthly"

            val furnishingString = if(apartmentModel.isFurnished) "Furnishing: Yes" else "Furnishing: No"
            val floorString = "Floor: ${apartmentModel.aptFloor}"
            val roomString = "Rooms: ${apartmentModel.roomAmount}"
            val aptTypeString = if(apartmentModel.aptType == APT_TYPE_HOUSE) "Apartment type: House" else "Apartment type: Flat"
            val acreageString = "Acreage: ${apartmentModel.acreage}"
            val locationString = "${apartmentModel.address}, ${apartmentModel.city}"
            timeTextView.text = convertTime(apartmentModel.timeStamp)
            titleTextView.text = apartmentModel.title
            priceTextView.text = priceString
            priceTypeTV.text = paymentTypeString
            furnishedTV.text = furnishingString
            floorTV.text = floorString
            roomTV.text = roomString
            aptTypeTV.text = aptTypeString
            acreageTV.text = acreageString
            descriptionTV.text = apartmentModel.description
            locationTV.text = locationString

            ratingBar.rating = apartmentModel.rating
            if(apartmentModel.rating != 0F){
                val df = DecimalFormat("#.#")
                val ratingString = "Average rating of ${df.format(apartmentModel.rating)} from ${apartmentModel.ratingQuantity} reviews."
                ratingText.visibility = View.VISIBLE
                ratingText.text = ratingString
            }
        }
    }

    private fun convertTime(timeStamp: Long): String{
        val simpleDateFormat = SimpleDateFormat("dd MMM yyyy HH:mm")
        return simpleDateFormat.format(timeStamp)
    }

    inner class EmptyTouchListener: View.OnTouchListener{
        override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
            return true
        }

    }
}