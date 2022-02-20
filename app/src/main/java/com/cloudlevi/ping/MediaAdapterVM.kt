package com.cloudlevi.ping

import android.net.Uri
import com.google.firebase.storage.StorageReference

interface MediaAdapterVM {
    fun getCurrentListByID(id: String?): MutableList<StorageReference>
    fun getCurrentListByPos(pos: Int): MutableList<StorageReference>
    fun onMediaImageClick(id: String?, position: Int?)
}