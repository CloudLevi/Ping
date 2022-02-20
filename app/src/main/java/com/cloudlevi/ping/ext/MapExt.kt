package com.cloudlevi.ping.ext

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

fun GoogleMap.setNewMarkerCustom(
    latLng: LatLng,
    animDuration: Int = 300,
    zoom: Float = 15f,
    clear: Boolean = true
): Marker? {
    if (clear) clear()
    val marker = addMarker(MarkerOptions().position(latLng))
    val cameraPosition = CameraPosition.Builder()
        .target(latLng)
        .zoom(zoom)
        .build()
    val cu = CameraUpdateFactory.newCameraPosition(cameraPosition)
    animateCamera(cu, animDuration, object : GoogleMap.CancelableCallback {
        override fun onCancel() {}
        override fun onFinish() {}
    })
    return marker
}

fun Marker.getAddress(context: Context): String? {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(
        position.latitude,
        position.longitude,
        1
    )
    return addresses.getOrNull(0)?.getAddressLine(0)
}

fun Marker.getCountry(context: Context): String? {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(
        position.latitude,
        position.longitude,
        1
    )
    return addresses.getOrNull(0)?.countryName
}

fun Marker.getCity(context: Context): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(
        position.latitude,
        position.longitude,
        1
    )
    return addresses.getOrNull(0)?.locality ?: ""
}

fun LatLng.getStreet(context: Context): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(
        latitude,
        longitude,
        1
    )
    return addresses.getOrNull(0)?.thoroughfare ?: ""
}

fun LatLng.getCity(context: Context): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(
        latitude,
        longitude,
        1
    )
    return addresses.getOrNull(0)?.locality ?: ""
}

fun LatLng.getCountry(context: Context): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses = geocoder.getFromLocation(
        latitude,
        longitude,
        1
    )
    return addresses.getOrNull(0)?.countryName ?: ""
}

fun LatLng.getCityCountry(context: Context) =
    "${getCity(context)}, ${getCountry(context)}"

fun LatLng?.getAddress(context: Context? = null, gCoder: Geocoder? = null): String {
    if (this == null) return ""
    val geocoder = if (gCoder == null && context != null) Geocoder(context, Locale.getDefault())
    else gCoder ?: return printCoordinates()

    val address =
        geocoder.getFromLocation(latitude, longitude, 1).getOrNull(0)
            ?: return printCoordinates()

    return address.getAddressLine(0)
}

fun LatLng.printCoordinates() = "${this.latitude}, ${this.longitude}"

fun LatLng?.countryCode(context: Context? = null, gCoder: Geocoder? = null): String {
    if (this == null) return "US"
    val geocoder = if (gCoder == null && context != null) Geocoder(context, Locale.getDefault())
    else gCoder ?: return "US"
    return geocoder.getFromLocation(latitude, longitude, 1).getOrNull(0)?.countryCode
        ?: return "US"
}