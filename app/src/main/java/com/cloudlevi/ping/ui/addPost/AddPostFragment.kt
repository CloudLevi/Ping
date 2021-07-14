package com.cloudlevi.ping.ui.addPost

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.AddImageModel
import com.cloudlevi.ping.databinding.FragmentAddPostBinding
import com.cloudlevi.ping.ui.addPost.AddPostFragmentEvent.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.io.ByteArrayOutputStream


@AndroidEntryPoint
class AddPostFragment : Fragment(R.layout.fragment_add_post), AddImageAdapter.OnCLickedListener {

    private val viewModel: AddPostFragmentViewModel by activityViewModels()
    private lateinit var binding: FragmentAddPostBinding
    private lateinit var imageResultLauncher: ActivityResultLauncher<String>
    private lateinit var addImageAdapter: AddImageAdapter
    private lateinit var byteArrayData: ByteArray

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddPostBinding.bind(view)

        addImageAdapter = AddImageAdapter(this)

        binding.fragmentAddPostScrollView.post(Runnable {
            binding.fragmentAddPostScrollView.scrollTo(0, viewModel.scrollPositionY)
        })

        switchAptTypeColors(viewModel.aptTypeValue)
        switchPriceTypeColors(viewModel.priceType)

        imageResultLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

                Glide.with(this)
                    .asBitmap()
                    .load(uri)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            val baos = ByteArrayOutputStream()
                            resource.compress(Bitmap.CompressFormat.JPEG, 25, baos)
                            byteArrayData = baos.toByteArray()
                            viewModel.handleFinishedImageIntent(uri, byteArrayData)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            Log.d(TAG, "Load Cleared")
                        }
                    })
            }

        binding.apply {

            titleEditText.apply {
                addTextChangedListener {
                    viewModel.title = it.toString().trim()
                    val characterCount = "${it?.length ?: 0}/70"
                    titleCharacterCount.text = characterCount
                }
                setText(viewModel.title)
            }

            floorCustomPicker.apply {
                buttonMinus.setOnClickListener {
                    viewModel.pickerButtonClicked(CLICK_TYPE_MINUS, PICKER_TYPE_FLOOR)
                }
                buttonPlus.setOnClickListener {
                    viewModel.pickerButtonClicked(CLICK_TYPE_PLUS, PICKER_TYPE_FLOOR)
                }
                amountTV.text = viewModel.floorValue.toString()
            }
            roomCustomPicker.apply {
                buttonMinus.setOnClickListener {
                    viewModel.pickerButtonClicked(CLICK_TYPE_MINUS, PICKER_TYPE_ROOMS)
                }
                buttonPlus.setOnClickListener {
                    viewModel.pickerButtonClicked(CLICK_TYPE_PLUS, PICKER_TYPE_ROOMS)
                }
                amountTV.text = viewModel.roomAmount.toString()
            }

            acreageEditText.setText(checkIfDoubleIsZero(viewModel.acreage))
            acreageEditText.addTextChangedListener {
                if (it.toString().trim().isNotEmpty()) {
                    viewModel.acreage = it.toString().toDouble()
                } else {
                    viewModel.acreage = 0.0
                }
            }

            cityEditText.setText(viewModel.city)
            cityEditText.addTextChangedListener {
                viewModel.city = it.toString().trim()
            }

            addressEditText.addTextChangedListener {
                viewModel.address = it.toString().trim()
            }
            addressEditText.setText(viewModel.address)

            descriptionEditText.addTextChangedListener {
                viewModel.description = it.toString().trim()
            }
            descriptionEditText.setText(viewModel.description)

            priceEditText.setText(checkIfIntIsZero(viewModel.price))
            priceEditText.addTextChangedListener {
                if (it.toString().trim().isNotEmpty()) {
                    viewModel.price = it.toString().toInt()
                } else {
                    viewModel.price = 0
                }
            }

            aptTypeChoiceFlatButton.setOnClickListener {
                viewModel.onFlatButtonClicked()
            }
            aptTypeChoiceHouseButton.setOnClickListener {
                viewModel.onHouseButtonClicked()
            }
            furnishingBTN.setOnClickListener {
                viewModel.furnishingBTNCLicked()
            }
            priceTypeDayBTN.setOnClickListener {
                viewModel.priceTypeClicked(PRICE_TYPE_PER_DAY)
            }
            priceTypeWeekBTN.setOnClickListener {
                viewModel.priceTypeClicked(PRICE_TYPE_PER_WEEK)
            }
            priceTypeMonthBTN.setOnClickListener {
                viewModel.priceTypeClicked(PRICE_TYPE_PER_MONTH)
            }

            uploadButton.setOnClickListener {
                viewModel.onUploadButtonClicked()
            }

            if (!viewModel.isImagesArrayInitialized) {
                viewModel.imagesArray = arrayListOf(
                    AddImageModel(0),
                    AddImageModel(1),
                    AddImageModel(2),
                    AddImageModel(3),
                    AddImageModel(4)
                )
                viewModel.isImagesArrayInitialized = true
            } else {
                viewModel.insertPreviousImages()
            }

            imagesAddRecyclerView.apply {
                adapter = addImageAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
            }

            viewModel.imagesArrayLiveData.observe(viewLifecycleOwner) {
                addImageAdapter.submitList(it)
                addImageAdapter.notifyDataSetChanged()
            }

            viewModel.progressTextLiveData.observe(viewLifecycleOwner){
                progressTV.text = it
            }

        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.addPostFragmentEvent.collect { event ->
                when (event) {
                    is AptTypeChanged -> switchAptTypeColors(event.typeValue)
                    is FurnishedValueChanged -> switchFurnishedBTNColor(event.furnishedValue)
                    is PriceTypeChange -> switchPriceTypeColors(event.priceType)
                    is UpdateAdapterValues -> {
                        addImageAdapter.submitList(viewModel.imagesArray)
                        addImageAdapter.notifyDataSetChanged()
                    }
                    is ChangeProgressbarStatus -> changeProgressStatus(event.status)
                    is SendToastMessage -> Toast.makeText(
                        requireContext(),
                        event.message,
                        Toast.LENGTH_LONG
                    ).show()
                    is UpdatePickerTV -> updatePickerTV(event.pickerFloor, event.pickerRooms)
                }
            }
        }
    }

    private fun switchAptTypeColors(aptType: Int) {
        when (aptType) {
            APT_TYPE_FLAT -> {
                binding.aptTypeChoiceFlatButton
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.buttonColorActive
                        )
                    )
                binding.aptTypeChoiceHouseButton
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.buttonColorInactive
                        )
                    )
                binding.floorPickerLayout
                    .visibility = View.VISIBLE
            }
            APT_TYPE_HOUSE -> {
                binding.aptTypeChoiceFlatButton
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.buttonColorInactive
                        )
                    )
                binding.aptTypeChoiceHouseButton
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.buttonColorActive
                        )
                    )
                binding.floorPickerLayout
                    .visibility = View.GONE
            }
        }
    }

    private fun switchPriceTypeColors(priceType: Int) {
        when (priceType) {
            PRICE_TYPE_PER_DAY -> {
                changeButtonColor(binding.priceTypeDayBTN, R.color.buttonColorActive)
                changeButtonColor(binding.priceTypeWeekBTN, R.color.buttonColorInactive)
                changeButtonColor(binding.priceTypeMonthBTN, R.color.buttonColorInactive)
            }
            PRICE_TYPE_PER_WEEK -> {
                changeButtonColor(binding.priceTypeDayBTN, R.color.buttonColorInactive)
                changeButtonColor(binding.priceTypeWeekBTN, R.color.buttonColorActive)
                changeButtonColor(binding.priceTypeMonthBTN, R.color.buttonColorInactive)
            }
            PRICE_TYPE_PER_MONTH -> {
                changeButtonColor(binding.priceTypeDayBTN, R.color.buttonColorInactive)
                changeButtonColor(binding.priceTypeWeekBTN, R.color.buttonColorInactive)
                changeButtonColor(binding.priceTypeMonthBTN, R.color.buttonColorActive)
            }
        }
    }

    private fun switchFurnishedBTNColor(furnishedValue: Boolean) {
        when (furnishedValue) {
            false -> {
                binding.furnishingBTN
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.buttonColorInactive
                        )
                    )
                binding.furnishingBTN
                    .text = "No"
            }
            true -> {
                binding.furnishingBTN
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.buttonColorActive
                        )
                    )
                binding.furnishingBTN
                    .text = "Yes"
            }
        }
    }

    private fun changeButtonColor(button: Button, color: Int) {
        button.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun updatePickerTV(floorTV: String, roomsTV: String) {
        binding.apply {
            floorCustomPicker.amountTV.text = floorTV
            roomCustomPicker.amountTV.text = roomsTV
        }
    }

    private fun changeProgressStatus(status: Int) {
        binding.addFragmentProgressBar.visibility = status
        binding.progressTV.visibility = status
        var childStatus = false;
        when (status) {
            View.VISIBLE -> {
                binding.mainRelativeLayout.foreground =
                    ContextCompat.getDrawable(requireContext(), R.color.black_transparent)
                childStatus = false
            }
            View.GONE -> {
                binding.mainRelativeLayout.foreground = null
                childStatus = true
            }
        }

        changeAllViewsClickability(childStatus)
    }


    private fun changeAllViewsClickability(status: Boolean) {
        binding.apply {
            for (childView in mainRelativeLayout.children) {
                when (childView) {
                    is LinearLayout -> changeViewGroupClickability(childView, status)
                    is FrameLayout -> changeViewGroupClickability(childView, status)
                    is Button -> childView.isEnabled = status
                }
                viewModel.switchRecyclerViewChildrenStatus(status)

                titleLayout.isEnabled = status
                acreageLayout.isEnabled = status
                cityLayout.isEnabled = status
                addressLayout.isEnabled = status
                descriptionLayout.isEnabled = status
                priceInputLayout.isEnabled = status
            }
        }
    }

    private fun changeViewGroupClickability(parent: ViewGroup, status: Boolean) {
        binding.apply {
            for (childView in parent.children) {
                childView.isEnabled = status
            }
        }
    }

    private fun checkIfDoubleIsZero(value: Double): String {
        return if (value == 0.0) ""
        else value.toString()
    }

    private fun checkIfIntIsZero(value: Int): String {
        return if (value == 0) ""
        else value.toString()
    }

    private fun startActivityForImageResult(button: Int) {
        viewModel.latestClickedImageButton = button
        imageResultLauncher.launch("image/*")
    }

    override fun onAddButtonClicked(addImageModel: AddImageModel) {
        startActivityForImageResult(addImageModel.viewID)
    }

    override fun onRemoveButtonClicked(addImageModel: AddImageModel) {
        viewModel.onImageRemoveButtonCLicked(addImageModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.scrollPositionY = binding.fragmentAddPostScrollView.scrollY
    }

}