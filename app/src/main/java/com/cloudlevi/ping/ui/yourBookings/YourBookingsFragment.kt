package com.cloudlevi.ping.ui.yourBookings

import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.core.transition.addListener
import androidx.core.transition.doOnEnd
import androidx.core.transition.doOnStart
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cloudlevi.ping.BaseFragment
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.RentalMode
import com.cloudlevi.ping.databinding.FragmentYourBookingsBinding
import com.cloudlevi.ping.di.GlideApp
import com.cloudlevi.ping.ext.LockableLLManager
import com.cloudlevi.ping.ext.removeAnimations
import com.cloudlevi.ping.ext.toggleAllViewsEnabled
import com.cloudlevi.ping.ext.visibleOrGone
import com.cloudlevi.ping.ui.userChat.MessageMediaAdapter
import com.cloudlevi.ping.ui.userChat.UserChatAdapter
import dagger.hilt.android.AndroidEntryPoint
import com.cloudlevi.ping.ui.yourBookings.YourBookingsViewModel.ActionType.*
import com.cloudlevi.ping.ui.yourBookings.YourBookingsViewModel.Action
import com.google.firebase.storage.StorageReference
import com.stfalcon.imageviewer.StfalconImageViewer
import java.util.concurrent.locks.Lock

@AndroidEntryPoint
class YourBookingsFragment : BaseFragment<FragmentYourBookingsBinding>
    (R.layout.fragment_your_bookings, true) {

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentYourBookingsBinding =
        FragmentYourBookingsBinding::inflate

    private lateinit var binding: FragmentYourBookingsBinding
    private val viewModel: YourBookingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val args = if (arguments != null) YourBookingsFragmentArgs.fromBundle(requireArguments())
        else null

        binding = FragmentYourBookingsBinding.inflate(inflater, container, false)
        viewModel.fragmentCreated(args?.rentalMode?: RentalMode.TENANT_MODE)

        viewModel.doAction.observe(viewLifecycleOwner) {
            val data = it.getDataSafely() ?: return@observe
            doAction(data)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        changeTitle(viewModel.determineFragmentTitle())

        binding.apply {
            bookingsRecycler.layoutManager = LockableLLManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            bookingsRecycler.adapter = viewModel.adapter
            bookingsRecycler.removeAnimations()
        }
    }

    private fun doAction(action: Action) {
        when (action.actionType) {
            TOGGLE_LOADING -> toggleLoading(action.bool ?: false)
            UPDATE_IMAGE -> notifyImageUpdated(action.pos ?: 0)
            NOTIFY_RECYCLER_RESIZE -> animateRecycler()
            OPEN_IMAGE_AT -> openImageAt(action.listPos?: 0, action.pos?: 0)
        }
    }

    private fun changeTitle(@StringRes resID: Int){
        binding.titleTV.setText(resID)
    }

    private fun openImageAt(listPos: Int, imagePos: Int) {
        val bookingHolder = (binding.bookingsRecycler.findViewHolderForAdapterPosition(listPos) as? YourBookingsAdapter.BookingVH)?: return
        val mediaHolder = (bookingHolder.binding.imagesRecycler.findViewHolderForAdapterPosition(imagePos) as? MessageMediaAdapter.MediaViewHolder)

        val imagesList = viewModel.bookingsList[listPos].aImagesList

        val imageLoader: (view: ImageView, imgRef: StorageReference) -> Unit = { view, imgRef ->
            GlideApp.with(view.context)
                .load(imgRef)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(view)
        }
        val imageChangeListener: (pos: Int) -> Unit = { pos ->
            bookingHolder.updateScrollPos(pos)
        }
        StfalconImageViewer.Builder(requireContext(), imagesList, imageLoader)
            .withStartPosition(imagePos)
            .withTransitionFrom(mediaHolder?.binding?.imageView)
            .withImageChangeListener(imageChangeListener)
            .withHiddenStatusBar(false)
            .show()
    }


    private fun animateRecycler() {
        binding.apply {
            val layoutManager = (bookingsRecycler.layoutManager as? LockableLLManager)
            val autoTransition = AutoTransition().also { transition ->
                transition.addListener {
                    it.doOnStart { layoutManager?.scrollLocked = true }
                    it.doOnEnd { layoutManager?.scrollLocked = false }
                }
            }

            TransitionManager.beginDelayedTransition(bookingsRecycler, autoTransition)
        }
    }

    private fun notifyImageUpdated(pos: Int) {
        Log.d("DEBUG", "notifyImageUpdated: pos:$pos")
        val holder =
            binding.bookingsRecycler.findViewHolderForAdapterPosition(pos) as? YourBookingsAdapter.BookingVH
                ?: return

        holder.notifyImageItemChanged(pos)
    }

    private fun toggleLoading(isLoading: Boolean) {
        binding.apply {
            progressLayout.visibleOrGone(isLoading)

            toggleAllViewsEnabled(!isLoading, binding.root)
        }
    }

}