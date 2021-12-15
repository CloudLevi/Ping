package com.cloudlevi.ping.ext

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import java.io.ByteArrayOutputStream
import java.io.File
import android.provider.MediaStore
import android.graphics.BitmapFactory

import android.provider.ContactsContract

import android.content.ContentResolver

fun processIntentImage(
    context: Context,
    uri: Uri?,
    callback: (uri: Uri, byteArray: ByteArray) -> Unit
) {
    Glide.with(context)
        .asBitmap()
        .load(uri)
        .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(
                resource: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                uri?: return
                val baos = ByteArrayOutputStream()
                resource.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                callback(uri, baos.toByteArray())
            }

            override fun onLoadCleared(placeholder: Drawable?) {
            }
        })
}