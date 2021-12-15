package com.cloudlevi.ping.ui.apartmentPage

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.cloudlevi.ping.R
import com.cloudlevi.ping.databinding.ApartmentPageSliderItemBinding
import com.google.android.material.shape.CornerFamily

class ApartmentPageSliderItem: Fragment(R.layout.apartment_page_slider_item) {

    private lateinit var binding: ApartmentPageSliderItemBinding

    companion object {
        const val BUNDLE_POSITION = "position"
        const val BUNDLE_IMAGE_URL= "image_url"

        fun getInstance(position: Int, imageUrl: Uri): Fragment {
            val apartmentPageSliderItem = ApartmentPageSliderItem()
            val bundle = Bundle()
            bundle.putInt(BUNDLE_POSITION, position)
            bundle.putString(BUNDLE_IMAGE_URL, imageUrl.toString())
            apartmentPageSliderItem.arguments = bundle

            return apartmentPageSliderItem
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = ApartmentPageSliderItemBinding.bind(view)

        val position = requireArguments().getInt(BUNDLE_POSITION, 0)
        val imageUrl = requireArguments().getString(BUNDLE_IMAGE_URL)

        binding.apply {
            Glide.with(view)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.progress_animation_small)
                .into(sliderImageView)

                sliderImageView.shapeAppearanceModel = sliderImageView.shapeAppearanceModel.toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, 60f)
                .build()
        }
    }
}