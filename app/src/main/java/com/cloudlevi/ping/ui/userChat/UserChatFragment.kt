package com.cloudlevi.ping.ui.userChat

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cloudlevi.ping.BaseFragment
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.databinding.FragmentUserChatBinding
import com.cloudlevi.ping.di.GlideApp
import com.cloudlevi.ping.ext.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import com.cloudlevi.ping.ui.userChat.UserChatViewModel.UserChatEvent.*
import com.cloudlevi.ping.ui.yourBookings.YourBookingsAdapter
import com.google.firebase.storage.StorageReference
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader

@AndroidEntryPoint
class UserChatFragment :
    BaseFragment<FragmentUserChatBinding>
        (R.layout.fragment_user_chat, false) {

    private lateinit var binding: FragmentUserChatBinding
    private val viewModel: UserChatViewModel by viewModels()

    private var singleImageLauncher: ActivityResultLauncher<String>? = null
    private var multipleImagesResultLauncher: ActivityResultLauncher<String>? = null

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentUserChatBinding =
        FragmentUserChatBinding::inflate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentUserChatBinding.inflate(inflater, container, false)

        val args = UserChatFragmentArgs.fromBundle(requireArguments())

        viewModel.fragmentCreate(args.userModel, args.chatListItem)

        singleImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                processIntentImage(requireContext(), uri) { processedUri, byteArray ->
                    viewModel.imageReplacementReceived(processedUri, byteArray)
                }
            }

        multipleImagesResultLauncher =
            registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { list ->

                val leftCells = 10 - viewModel.attachmentsList.size

                val maxAmount = if (list.size > leftCells) leftCells
                else list.size

                list.forEachIndexed { index, uri ->
                    if (index < maxAmount) {
                        processIntentImage(requireContext(), uri) { processedUri, byteArray ->
                            viewModel.imageAttachmentReceived(processedUri, byteArray)
                        }
                    }
                }
            }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            if (messageEditText.text.isNullOrEmpty()) sendBtn.visibility = View.GONE

            messageEditText.addTextChangedListener {
                sendBtn.visibility = if (it.isNullOrEmpty()) View.GONE
                else View.VISIBLE
            }

            sendBtn.setOnClickListener {
                val msg = binding.messageEditText.text.toString().trim()
                if (msg.isEmpty()) {
                    makeLongToast("Message can't be empty")
                    return@setOnClickListener
                }

                toggleProgress(viewModel.attachmentsList.isNotEmpty())
                viewModel.sendMessage(msg)

                binding.messageEditText.setText("")
            }
            chatRecycler.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false).also {
                    it.stackFromEnd = true
                }
            chatRecycler.adapter = viewModel.userChatAdapter
            chatRecycler.removeAnimations()

            attachmentsRecycler.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            attachmentsRecycler.adapter = viewModel.attachmentsAdapter

            backBtn.setOnClickListener { findNavController().popBackStack() }

            attachBtn.setOnClickListener {
                if (viewModel.attachmentsList.size == 10) sendLongToast("Limit reached (10 images)")
                else multipleImagesChooser()
            }
        }

        viewModel.receiverModel.observe(viewLifecycleOwner) {
            it ?: return@observe
            setUserData(it)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.userChatEvent.collect {
                when (it) {
                    is AttachmentVisibility -> attachmentVisibility(it.isVisible)
                    is AddClick -> multipleImagesChooser()
                    is ChooseSingleImage -> singleImageChooser()
                    is ToggleLoading -> toggleProgress(it.isLoading)
                    is ImageUploadFinished -> imageUploadFinished()
                    is UpdateMessages -> updateMessages()
                    is OpenImageAt -> openImageAt(it.messagePos, it.startPos, it.imagesList)
                    is NotifyImageChanged -> notifyImageChanged(it.adapterPos)
                }
            }
        }
    }

    private fun notifyImageChanged(pos: Int){
        val holder = binding.chatRecycler.findViewHolderForAdapterPosition(pos) as? UserChatAdapter.MessageVH?: return

        holder.notifyImageItemChanged(pos)
    }

    private fun openImageAt(messagePos: Int, startPos: Int, imagesList: List<StorageReference>) {

        val holder = (binding.chatRecycler.findViewHolderForAdapterPosition(messagePos) as? UserChatAdapter.MessageVH)
        val mediaHolder = (holder?.imagesRecycler?.findViewHolderForAdapterPosition(startPos) as? MessageMediaAdapter.MediaViewHolder)

        val imageLoader: (view: ImageView, imageRef: StorageReference) -> Unit = { view, imageRef ->
            GlideApp.with(view.context)
                .load(imageRef)
                .error(R.drawable.placeholder)
                .into(view)
        }
        val imageChangeListener: (pos: Int) -> Unit = { pos ->
            holder?.updateSavedScrollPos(pos)
        }
        StfalconImageViewer.Builder(requireContext(), imagesList, imageLoader)
            .withStartPosition(startPos)
            .withTransitionFrom(mediaHolder?.binding?.imageView)
            .withImageChangeListener(imageChangeListener)
            .withHiddenStatusBar(false)
            .show()
    }

    private fun updateMessages() {
        viewModel.userChatAdapter?.update()
        binding.chatRecycler.post {
            val itemCount = viewModel.userChatAdapter?.itemCount?: 0
            binding.chatRecycler.smoothScrollToPosition(if (itemCount > 0) itemCount - 1 else 0)
        }
    }

    private fun imageUploadFinished() {
        toggleProgress(false)
        attachmentVisibility(false)
        viewModel.attachmentsList.clear()
        viewModel.attachmentsAdapter.update()
    }

    private fun attachmentVisibility(isVisible: Boolean) {
        binding.apply {
            if (isVisible) {
                attachmentsRecycler.makeVisible()
                recyclerDivider.makeVisible()
            } else {
                attachmentsRecycler.makeGone()
                recyclerDivider.makeGone()
            }
        }
    }

    private fun setUserData(user: User) {
        binding.apply {
            loadUserImage(viewModel.getReceiverImageURL())
            userNameTV.text = user.displayName
            userStatusTV.text = if (user.userOnline == true) {
                setStatusDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_online)
                )
                getString(R.string.online)
            } else {
                setStatusDrawable(null)
                getString(R.string.offline)
            }
        }
    }

    private fun setStatusDrawable(drawable: Drawable?) {
        binding.userStatusTV.setCompoundDrawablesWithIntrinsicBounds(
            drawable, null, null, null
        )
        binding.userStatusTV.compoundDrawablePadding = dpToPx(requireContext(), 4)
    }

    override fun onDestroy() {
        viewModel.onDestroy()
        super.onDestroy()
    }

    private fun singleImageChooser() {
        singleImageLauncher?.launch("image/*")
    }

    private fun multipleImagesChooser() {
        multipleImagesResultLauncher?.launch("image/*")
    }

    private fun loadUserImage(storageRef: StorageReference?) {
        GlideApp.with(requireContext())
            .load(storageRef)
            .centerCrop()
            .error(R.drawable.ic_profile_picture)
            .into(binding.profileImage)
    }

    private fun makeLongToast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    private fun toggleProgress(isVisible: Boolean) {
        binding.apply {
            if (isVisible) progressLayout.makeVisible()
            else progressLayout.makeGone()

            backBtn.isEnabled = !isVisible
            attachBtn.isEnabled = !isVisible
            messageEditText.isEnabled = !isVisible
            sendBtn.isEnabled = !isVisible
            attachmentsRecycler.children.forEach {
                it.isEnabled = !isVisible
            }
        }
    }
}