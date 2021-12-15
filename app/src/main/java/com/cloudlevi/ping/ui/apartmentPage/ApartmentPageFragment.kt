package com.cloudlevi.ping.ui.apartmentPage

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.cloudlevi.ping.ui.apartmentPage.ApartmentPageEvent.*
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.databinding.FragmentApartmentPageBinding
import com.cloudlevi.ping.ext.*
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import com.cloudlevi.ping.ui.apartmentPage.ApartmentPageViewModel.*
import com.cloudlevi.ping.ui.apartmentPage.ApartmentPageViewModel.ActionType.*

@AndroidEntryPoint
class ApartmentPageFragment :
    BaseFragment<FragmentApartmentPageBinding>
        (R.layout.fragment_apartment_page, true) {

    private lateinit var binding: FragmentApartmentPageBinding
    private val viewModel: ApartmentPageViewModel by viewModels()
    private lateinit var viewPagerAdapter: ApartmentPageSliderAdapter

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentApartmentPageBinding =
        FragmentApartmentPageBinding::inflate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentApartmentPageBinding.inflate(inflater, container, false)

        viewModel.action.observe(viewLifecycleOwner) {
            val data = it.getDataSafely() ?: return@observe
            doAction(data)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            viewPagerAdapter =
                ApartmentPageSliderAdapter(this@ApartmentPageFragment, hashMapOf())
            imageSlider.adapter = viewPagerAdapter

            if (arguments != null) {
                viewModel.onFragmentCreated(ApartmentPageFragmentArgs.fromBundle(requireArguments()).apartmentHomePost)
                if (ApartmentPageFragmentArgs.fromBundle(requireArguments()).fromUserLists) {
                    landLordName.visibility = View.GONE
                    landLordUserName.visibility = View.GONE
                    profileImage.visibility = View.GONE
                }
            }

            reviewRecycler.adapter = viewModel.reviewAdapter

            viewModel.imageUrlListLiveData.observe(viewLifecycleOwner) { imageList ->

                viewPagerAdapter.submitList(imageList)

                TabLayoutMediator(tabLayout, imageSlider) { tab, position ->
                    tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.circle_slider)
                }.attach()
            }

            viewModel.apartmentModelLiveData.observe(viewLifecycleOwner) { apartmentModel ->
                applyAllText(apartmentModel)
            }

            viewModel.currentLandLordLiveData.observe(viewLifecycleOwner) { currentLandLord ->
                val userNameText = "@${currentLandLord?.username}"
                binding.landLordName.text = currentLandLord?.displayName
                binding.landLordUserName.text = userNameText

                Glide.with(requireContext())
                    .load(currentLandLord?.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_profile_picture)
                    .into(profileImage)
            }

            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                viewModel.apartmentPageEvent.collect { event ->
                    when (event) {
                        is SendToastMessage -> sendToastMessage(event.message)
                        is ChangeProgressStatus -> changeProgressStatus(
                            event.status,
                            event.checkRating
                        )
                        is ChangeRatingClickable -> {
                            //ratingBar.setIsIndicator(!event.status)
                        }
                        is ToggleBookVisibility -> bookNowBtn.visibleOrGone(event.isVisible)
                    }
                }
            }

            bookNowBtn.setOnClickListener { navigateToBooking() }

            rateBtn.setOnClickListener { showRatingDialog() }

            profileImage.setOnClickListener {
                val action = ApartmentPageFragmentDirections
                    .actionApartmentPageFragmentToUserPostsFragment(viewModel.getUserModel(), null)
                findNavController().navigate(action)
            }

        }
    }

    private fun navigateToUser(userID: String) {
        val action = ApartmentPageFragmentDirections
            .actionApartmentPageFragmentToUserPostsFragment(null, userID)
        findNavController().navigate(action)
    }

    private fun navigateToBooking() {
        val action = ApartmentPageFragmentDirections.actionApartmentPageFragmentToBookingFragment(
            viewModel.currentApartmentModel, viewModel.currentLandLordLiveData.value!!
        )
        findNavController().navigate(action)
    }

    private fun sendToastMessage(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

    private fun changeProgressStatus(status: Int, checkRating: Boolean) {
        if (checkRating) checkRatingVisibility()

        val isEnabled = status == View.GONE
        binding.apply {
            progressBar.visibility = status

            toggleAllViewsEnabled(isEnabled, binding.root)

            when (status) {
                View.VISIBLE -> mainConstraintLayout.foreground =
                    ContextCompat.getDrawable(requireContext(), R.color.black_transparent)
                View.GONE -> mainConstraintLayout.foreground = null
            }
        }
    }

    private fun checkRatingVisibility() {
        binding.apply {
            if (!viewModel.isRatingVisible()) rateBtn.makeGone()
        }
    }

    private fun applyAllText(apartmentModel: ApartmentHomePost) {
        binding.apply {
            val priceString = apartmentModel.getPricingText()
            val paymentTypeString =
                getString(R.string.payment_type_) + when (apartmentModel.priceType) {
                    PRICE_TYPE_PER_DAY -> getString(R.string.daily)
                    PRICE_TYPE_PER_WEEK -> getString(R.string.weekly)
                    else -> getString(R.string.monthly)
                }

            val furnishingString =
                if (apartmentModel.isFurnished) getString(R.string.furnishing_yes)
                else getString(R.string.furnishing_no)
            val floorString = getString(R.string.floor_, apartmentModel.aptFloor.toString())
            val roomString = getString(R.string.rooms_, apartmentModel.roomAmount.toString())
            val aptTypeString =
                if (apartmentModel.aptType == APT_TYPE_HOUSE) getString(R.string.apt_type_house)
                else getString(R.string.apt_type_flat)
            val acreageString = getString(R.string.acreage_, apartmentModel.acreage.toString())
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

            applyRating(apartmentModel.calculateAverageRating(), apartmentModel.reviewsCount())
        }
    }

    private fun doAction(a: Action) {
        when (a.type) {
            UPDATE_RATING -> applyRating(a.avg ?: 0.0, a.count ?: 0)
            UPDATE_RATING_IMAGE -> updateRatingImage(a.string, a.pos ?: 0)
            TOGGLE_REVIEWS_VISIBILITY -> toggleReviewsVisibility(a.bool ?: false)
            CURRENT_REVIEW_CLICK -> showRatingDialog()
            OTHER_REVIEW_CLICK -> navigateToUser(a.string ?: "")
            RATING_VISIBILITY -> {
                Log.d("DEBUG", "received rating visibility: ${a.bool}")
                binding.rateBtn.visibleOrGone(a.bool?: false)
            }
        }
    }

    private fun toggleReviewsVisibility(isVisible: Boolean) {
        binding.reviewRecycler.visibleOrGone(isVisible)
        binding.reviewsTV.visibleOrGone(isVisible)
    }

    private fun updateRatingImage(url: String?, pos: Int) {
        val holder =
            binding.reviewRecycler.findViewHolderForAdapterPosition(pos) as? ReviewAdapter.ReviewVH
                ?: return

        holder.updateImage(url)
    }

    private fun showRatingDialog() {
        showRatingDialog(requireContext(),
            viewModel.getCurrentUserRating(),
            viewModel.getCurrentUserComment(),
            object : RatingDialogListener {
                override fun onPositiveClick(rating: Float, experience: String) {
                    viewModel.ratingChanged(rating, experience)
                }

                override fun onNegativeClick() {
                    viewModel.deleteReview()
                }
            })
    }

    private fun applyRating(average: Double, reviewCount: Int) {
        binding.apply {
            if (reviewCount != 0) {
                var avgString = DecimalFormat("#.#").format(average)
                if (avgString.length == 1) avgString = "$avgString.0"

                val ratingString = SpannableString(
                    getString(
                        R.string.avg_from_reviews,
                        avgString,
                        reviewCount.toString()
                    )
                ).also {
                    it.setSpan(
                        StyleSpan(Typeface.BOLD),
                        0,
                        avgString.length,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                ratingTV.makeVisible()
                ratingTV.text = ratingString
            } else {
                ratingTV.makeGone()
            }
        }
    }

    private fun convertTime(timeStamp: Long): String {
        val simpleDateFormat = SimpleDateFormat("dd MMM yyyy HH:mm")
        return simpleDateFormat.format(timeStamp)
    }
}