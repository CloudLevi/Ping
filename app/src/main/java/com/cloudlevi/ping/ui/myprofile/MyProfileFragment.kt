package com.cloudlevi.ping.ui.myprofile

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.cloudlevi.ping.R
import com.cloudlevi.ping.databinding.FragmentMyProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import com.cloudlevi.ping.ui.myprofile.MyProfileFragmentEvent.*
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class MyProfileFragment: Fragment(R.layout.fragment_my_profile) {

    private val viewModel: MyProfileFragmentViewModel by activityViewModels()
    private lateinit var binding: FragmentMyProfileBinding

    private lateinit var byteArrayData: ByteArray
    private lateinit var imageResultLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.fragmentCreate()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentMyProfileBinding.bind(view)

        binding.profileImage.setImageResource(R.drawable.ic_profile_picture)

        imageResultLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

                Glide.with(this)
                    .asBitmap()
                    .load(uri)
                    .placeholder(R.drawable.progress_animation_small)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            val baos = ByteArrayOutputStream()
                            resource.compress(Bitmap.CompressFormat.JPEG, 25, baos)
                            byteArrayData = baos.toByteArray()
                            viewModel.handleFinishedImageIntent(uri, byteArrayData)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            Log.d(ContentValues.TAG, "Load Cleared")
                        }
                    })
            }

        binding.apply {

            determineChangePasswordLayoutVisibility()

            logoutButton.setOnClickListener {
                viewModel.onLogoutButtonClicked()

            }

            greetingsTV.text = "Hello, ${viewModel.displayName}"

            addPostButton.setOnClickListener {
                findNavController().navigate(MyProfileFragmentDirections.actionMyProfileFragmentToAddPostFragment())
            }

            myPostsLayout.setOnClickListener {
                val action = MyProfileFragmentDirections.actionMyProfileFragmentToUserPostsFragment(viewModel.getUserModel())
                findNavController().navigate(action)
            }

            changeDisplayNameLayout.setOnClickListener {
                findNavController().navigate(MyProfileFragmentDirections.actionMyProfileFragmentToChangeDisplayNameFragment())
            }

            profileImage.setOnClickListener {
                imageResultLauncher.launch("image/*")
            }
        }

        viewModel.imageUriLiveData.observe(viewLifecycleOwner){
            loadProfilePic(it)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.myProfileFragmentEvent.collect { event ->
                when(event) {
                    is NavigateToLoginScreen -> {
                        navigateToLoginScreen()
                    }
                    is UpdateUserName -> binding.greetingsTV.text = event.userName
                    is SendToastMessage -> Toast
                        .makeText(requireContext(), event.message, Toast.LENGTH_LONG)
                        .show()
                    is ProfileImageUpdated -> loadProfilePic(event.uri)
                }

            }
        }
    }

    private fun loadProfilePic(uri: Uri?){
        Glide
            .with(requireContext())
            .load(uri)
            .centerCrop()
            .into(binding.profileImage)
    }

    private fun determineChangePasswordLayoutVisibility() {
        if (viewModel.loggedThroughGoogle)
            if (this::binding.isInitialized) binding.changePasswordLayout.visibility = View.GONE
    }

    private fun navigateToLoginScreen(){
        findNavController().navigate(R.id.action_myProfileFragment_to_loginFragment)
    }
}