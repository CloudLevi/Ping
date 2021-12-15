package com.cloudlevi.ping.ext

import android.annotation.SuppressLint
import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.forEach
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.SortBy
import com.cloudlevi.ping.data.SortOrder
import com.cloudlevi.ping.databinding.DialogChangeLanguageBinding
import com.cloudlevi.ping.databinding.DialogRatingBinding
import com.cloudlevi.ping.databinding.DialogSortingBinding

@SuppressLint("ClickableViewAccessibility")
fun showRatingDialog(
    context: Context,
    initialValue: Double,
    comment: String?,
    listener: RatingDialogListener
) {

    var isRated = false

    val builder = AlertDialog.Builder(
        context,
        R.style.AppTheme_AlertDialogStyle
    )

    val view = View.inflate(context, R.layout.dialog_rating, null)
    val binding = DialogRatingBinding.bind(view)

    builder.setView(binding.root)

    binding.apply {
        builder.setPositiveButton(
            R.string.submit
        ) { p0, p1 -> }

        builder.setNegativeButton(
            R.string.delete_review
        ) { p0, p1 -> }

        ratingBar.setOnTouchListener { view, motionEvent ->
            isRated = true
            errorTV.makeGone()
            ratingTV.text = getRatingText(context, ratingBar.rating)
            false
        }

        if (initialValue != -1.0) {
            isRated = true
            ratingBar.rating = initialValue.toFloat()
            ratingTV.text = getRatingText(context, ratingBar.rating)
        }
        if (!comment.isNullOrEmpty()) {
            commentEditText.setText(comment)
        }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton: Button = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            positiveButton.setOnClickListener {
                if (!isRated) {
                    errorTV.makeVisible()
                    return@setOnClickListener
                }
                listener.onPositiveClick(ratingBar.rating, commentEditText.text.toString().trim())
                dialog.dismiss()
            }

            negativeButton.setOnClickListener {
                listener.onNegativeClick()
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.rounded_bg_16dp
            )
        )
    }
}

@SuppressLint("ClickableViewAccessibility")
fun showPickerDialog(
    context: Context,
    @StringRes titleRes: Int,
    @ArrayRes pickerValuesRes: Int,
    initialValue: Int,
    listener: LanguageDialogListener
) {
    val builder = AlertDialog.Builder(
        context,
        R.style.AppTheme_AlertDialogStyle
    )

    val view = View.inflate(context, R.layout.dialog_change_language, null)
    val binding = DialogChangeLanguageBinding.bind(view)

    builder.setView(binding.root)

    binding.apply {
        titleTV.setText(titleRes)
        val array = context.resources.getStringArray(pickerValuesRes)
        picker.minValue = 0
        picker.maxValue = array.lastIndex
        picker.displayedValues = array

        builder.setPositiveButton(
            R.string.apply
        ) { p0, p1 -> }

        builder.setNegativeButton(
            R.string.cancel
        ) { p0, p1 -> }

        if (initialValue != -1) {
            picker.value = initialValue
        }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positiveButton.setOnClickListener {
                listener.onPositiveClick(picker.value)
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.rounded_bg_16dp
            )
        )
    }
}

