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
import com.cloudlevi.ping.ui.apartmentPage.ApartmentPageEvent.*
import com.cloudlevi.ping.*
import com.cloudlevi.ping.R
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
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.di.GlideApp
import com.google.android.gms.maps.GoogleMap


@AndroidEntryPoint
class ApartmentPageFragment :
    BaseFragment<FragmentApartmentPageBinding>
        (R.layout.fragment_apartment_page, true), OnMapReadyCallback {

    private lateinit var binding: FragmentApartmentPageBinding
    private val viewModel: ApartmentPageViewModel by viewModels()
    private lateinit var viewPagerAdapter: ApartmentPageSliderAdapter

    private var googleMap: GoogleMap? = null

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentApartmentPageBinding =
        FragmentApartmentPageBinding::inflate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentApartmentPageBinding.inflate(inflater, container, false)

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

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

            viewModel.apartmentModelLiveData.observe(viewLifecycleOwner) { apartmentModel ->
                applyAllText(apartmentModel)
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

            locationTV.setOnClickListener { showCurrentLocation() }
            bookNowBtn.setOnClickListener { navigateToBooking() }
            rateBtn.setOnClickListener { showRatingDialog() }

            profileImage.setOnClickListener {
                val action = ApartmentPageFragmentDirections
                    .actionApartmentPageFragmentToUserPostsFragment(
                        viewModel.getLandLordModel(),
                        null
                    )
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
            viewModel.currentApartmentModel, viewModel.currentLandLordModel!!
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
            val priceString = apartmentModel.mGetPricingText()
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
            val locationString = apartmentModel.createLatLng().getAddress(requireContext())
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
                binding.rateBtn.visibleOrGone(a.bool ?: false)
            }
            IMAGES_COMPILED -> imagesReceived()
            LANDLORD_INFO_RECEIVED -> landLordInfoReceived(viewModel.currentLandLordModel)
        }
    }

    private fun imagesReceived() {
        viewPagerAdapter.submitList(viewModel.newImageUrlList)

        binding.apply {
            TabLayoutMediator(tabLayout, imageSlider) { tab, position ->
                tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.circle_slider)
            }.attach()
        }
    }

    private fun landLordInfoReceived(landlordModel: User?) {
        val userNameText = "@${landlordModel?.username}"
        binding.landLordName.text = landlordModel?.displayName
        binding.landLordUserName.text = userNameText

        GlideApp.with(requireContext())
            .load(viewModel.getCurrentLandLordImageRef())
            .centerCrop()
            .placeholder(R.drawable.ic_profile_picture)
            .error(R.drawable.ic_profile_picture)
            .into(binding.profileImage)
    }

    private fun showCurrentLocation() {
        val latLng = LatLng(
            viewModel.currentApartmentModel.latitude,
            viewModel.currentApartmentModel.longitude
        )
        googleMap?.apply {
            val marker = setNewMarkerCustom(latLng)
            val title =
                marker?.getAddress(requireContext()) ?: "${latLng.latitude}, ${latLng.longitude}"
            marker?.title = title
            marker?.showInfoWindow()
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.apply {
            mapType = GoogleMap.MAP_TYPE_NORMAL
            showCurrentLocation()
            setOnMarkerClickListener { m ->
                val t =
                    m.getAddress(requireContext()) ?: return@setOnMarkerClickListener false
                m.title = t
                m.showInfoWindow()
                true
            }
        }
        googleMap?.uiSettings?.apply {
            isCompassEnabled = true
            isZoomControlsEnabled = true
            isRotateGesturesEnabled = true
            isScrollGesturesEnabledDuringRotateOrZoom = true
            setAllGesturesEnabled(true)
        }
    }


    override fun onResume() {
        if (this::binding.isInitialized)
            binding.mapView.onResume()
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (this::binding.isInitialized)
            binding.mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::binding.isInitialized)
            binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (this::binding.isInitialized)
            binding.mapView.onLowMemory()
    }
}