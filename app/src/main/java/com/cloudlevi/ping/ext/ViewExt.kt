package com.cloudlevi.ping.ext

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Space
import android.widget.TextView
import androidx.core.view.forEach
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

fun TextView.applyText(appliedText: String?) {
    if (appliedText.isNullOrEmpty()) {
        makeGone()
        return
    }
    text = appliedText
    makeVisible()
}

fun View.makeGone() {
    this.visibility = View.GONE
}

fun View.makeVisible() {
    this.visibility = View.VISIBLE
}

fun View.visibleOrGone(isVisible: Boolean) {
    if (isVisible) this.makeVisible()
    else this.makeGone()
}


fun toggleAllViewsEnabled(isEnabled: Boolean, view: View) {
    if (view is ViewGroup) {
        view.forEach {
            toggleAllViewsEnabled(isEnabled, it)
        }
    } else view.isEnabled = isEnabled
}

fun RecyclerView.attachSnapHelper() {
    this.onFlingListener = null
    PagerSnapHelper().attachToRecyclerView(this)
}

fun RecyclerView.removeAnimations() {
    (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
}

fun EditText.blockSpaces() {
    this.addTextChangedListener(SpacesWatcher(this))
}

private class SpacesWatcher(
    val editText: EditText
) :
    TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun afterTextChanged(p: Editable?) {
        val inputText = p.toString()
        val newText = inputText.filterNot { it.isWhitespace() }
        if (inputText == newText) return

        val selection = editText.selectionStart

        editText.removeTextChangedListener(this)
        editText.setText(newText)

        if (selection >= newText.length) editText.setSelection(newText.length)
        else editText.setSelection(selection)

        editText.addTextChangedListener(this)
    }
}