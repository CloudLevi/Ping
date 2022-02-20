package com.cloudlevi.ping.ui.addPost

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.cloudlevi.ping.databinding.DialogMapBinding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import java.util.*
import android.view.WindowManager
import android.view.Gravity
import android.util.DisplayMetrics
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.cloudlevi.ping.ext.*
import java.io.IOException

class MapDialogFragment : DialogFragment(), OnMapReadyCallback {

    private lateinit var binding: DialogMapBinding
    private var googleMap: GoogleMap? = null
    private var location: LatLng? = null
    private lateinit var geocoder: Geocoder

    override fun onStart() {
        super.onStart()
        val dialog: Dialog = dialog ?: return
        val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)

        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        dialog.window?.apply {
            setGravity(Gravity.START or Gravity.TOP)
            val attributes = this.attributes
            attributes.x = dpToPx(requireContext(), 16)
            attributes.y = (dpToPx(requireContext(), 80))

            setAttributes(attributes)

            val width: Int = screenWidth - dpToPx(requireContext(), 32)
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        geocoder = Geocoder(requireContext(), Locale.getDefault())

        binding = DialogMapBinding.inflate(inflater, container, false)

        val args = requireArguments()
        location = args.get(LAT_LONG) as? LatLng

        binding.apply {
            mapView.onCreate(savedInstanceState)
            mapView.getMapAsync(this@MapDialogFragment)

            searchEditText.setOnEditorActionListener { textView, i, keyEvent ->
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    getAddressesByQuery(textView.text.toString().trim())
                    return@setOnEditorActionListener true
                }
                false
            }

            posBtn.setOnClickListener {
                if (location == null) {
                    mapError.makeVisible()
                    return@setOnClickListener
                }
                setFragmentResult(APPLY, bundleOf(LAT_LONG to location))
                dismiss()
            }

            negBtn.setOnClickListener {
                setFragmentResult(CANCEL, bundleOf())
                dismiss()
            }
        }

        return binding.root
    }

    private fun getAddressesByQuery(query: String) {
        if (query.isNotEmpty()) {
            try {
                val firstResult = geocoder
                    .getFromLocationName(query, 1)
                    .getOrNull(0)

                if (firstResult == null) {
                    location = null
                    sendLongToast("Nothing found!")
                } else {
                    val latLng = LatLng(firstResult.latitude, firstResult.longitude)
                    googleMap?.setNewMarkerCustom(latLng)?.showInfoWindow()
                    location = latLng
                }
            } catch (e: IOException) {
                e.printStackTrace()
                location = null
                sendLongToast("Nothing found!")
            }
        }
    }

    private fun sendLongToast(text: String) =
        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()

    companion object {
        const val DIALOG_TAG = "mapDialogFragment"
        const val APPLY = "apply_key"
        const val CANCEL = "cancel_key"
        const val LAT_LONG = "lat_long"

        fun createFragment(latLng: LatLng?): MapDialogFragment = MapDialogFragment().also { f ->
            f.arguments = bundleOf(LAT_LONG to latLng)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.apply {
            mapType = GoogleMap.MAP_TYPE_NORMAL

            if (location != null) setNewMarkerCustom(location!!)?.showInfoWindow()

            setOnMapClickListener { latLng ->
                val marker = setNewMarkerCustom(latLng)
                location = latLng
                val title =
                    marker?.getAddress(requireContext()) ?: return@setOnMapClickListener
                marker.title = title
                marker.showInfoWindow()
            }

            setOnMarkerClickListener { marker ->
                val title =
                    marker.getAddress(requireContext()) ?: return@setOnMarkerClickListener false
                marker.title = title
                marker.showInfoWindow()
                true
            }
        }
        googleMap?.uiSettings?.apply {
            isCompassEnabled = true
            isZoomControlsEnabled = true
            isRotateGesturesEnabled = true
            isScrollGesturesEnabledDuringRotateOrZoom = true
            setAllGesturesEnabled(true)
        }
    }

    override fun onResume() {
        binding.mapView.onResume()
        super.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        binding.mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        binding.mapView.onLowMemory()
        super.onLowMemory()
    }

}