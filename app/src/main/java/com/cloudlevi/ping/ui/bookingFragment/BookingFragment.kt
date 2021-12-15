package com.cloudlevi.ping.ui.bookingFragment

import android.content.ContentValues
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.core.util.Pair
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.bumptech.glide.Glide
import com.cloudlevi.ping.BaseFragment
import com.cloudlevi.ping.R
import com.cloudlevi.ping.STRIPE_KEY_TEST
import com.cloudlevi.ping.data.PaymentType
import com.cloudlevi.ping.data.RentalMode
import com.cloudlevi.ping.databinding.FragmentBookingBinding
import com.cloudlevi.ping.ext.*
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import dagger.hilt.android.AndroidEntryPoint
import com.cloudlevi.ping.ui.bookingFragment.BookingViewModel.Action
import com.cloudlevi.ping.ui.bookingFragment.BookingViewModel.ActionType.*
import com.cloudlevi.ping.ui.userChat.MessageMediaAdapter
import com.cloudlevi.ping.ui.yourBookings.YourBookingsViewModel
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentIntentResult
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.StripeIntent

@AndroidEntryPoint
class BookingFragment :
    BaseFragment<FragmentBookingBinding>(R.layout.fragment_booking, true) {

    private lateinit var binding: FragmentBookingBinding
    private val viewModel: BookingViewModel by viewModels()
    private var stripe: Stripe? = null

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentBookingBinding =
        FragmentBookingBinding::inflate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentBookingBinding.inflate(inflater, container, false)

        val args = BookingFragmentArgs.fromBundle(requireArguments())
        viewModel.fragmentCreated(args.apartmentHomePost, args.landLord)
        stripe = Stripe(requireContext(), STRIPE_KEY_TEST)

        viewModel.doAction.observe(viewLifecycleOwner) {
            val data = it.getDataSafely()
            if (data != null) doAction(data)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyFieldsFromVM()

        binding.apply {
            backBtn.setOnClickListener { findNavController().popBackStack() }
            dateRangeLayout.setOnClickListener { showDatePicker() }
            timePickerLayout.setOnClickListener { showTimePicker() }
            continueBtn.setOnClickListener { proceedBooking() }
            clearTV.setOnClickListener { resetFields() }
            paymentTypeLayout.setOnCheckedChangeListener { _, checkedID ->
                paymentTypeCheckedChanged(checkedID)
            }
            specialEditText.addTextChangedListener {
                viewModel.specialWishesText = it?.toString()?.trim() ?: ""
            }

        }
    }

    private fun showDatePicker() {

        val constraints =
            CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build()

        val dateRangePickerBuilder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.select_dates))
            .setCalendarConstraints(constraints)

        if (viewModel.areDatesSelected())
            dateRangePickerBuilder.setSelection(
                Pair.create(viewModel.checkInDateLong, viewModel.checkOutDateLong)
            )

        val dateRangePicker = dateRangePickerBuilder.build()

        dateRangePicker.addOnPositiveButtonClickListener { applyDateText(it.first, it.second) }
        dateRangePicker.show(requireActivity().supportFragmentManager, "DatePicker")
    }

    private fun paymentTypeCheckedChanged(checkedID: Int) {
        binding.paymentTypeError.makeGone()
        binding.cardInputError.makeGone()

        viewModel.paymentType = when (checkedID) {
            R.id.cardPayment -> PaymentType.CARD
            else -> PaymentType.CASH
        }

        binding.paymentTypeLayout.forEach { rb ->
            val textColor = if (rb.id != checkedID) R.color.text_color
            else R.color.white

            (rb as RadioButton).setTextColor(getColor(textColor))
        }

        viewModel.calculatePricing()
        pricingUpdated()
        switchContinueText()

        if (binding.pricingLayout.isVisible && viewModel.paymentType == PaymentType.CARD)
            binding.cardInputLayout.makeVisible()
        else binding.cardInputLayout.makeGone()
    }

    private fun showTimePicker() {

        val tpd = TimePickerDialog.newInstance(
            { view, hourOfDay, minute, second ->
                applyTimeText(hourOfDay, minute)
            },
            viewModel.checkInHour,
            viewModel.checkInMinute,
            true
        )
        tpd.title = getString(R.string.select_check_in_time)
        tpd.setMinTime(14, 0, 0)

        tpd.show(requireActivity().supportFragmentManager, "TimePickerDialog")
    }

    private fun applyDateText(startDate: Long, endDate: Long) {
        val dateRangeText =
            "${startDate.showDateCheckinCheckout()} - ${endDate.showDateCheckinCheckout()}"
        binding.dateRangeTV.setTypeface(null, Typeface.NORMAL)
        binding.dateRangeTV.text = dateRangeText
        binding.datesError.makeGone()

        viewModel.checkInDateLong = startDate
        viewModel.checkOutDateLong = endDate

        viewModel.calculatePricing()
        pricingUpdated()
    }


    private fun doAction(action: Action) {
        when (action.actionType) {
            OPEN_IMAGE_AT -> openImageAt(action.pos ?: 0)
            TOAST -> {
                toggleProgress(false)
                sendLongToast(action.msg ?: "")
            }
            STRIPE_CONFIRM -> if (action.confirmParams != null) onStripeConfirm(action.confirmParams)
            else toggleProgress(false)
            TOGGLE_PROGRESS -> toggleProgress(action.bool ?: false)
            BOOKING_CREATED -> onBookingCreated()
        }
    }

    private fun onBookingCreated() {
        sendLongToast("Booked successfully")
        val action =
            BookingFragmentDirections.actionBookingFragmentToYourBookingsFragment(RentalMode.TENANT_MODE)
        findNavController().navigate(action)
    }

    private fun onStripeConfirm(confirmParams: ConfirmPaymentIntentParams) {
        stripe?.confirmPayment(this, confirmParams)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        stripe?.onPaymentResult(requestCode, data, paymentResultCallback)
    }

    private val paymentResultCallback = object : ApiResultCallback<PaymentIntentResult> {
        override fun onError(e: Exception) {
            toggleProgress(false)
            Log.d(ContentValues.TAG, "Payment error: ${e.message}")
            sendLongToast("Payment error: ${e.message}")
        }

        override fun onSuccess(result: PaymentIntentResult) {
            toggleProgress(false)
            val paymentIntent = result.intent
            val status = paymentIntent.status
            if (status == StripeIntent.Status.Succeeded) {
                viewModel.saveBookingRemotely(true)
            } else if (status == StripeIntent.Status.RequiresPaymentMethod) {
                sendLongToast("Payment failed, error: ${paymentIntent.lastPaymentError}")
            }
        }
    }

    private fun openImageAt(startPos: Int) {

        val mediaHolder =
            (binding.imagesRecycler.findViewHolderForAdapterPosition(startPos) as? MessageMediaAdapter.MediaViewHolder)
        val imagesList = viewModel.currentHomePost?.imagesList

        val imageLoader: (view: ImageView, image: Uri) -> Unit = { view, image ->
            Glide.with(view.context)
                .load(image)
                .into(view)
        }
        val imageChangeListener: (pos: Int) -> Unit = { pos ->
            //holder?.updateSavedScrollPos(pos)
        }
        StfalconImageViewer.Builder(requireContext(), imagesList, imageLoader)
            .withStartPosition(startPos)
            .withTransitionFrom(mediaHolder?.binding?.imageView)
            .withImageChangeListener(imageChangeListener)
            .withHiddenStatusBar(false)
            .show()
    }

    private fun applyTimeText(startHour: Int, startMinute: Int) {
        val timeText = "$startHour:${startMinute.toMinuteString()}"
        binding.checkInTime.text = timeText

        viewModel.checkInHour = startHour
        viewModel.checkInMinute = startMinute
    }

    private fun proceedBooking() {
        binding.apply {
            if (!fieldsValidated()) return

            when {
                !timePickerLayout.isVisible -> timePickerLayout.makeVisible()
                !paymentTypeLayout.isVisible -> {
                    paymentTypeTV.makeVisible()
                    paymentTypeLayout.makeVisible()
                    specialWishLayout.makeVisible()
                }
                !pricingLayout.isVisible -> {
                    pricingLayout.makeVisible()
                    if (viewModel.paymentType == PaymentType.CARD)
                        cardInputLayout.makeVisible()
                    else cardInputLayout.makeGone()
                    switchContinueText()
                }
                else -> {
                    if (cardInputLayout.isVisible) {
                        toggleProgress(true, R.string.processing_payment)
                        val params = cardInputWidget.paymentMethodCreateParams
                        if (params != null)
                            viewModel.startPayment(params)
                    } else {
                        toggleProgress(true, R.string.loading)
                        viewModel.saveBookingRemotely()
                    }
                }
            }
        }
    }

    private fun switchContinueText(text: String? = null) {
        binding.apply {
            if (text == null) {
                if (pricingLayout.isVisible) {
                    continueBtn.text = if (viewModel.paymentType == PaymentType.CARD)
                        getString(R.string.pay)
                    else getString(R.string.continue_text)
                }
            } else continueBtn.text = text
        }
    }

    private fun fieldsValidated(): Boolean {
        var isValidated = true
        binding.apply {
            if (!viewModel.areDatesSelected()) {
                isValidated = false
                datesError.makeVisible()
            }

            if (paymentTypeLayout.isVisible && !viewModel.isPaymentTypeSelected()) {
                isValidated = false
                paymentTypeError.makeVisible()
            }

            if (cardInputLayout.isVisible && cardInputWidget.cardParams == null) {
                isValidated = false
                cardInputError.makeVisible()
            }
        }

        return isValidated
    }

    private fun pricingUpdated() {
        binding.apply {
            val currencySymbol = viewModel.currentHomePost?.currency ?: "$"
            val aptPriceText = viewModel.aptPriceLocalized.applyCurrencySymbol(currencySymbol)
            val pingFeeText = viewModel.pingFeeLocalized.applyCurrencySymbol(currencySymbol)
            val totalText = viewModel.totalLocalized.applyCurrencySymbol(currencySymbol)
            val totalToPayText = viewModel.total.applyCurrencySymbol("$")

            if (viewModel.pingFee == 0.0) {
                pingFeeLayout.makeGone()
            } else {
                pingFeeLayout.makeVisible()
            }

            aptPriceTV.text = aptPriceText
            pingFeeTV.text = pingFeeText
            totalTV.text = totalText
            totalToPayTV.text = totalToPayText
        }
    }

    private fun applyFieldsFromVM() {
        val apPost = viewModel.currentHomePost ?: return
        val mediaCount = apPost.imagesList.size

        binding.apply {
            if (viewModel.areDatesSelected())
                applyDateText(viewModel.checkInDateLong, viewModel.checkOutDateLong)

            applyTimeText(viewModel.checkInHour, viewModel.checkInMinute)

            val countryText = if (apPost.country.isEmpty()) "" else
                ", ${apPost.country}"
            val cityText = "${apPost.city}${countryText}"

            locationTV.text = apPost.address
            cityTV.text = cityText
            val acreageText = String.format(resources.getString(R.string.m2), apPost.acreage)
            acreageTV.text = HtmlCompat.fromHtml(acreageText, HtmlCompat.FROM_HTML_MODE_LEGACY)
            roomCountTV.text = apPost.roomCountString(requireContext())
            furnishmentTV.text = if (apPost.isFurnished) getString(R.string.furnished)
            else getString(R.string.no_furniture)
            ratingTV.text = apPost.calculateAverageRating().toString()
            val priceText =
                "${apPost.getPricingText()}/${apPost.priceTypeString(requireContext())}"
            priceTV.text = priceText

            imagesRecycler.adapter = viewModel.imagesAdapter
            PagerSnapHelper().attachToRecyclerView(imagesRecycler)

            if (mediaCount > 1) {
                val initialText = "1/${apPost.imagesList.size}"
                counterTV.makeVisible()
                counterTV.text = initialText

                imagesRecycler.setOnScrollChangeListener { view, _, _, _, _ ->
                    val layoutManager =
                        imagesRecycler.layoutManager as? LinearLayoutManager
                            ?: return@setOnScrollChangeListener

                    val firstVisible = layoutManager.findFirstVisibleItemPosition()

                    val text = "${firstVisible + 1}/$mediaCount"
                    counterTV.text = text
                }
            } else counterTV.makeGone()
        }
    }

    private fun toggleProgress(isLoading: Boolean, @StringRes textID: Int = R.string.loading) {
        toggleAllViewsEnabled(!isLoading, binding.root)
        binding.loadingTV.text = getString(textID)
        binding.progressLayout.isVisible = isLoading
    }

    private fun resetFields() {
        viewModel.clearFields()

        toggleProgress(false)

        binding.apply {
            dateRangeTV.setTypeface(null, Typeface.BOLD)
            dateRangeTV.text = getString(R.string.select_checkin_and_checkout_time)

            checkInTime.text = "14:00"

            paymentTypeLayout.clearFocus()
            paymentTypeLayout.clearCheck()

            paymentTypeLayout.forEach {
                it as RadioButton
                it.setTextColor(getColor(R.color.text_color))
            }
            timePickerLayout.makeGone()
            pricingLayout.makeGone()
            paymentTypeTV.makeGone()
            paymentTypeLayout.makeGone()
            specialWishLayout.makeGone()
            cardInputLayout.makeGone()

            continueBtn.text = getString(R.string.continue_text)

            specialEditText.clearFocus()
            specialEditText.setText("")

            datesError.makeGone()
            paymentTypeError.makeGone()
            cardInputError.makeGone()
        }
    }
}