package com.cloudlevi.ping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.cloudlevi.ping.ext.toggleAllViewsEnabled
import com.google.android.gms.maps.MapFragment

abstract class BaseFragment<VB : ViewBinding>(
    @LayoutRes layoutID: Int,
    private val showNavigation: Boolean?
) : Fragment(layoutID) {

    private lateinit var binding: VB

    abstract val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = bindingInflater.invoke(inflater, container, false)

        if (showNavigation == true)
            getMainActivity().showNavigation()
        else getMainActivity().hideNavigation()

        return binding.root
    }

    fun switchLoading(isLoading: Boolean, progressText: String = "") {
        getMainActivity().switchLoading(isLoading, progressText)
    }

    fun changeLoadingText(progressText: String) {
        getMainActivity().changeLoadingText(progressText)
    }

    fun sendShortToast(message: String) {
        if (message.trim().isEmpty()) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun sendLongToast(message: String) {
        if (message.trim().isEmpty()) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    fun getGoogleSignInIntent() = getMainActivity().googleSignInClient.signInIntent

    fun googleLogout() = getMainActivity().googleSignInClient.signOut()

    fun sendLongToast(@StringRes resID: Int) {
        Toast.makeText(requireContext(), getString(resID), Toast.LENGTH_LONG).show()
    }

    fun getMainActivity() = requireActivity() as MainActivity

    protected fun getColor(@ColorRes resID: Int) = ContextCompat.getColor(requireContext(), resID)
}