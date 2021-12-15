package com.cloudlevi.ping

import android.net.Uri

interface MediaAdapterVM {
    fun getCurrentListByID(id: String?): MutableList<Uri>
    fun getCurrentListByPos(pos: Int): MutableList<Uri>
    fun onMediaImageClick(id: String?, position: Int?)
}