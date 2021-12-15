package com.cloudlevi.ping.ui.addPost

import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.AddImageModel
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.ui.addPost.AddPostFragmentEvent.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.cloudlevi.ping.ui.addPost.*

@HiltViewModel
class AddPostFragmentViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    val action = MutableLiveData<AddPostEvent>()

    private val addPostFragmentEventChannel = Channel<AddPostFragmentEvent>()
    private val fileStorageReference =
        FirebaseStorage.getInstance().getReference("ApartmentUploads")
    private val apartmentsDatabaseReference =
        FirebaseDatabase.getInstance().getReference("apartments")
    val addPostFragmentEvent = addPostFragmentEventChannel.receiveAsFlow()

    var apartmentModel = ApartmentHomePost()

    init {
        viewModelScope.launch {
            getUserID()
        }
    }

    var userID: String = ""
        set(value) {
            field = value
            apartmentModel.landLordID = value
        }

    var title: String = state.get<String>("titleText") ?: ""
        set(value) {
            field = value
            state.set("titleText", value)
            apartmentModel.title = value
        }
    var aptTypeValue: Int = state.get<Int>("apartmentType") ?: APT_TYPE_FLAT
        set(value) {
            field = value
            state.set("apartmentType", value)
            apartmentModel.aptType = value
        }
    var floorValue: Int = state.get<Int>("floorValue") ?: 0
        set(value) {
            if (value in 0..50) {
                field = value
                state.set("floorValue", value)
                apartmentModel.aptFloor = value
                updatePickerTV(value.toString(), roomAmount.toString())
            }
        }
    var roomAmount: Int = state.get<Int>("roomValue") ?: 0
        set(value) {
            if (value in 0..50) {
                field = value
                state.set("roomValue", value)
                apartmentModel.roomAmount = value
                updatePickerTV(floorValue.toString(), value.toString())
            }
        }
    var furnishedValue: Boolean = state.get<Boolean>("furnishedValue") ?: false
        set(value) {
            field = value
            state.set("furnishedValue", value)
            apartmentModel.isFurnished = value
        }
    var acreage: Double = state.get<Double>("acreageValue") ?: 0.0
        set(value) {
            field = value
            state.set("acreageValue", value)
            apartmentModel.acreage = value
        }
    var city: String = state.get<String>("cityValue") ?: ""
        set(value) {
            field = value
            state.set("cityValue", value)
            apartmentModel.city = value
        }
    var address: String = state.get<String>("addressValue") ?: ""
        set(value) {
            field = value
            state.set("addressValue", value)
            apartmentModel.address = value
        }
    var description: String = state.get<String>("descriptionValue") ?: ""
        set(value) {
            field = value
            state.set("descriptionValue", value)
            apartmentModel.description = value
        }
    var price: Int = state.get<Int>("priceValue") ?: 0
        set(value) {
            field = value
            state.set("priceValue", value)
            apartmentModel.price = value
        }
    var priceType: Int = state.get<Int>("priceType") ?: 0
        set(value) {
            field = value
            state.set("priceType", value)
            apartmentModel.priceType = value
        }
    var imagesArray = ArrayList<AddImageModel>()
    var scrollPositionY: Int = state.get<Int>("scrollPositionY") ?: 0
        set(value) {
            field = value
            state.set("scrollPositionY", value)
        }

    //var imagesArrayLiveData = MutableLiveData<ArrayList<AddImageModel>>()
    var progressTextLiveData = MutableLiveData<String>()
    var isImagesArrayInitialized = false
    var latestClickedImageButton: Int = 0

    fun onFlatButtonClicked() {
        if (aptTypeValue == APT_TYPE_HOUSE) {
            aptTypeValue = APT_TYPE_FLAT
            viewModelScope.launch {
                addPostFragmentEventChannel.send(AptTypeChanged(aptTypeValue))
            }
        }
    }

    fun onHouseButtonClicked() {
        if (aptTypeValue == APT_TYPE_FLAT) {
            aptTypeValue = APT_TYPE_HOUSE
            viewModelScope.launch {
                addPostFragmentEventChannel.send(AptTypeChanged(aptTypeValue))
            }
        }
    }

    fun furnishingBTNCLicked() {
        furnishedValue = !furnishedValue
        viewModelScope.launch {
            addPostFragmentEventChannel.send(FurnishedValueChanged(furnishedValue))
        }
    }

    fun priceTypeClicked(buttonClicked: Int) {
        when (buttonClicked) {
            PRICE_TYPE_PER_DAY -> {
                if (priceType != PRICE_TYPE_PER_DAY) changePriceType(PRICE_TYPE_PER_DAY)
            }
            PRICE_TYPE_PER_WEEK -> {
                if (priceType != PRICE_TYPE_PER_WEEK) changePriceType(PRICE_TYPE_PER_WEEK)
            }
            PRICE_TYPE_PER_MONTH -> {
                if (priceType != PRICE_TYPE_PER_MONTH) changePriceType(PRICE_TYPE_PER_MONTH)
            }
        }
    }

    fun insertPreviousImages() {
        for (model in imagesArray) {
            if (model.uri != Uri.EMPTY) updateAdapterValues()
        }
    }

    fun onImageRemoveButtonClicked(addImageModel: AddImageModel) {
        imagesArray.forEachIndexed { index, model ->
            if (model.viewID == addImageModel.viewID) {

                for (currentIndex in (index + 1) until imagesArray.size) {
                    val currentImageModel = imagesArray[currentIndex]
                    val lowerViewId = currentImageModel.viewID - 1
                    imagesArray[currentIndex - 1] = currentImageModel.copy(viewID = lowerViewId)
                }

                imagesArray.last().apply {
                    filledIn = false
                    uri = Uri.EMPTY
                }

                action.value = AddPostEvent(AddPostAction.ADAPTER_CHANGED)
            }
        }
    }

    fun handleFinishedImageIntent(uri: Uri?, byteArrayCompressed: ByteArray) {

        for ((index, model) in imagesArray.withIndex()) {

            //If it's empty, fill it in
            if (!model.filledIn) {
                if (uri != null) {
                    imagesArray[index] = model.copy(
                        filledIn = true,
                        uri = uri,
                        byteArrayCompressed = byteArrayCompressed
                    )
                    action.value = AddPostEvent(AddPostAction.ADAPTER_CHANGED)
                    return
                }
            }
            //If it's not empty, AND:
            //If this is the clicked one, and it's empty - insert image
            if (model.viewID == latestClickedImageButton && !model.filledIn) {
                if (uri == null) {
                    action.value = AddPostEvent(AddPostAction.ADAPTER_CHANGED)
                    return
                } else {
                    imagesArray[index] =
                        model.copy(uri = uri, byteArrayCompressed = byteArrayCompressed)
                    action.value = AddPostEvent(AddPostAction.ADAPTER_CHANGED)
                    return
                }
            }

            if (model.viewID == latestClickedImageButton) {
                if (uri == null) {
                    action.value = AddPostEvent(AddPostAction.ADAPTER_CHANGED)
                    return
                } else {
                    imagesArray[index] =
                        model.copy(uri = uri, byteArrayCompressed = byteArrayCompressed)
                    action.value = AddPostEvent(AddPostAction.ADAPTER_CHANGED)
                    return
                }
            }

        }
    }

    fun onUploadButtonClicked() {
        if (checkAllFieldsValidity()) startUploadingApartment()
    }

    private fun startUploadingApartment() {
        changeProgressBarStatus(View.VISIBLE)
        apartmentModel.timeStamp = System.currentTimeMillis()
        val uploadImagesReference = fileStorageReference.child(
            apartmentModel.timeStamp.toString()
        )
        var index = 0
        setProgressText(index)
        for (currentImage in imagesArray) {

            if (currentImage.filledIn && currentImage.viewID != getAmountOfFilledImages() - 1) {
                uploadImagesReference
                    .child(currentImage.viewID.toString())
                    .putBytes(currentImage.byteArrayCompressed)
                    .addOnSuccessListener { task ->
                        index++
                        setProgressText(index)
                    }
                    .addOnFailureListener {
                        sendToastMessage(it.message.toString())
                    }
            }
            if (currentImage.viewID == getAmountOfFilledImages() - 1) {
                uploadImagesReference
                    .child(currentImage.viewID.toString())
                    .putBytes(currentImage.byteArrayCompressed)
                    .addOnSuccessListener { task ->
                        index++
                        setProgressText(index)

                        apartmentModel.imagesReference = uploadImagesReference.toString()
                        uploadDataFields(uploadImagesReference.child("0"))
                    }
                    .addOnFailureListener {
                        sendToastMessage(it.message.toString())
                    }
            }
        }
    }

    fun switchRecyclerViewChildrenStatus(status: Boolean) {
        for (currentImage in imagesArray) {
            currentImage.enabledStatus = status
        }

        action.value = AddPostEvent(AddPostAction.ADAPTER_CHANGED)
    }

    fun pickerButtonClicked(clickType: Int, pickerType: Int) {
        when (clickType) {
            CLICK_TYPE_PLUS -> {
                when (pickerType) {
                    PICKER_TYPE_FLOOR -> floorValue += 1
                    PICKER_TYPE_ROOMS -> roomAmount += 1
                }
            }
            CLICK_TYPE_MINUS -> {
                when (pickerType) {
                    PICKER_TYPE_FLOOR -> floorValue -= 1
                    PICKER_TYPE_ROOMS -> roomAmount -= 1
                }
            }
        }
    }

    private fun uploadDataFields(imagesReference: StorageReference) {
        progressTextLiveData.value = "Finishing upload..."
        apartmentModel.apartmentPostID = apartmentsDatabaseReference.push().key ?: ""

        imagesReference.downloadUrl.addOnSuccessListener { downloadUrl ->
            apartmentModel.firstImageReference = downloadUrl.toString()
            apartmentsDatabaseReference.child(apartmentModel.apartmentPostID)
                .setValue(apartmentModel)
                .addOnSuccessListener {
                    sendToastMessage("Post uploaded successfully!")
                }
                .addOnFailureListener {
                    sendToastMessage(it.message.toString())
                }
        }
        changeProgressBarStatus(View.GONE)
    }

    private fun getAmountOfFilledImages(): Int {
        var count = 0
        for (image in imagesArray) {
            if (image.filledIn) {
                count++
            }
        }
        apartmentModel.imageCount = count - 1
        return count
    }

    private fun setProgressText(amount: Int) {
        progressTextLiveData.value = "Uploading photos... $amount/${getAmountOfFilledImages()}"
    }

    private fun updateAdapterValues() {
        viewModelScope.launch {
            addPostFragmentEventChannel.send(UpdateAdapterValues)
        }
    }

    private fun changeProgressBarStatus(status: Int) {
        viewModelScope.launch {
            addPostFragmentEventChannel.send(ChangeProgressbarStatus(status))
        }
    }

    private fun updatePickerTV(pickerFloor: String, pickerRooms: String) = viewModelScope.launch {
        addPostFragmentEventChannel.send(UpdatePickerTV(pickerFloor, pickerRooms))
    }

    private fun sendToastMessage(message: String) {
        viewModelScope.launch {
            addPostFragmentEventChannel.send(SendToastMessage(message))
        }
    }

    private fun changePriceType(priceTypeValue: Int) {
        priceType = priceTypeValue
        viewModelScope.launch {
            addPostFragmentEventChannel.send(PriceTypeChange(priceTypeValue))
        }
    }

    private fun getUserID() {
        viewModelScope.launch {
            userID = preferencesManager.getUserID()
        }
    }

    private fun checkAllFieldsValidity(): Boolean {
        if (
            title.isEmpty() ||
            roomAmount == 0 ||
            acreage == 0.0 ||
            city.isEmpty() ||
            address.isEmpty() ||
            description.isEmpty() ||
            price == 0
        ) sendToastMessage("Please fill in all the blanks!")
        else if (aptTypeValue == APT_TYPE_FLAT && floorValue == 0) sendToastMessage("Please fill in all the blanks!")
        else if (getAmountOfFilledImages() == 0) sendToastMessage("Please upload at least one picture!")
        else return true

        return false
    }

    data class AddPostEvent(val actionType: AddPostAction, val num: Int? = null)

    enum class AddPostAction {
        ADAPTER_CHANGED
    }

}

sealed class AddPostFragmentEvent {
    data class AptTypeChanged(val typeValue: Int) : AddPostFragmentEvent()
    data class FurnishedValueChanged(val furnishedValue: Boolean) : AddPostFragmentEvent()
    data class PriceTypeChange(val priceType: Int) : AddPostFragmentEvent()
    object UpdateAdapterValues : AddPostFragmentEvent()
    data class ChangeProgressbarStatus(val status: Int) : AddPostFragmentEvent()
    data class SendToastMessage(val message: String) : AddPostFragmentEvent()
    data class UpdatePickerTV(val pickerFloor: String, val pickerRooms: String) :
        AddPostFragmentEvent()
}