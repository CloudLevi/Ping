package com.cloudlevi.ping.ui.addPost

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.AddImageModel
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.databinding.FragmentAddPostBinding
import com.cloudlevi.ping.ext.*
import com.cloudlevi.ping.ui.addPost.AddPostFragmentEvent.*
import com.cloudlevi.ping.ui.addPost.AddPostFragmentViewModel.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import java.io.ByteArrayOutputStream
import java.util.*
import com.cloudlevi.ping.ui.addPost.AddPostFragmentViewModel.AddPostEvent.*
import com.cloudlevi.ping.ui.addPost.AddPostFragmentViewModel.AddPostAction.*

@AndroidEntryPoint
class AddPostFragment :
    BaseFragment<FragmentAddPostBinding>(R.layout.fragment_add_post, true),
    AddImageAdapter.OnCLickedListener {

    private val viewModel: AddPostFragmentViewModel by activityViewModels()
    private lateinit var binding: FragmentAddPostBinding
    private lateinit var imageResultLauncher: ActivityResultLauncher<String>
    private lateinit var addImageAdapter: AddImageAdapter
    private lateinit var byteArrayData: ByteArray
    private lateinit var geocoder: Geocoder

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentAddPostBinding =
        FragmentAddPostBinding::inflate

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding.fragmentAddPostScrollView.post {
            binding.fragmentAddPostScrollView.scrollTo(0, viewModel.scrollPositionY)
        }
        populateFromViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = FragmentAddPostBinding.inflate(inflater, container, false)
        geocoder = Geocoder(requireContext(), Locale.getDefault())

        viewModel.init()
        viewModel.action.observe(viewLifecycleOwner) {
            val data = it.getDataSafely() ?: return@observe
            doAction(data)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        addImageAdapter = AddImageAdapter(this, viewModel)

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

            childFragmentManager.setFragmentResultListener(
                MapDialogFragment.APPLY,
                this@AddPostFragment
            ) { _, bundle ->
                val latLng = bundle.get(MapDialogFragment.LAT_LONG) as? LatLng
                    ?: return@setFragmentResultListener
                viewModel.latLng = latLng
                viewModel.countryCode = latLng.countryCode(gCoder = geocoder)

                locationTV.text = latLng.getAddress(gCoder = geocoder)
                locationError.makeGone()
            }

            titleEditText.apply {
                addTextChangedListener {
                    viewModel.title = it.toString().trim()
                    val characterCount = "${it?.length ?: 0}/70"
                    titleCharacterCount.text = characterCount
                    titleError.makeGone()
                }
            }

            fragmentAddPostScrollView.setOnScrollChangeListener { view, x, y, i3, i4 ->
                viewModel.scrollPositionY = y
            }

            floorCustomPicker.apply {
                buttonMinus.setOnClickListener {
                    viewModel.pickerButtonClicked(CLICK_TYPE_MINUS, PICKER_TYPE_FLOOR)
                    floorError.makeGone()
                }
                buttonPlus.setOnClickListener {
                    viewModel.pickerButtonClicked(CLICK_TYPE_PLUS, PICKER_TYPE_FLOOR)
                    floorError.makeGone()
                }
            }
            roomCustomPicker.apply {
                buttonMinus.setOnClickListener {
                    viewModel.pickerButtonClicked(CLICK_TYPE_MINUS, PICKER_TYPE_ROOMS)
                    roomError.makeGone()
                }
                buttonPlus.setOnClickListener {
                    viewModel.pickerButtonClicked(CLICK_TYPE_PLUS, PICKER_TYPE_ROOMS)
                    roomError.makeGone()
                }
            }

            acreageEditText.addTextChangedListener {
                val string = it.toString().trimDigits()
                if (string.isNotEmpty()) {
                    viewModel.acreage = string.toDouble()
                } else {
                    viewModel.acreage = 0.0
                }
                acreageError.makeGone()
            }

            descriptionEditText.addTextChangedListener {
                viewModel.description = it.toString().trim()
                descriptionError.makeGone()
            }

            priceEditText.addTextChangedListener {
                val string = it.toString().trimDigitsInt()
                if (string.isNotEmpty()) {
                    viewModel.price = string.toInt()
                } else {
                    viewModel.price = 0
                }
                priceError.makeGone()
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

            locationLayout.setOnClickListener {
                onSelectLocationClick()
            }

            imagesAddRecyclerView.apply {
                adapter = addImageAdapter
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                setHasFixedSize(true)
            }

            viewModel.progressTextLiveData.observe(viewLifecycleOwner) {
                changeLoadingText(it)
            }

            addImageAdapter.update()
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.addPostFragmentEvent.collect { event ->
                when (event) {
                    is AptTypeChanged -> switchAptTypeColors(event.typeValue)
                    is FurnishedValueChanged -> switchFurnishedBTNColor(event.furnishedValue)
                    is PriceTypeChange -> switchPriceTypeColors(event.priceType)
                    is UpdateAdapterValues -> {
                        addImageAdapter.update()
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

    private fun onSelectLocationClick() {
        val dialog = MapDialogFragment.createFragment(viewModel.latLng)
        dialog.show(childFragmentManager, MapDialogFragment.DIALOG_TAG)
    }

    private fun doAction(e: AddPostEvent) {
        binding.apply {
            when (e.actionType) {
                ADAPTER_CHANGED -> {
                    if (viewModel.getAmountOfFilledImages() > 0) imagesError.makeGone()
                    addImageAdapter.update()
                }
                TITLE_ERROR -> titleError.visibleOrGone(e.bool ?: false)
                ROOM_ERROR -> roomError.visibleOrGone(e.bool ?: false)
                ACREAGE_ERROR -> acreageError.visibleOrGone(e.bool ?: false)
                LAT_LNG_ERROR -> locationError.visibleOrGone(e.bool ?: false)
                DESCRIPTION_ERROR -> descriptionError.visibleOrGone(e.bool ?: false)
                PRICE_ERROR -> priceError.visibleOrGone(e.bool ?: false)
                FLOOR_ERROR -> floorError.visibleOrGone(e.bool ?: false)
                IMAGES_ERROR -> imagesError.visibleOrGone(e.bool ?: false)
                NAVIGATE_TO_POST -> navigateToPost(e.homePost)
                POPULATE_FIELDS -> populateFromViewModel()
                NETWORK_ERROR -> sendLongToast(R.string.network_error_message)
                else -> {}
            }
        }
    }

    private fun populateFromViewModel() {
        switchAptTypeColors(viewModel.aptTypeValue)
        switchPriceTypeColors(viewModel.priceType)

        binding.apply {
            titleEditText.setText(viewModel.title)
            floorCustomPicker.amountTV.text = viewModel.floorValue.toString()
            roomCustomPicker.amountTV.text = viewModel.roomAmount.toString()
            if (viewModel.latLng != null)
                locationTV.text = viewModel.latLng?.getAddress(gCoder = geocoder)
            acreageEditText.setText(checkIfDoubleIsZero(viewModel.acreage))
            descriptionEditText.setText(viewModel.description)
            priceEditText.setText(checkIfIntIsZero(viewModel.price))
            if (!viewModel.isImagesArrayInitialized) {
                viewModel.imagesArray.clear()
                for (a in 0..4) {
                    viewModel.imagesArray.add(AddImageModel(a))
                }
                viewModel.isImagesArrayInitialized = true
            } else {
                viewModel.insertPreviousImages()
            }
            addImageAdapter.update()
        }
    }

    private fun navigateToPost(post: ApartmentHomePost?) {
        post ?: return
        val action = AddPostFragmentDirections.addPostToApartmentPage(post)
        findNavController().navigate(action)
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
                    .text = getString(R.string.no)
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
                    .text = getString(R.string.yes)
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
        val isLoading = status == View.VISIBLE
        switchLoading(isLoading)
        toggleAllViewsEnabled(!isLoading, binding.fragmentAddPostScrollView)
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
        viewModel.onImageRemoveButtonClicked(addImageModel)
    }

}