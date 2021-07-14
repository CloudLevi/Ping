package com.cloudlevi.ping.data

import android.net.Uri

data class AddImageModel constructor(
    var viewID: Int = 0,
    var filledIn: Boolean = false,
    var uri: Uri = Uri.EMPTY,
    var enabledStatus: Boolean = true,
    var byteArrayCompressed: ByteArray = byteArrayOf()
) {
}