@SuppressLint("ClickableViewAccessibility")
fun showSortingDialog(
    context: Context,
    initialSortBy: SortBy = SortBy.NONE,
    initialSortOrder: SortOrder = SortOrder.NONE,
    callback: (sortEnum: SortBy, sortOrder: SortOrder) -> Unit
) {
    val builder = AlertDialog.Builder(
        context,
        R.style.AppTheme_AlertDialogStyle
    )

    val view = View.inflate(context, R.layout.dialog_sorting, null)
    val binding = DialogSortingBinding.bind(view)

    builder.setView(binding.root)

    binding.apply {

        builder.setPositiveButton(
            R.string.apply
        ) { p0, p1 -> }

        builder.setNegativeButton(
            R.string.cancel
        ) { p0, p1 -> }

        val sortByButtonID = getCurrentSortOrderID(sortOptions, initialSortBy)
        if (sortByButtonID >= 0) {
            val button = sortOptions.findViewById<RadioButton>(sortByButtonID)
            button.isChecked = true
            button.setTextColor(ContextCompat.getColor(button.context, R.color.white))
        }

        if (initialSortOrder != SortOrder.NONE){
            val button = if (initialSortOrder == SortOrder.ASCENDING) ascendingButton
            else descendingButton
            button.isChecked = true
            button.setTextColor(ContextCompat.getColor(button.context, R.color.white))
        }

        sortOptions.forEach { view ->
            if (view is RadioButton) {
                view.setOnClickListener {
                    sortByError.makeGone()
                    uncheckAllRadioButtons(sortOptions, it.id)
                    view.setTextColor(ContextCompat.getColor(view.context, R.color.white))
                }
            }
        }

        sortOrderOptions.forEach { view ->
            if (view is RadioButton) {
                view.setOnClickListener {
                    sortOrderError.makeGone()
                    uncheckAllRadioButtons(sortOrderOptions, it.id)
                    view.setTextColor(ContextCompat.getColor(view.context, R.color.white))
                }
            }
        }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positiveButton.setOnClickListener {
                val sortBy = getSortByEnum(sortOptions)
                val sortOrder = getSortOrderEnum(sortOrderOptions)

                var isError = false

                if (sortBy == SortBy.NONE) {
                    sortByError.makeVisible()
                    isError = true
                }
                if (sortOrder == SortOrder.NONE) {
                    isError = true
                    sortOrderError.makeVisible()
                }

                if (isError) return@setOnClickListener

                callback(sortBy, sortOrder)
                dialog.dismiss()
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.rounded_bg_16dp
            )
        )
    }
}

fun getRatingText(context: Context, rating: Float): String {
    @StringRes
    val resID = when (rating) {
        0f -> R.string.awful_experience
        0.5f -> R.string.very_bad_experience
        1f -> R.string.bad_experience
        1.5f -> R.string.unpleasant_experience
        2f -> R.string.subpar_experience
        2.5f -> R.string.acceptable_experience
        3f -> R.string.normal_experience
        3.5f -> R.string.good_experience
        4f -> R.string.great_experience
        4.5f -> R.string.amazing_experience
        else -> R.string.outstanding_experience
    }

    return context.getString(resID)
}

fun getSortByEnum(viewGroup: ViewGroup): SortBy {
    var returnValue = SortBy.NONE
    viewGroup.children.filterIsInstance<RadioButton>().forEach { rb ->
        if (rb.isChecked) {
            returnValue = when (rb.id) {
                R.id.priceButton -> SortBy.PRICE
                R.id.timeButton -> SortBy.TIME
                R.id.nameButton -> SortBy.NAME
                R.id.ratingButton -> SortBy.RATING
                R.id.acreageButton -> SortBy.ACREAGE
                R.id.roomButton -> SortBy.ROOM_AMOUNT
                else -> SortBy.NONE
            }
        }
    }
    return returnValue
}

fun getCurrentSortOrderID(viewGroup: ViewGroup, initialSortBy: SortBy): Int {
    var returnValue = -1
    viewGroup.children.filterIsInstance<RadioButton>().forEach { rb ->
        returnValue = when (initialSortBy) {
            SortBy.PRICE -> R.id.priceButton
            SortBy.TIME -> R.id.timeButton
            SortBy.NAME -> R.id.nameButton
            SortBy.RATING -> R.id.ratingButton
            SortBy.ACREAGE -> R.id.acreageButton
            SortBy.ROOM_AMOUNT -> R.id.roomButton
            else -> -1
        }
    }
    return returnValue
}

fun getSortOrderEnum(viewGroup: ViewGroup): SortOrder {
    var returnValue = SortOrder.NONE
    viewGroup.children.filterIsInstance<RadioButton>().forEach { rb ->
        if (rb.isChecked) {
            returnValue = when (rb.id) {
                R.id.ascendingButton -> SortOrder.ASCENDING
                R.id.descendingButton -> SortOrder.DESCENDING
                else -> SortOrder.NONE
            }
        }
    }
    return returnValue
}

fun uncheckAllRadioButtons(viewGroup: ViewGroup, idExcluded: Int) {
    viewGroup.forEach {
        if (it is RadioButton && it.id != idExcluded) {
            it.isChecked = false
            it.setTextColor(ContextCompat.getColor(it.context, R.color.buttonColorActive))
        }
    }
}


interface RatingDialogListener {
    fun onPositiveClick(rating: Float, experience: String)
    fun onNegativeClick()
}

interface LanguageDialogListener {
    fun onPositiveClick(pos: Int)
}