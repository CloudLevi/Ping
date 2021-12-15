package com.cloudlevi.ping.data

import android.net.Uri

data class MediaAttachment(
    var uri: Uri = Uri.EMPTY,
    var byteArray: ByteArray? = null
) {
}