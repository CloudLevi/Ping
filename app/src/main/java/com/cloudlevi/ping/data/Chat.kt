package com.cloudlevi.ping.data

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import com.google.firebase.database.Exclude
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

data class Chat(
    val chatID: String = "",
    var messages: List<Message> = mutableListOf()
) {

}

@Parcelize
data class Message(
    val messageID: String? = "",
    val message: String? = "",
    val senderID: String? = "",
    val timeStamp: Long? = null,
    var read: Boolean? = false,
    var mediaCount: Int? = 0
): Parcelable {

    @IgnoredOnParcel
    @Exclude
    @set:Exclude @get:Exclude var imagesList: MutableList<Uri> = mutableListOf()

    fun hasMedia() = mediaCount != null && mediaCount != 0

}