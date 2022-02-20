package com.cloudlevi.ping.ui.myprofile

import android.content.ContentValues
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.RentalMode
import com.cloudlevi.ping.databinding.FragmentMyProfileBinding
import com.cloudlevi.ping.di.GlideApp
import com.cloudlevi.ping.ext.LanguageDialogListener
import com.cloudlevi.ping.ext.getPosForCurrency
import com.cloudlevi.ping.ext.getPosForLanguage
import com.cloudlevi.ping.ext.showPickerDialog
import com.cloudlevi.ping.ui.addPost.AddPostFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import com.cloudlevi.ping.ui.myprofile.MyProfileFragmentEvent.*
import java.io.ByteArrayOutputStream
import com.cloudlevi.ping.ui.myprofile.MyProfileFragmentViewModel.ActionType.*
import com.cloudlevi.ping.ui.myprofile.MyProfileFragmentViewModel.Action
import com.google.firebase.storage.StorageReference

@AndroidEntryPoint
class MyProfileFragment :
    BaseFragment<FragmentMyProfileBinding>
        (R.layout.fragment_my_profile, true) {

    private val viewModel: MyProfileFragmentViewModel by viewModels()
    private val mAVM: MainActivityViewModel by activityViewModels()

    private lateinit var binding: FragmentMyProfileBinding

    private lateinit var byteArrayData: ByteArray
    private lateinit var imageResultLauncher: ActivityResultLauncher<String>

    private var sharedPrefs: SharedPreferences? = null

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentMyProfileBinding =
        FragmentMyProfileBinding::inflate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        viewModel.fragmentCreate()

        binding = FragmentMyProfileBinding.inflate(inflater, container, false)

        sharedPrefs = requireContext().getSharedPreferences(SHARED_PREFERENCES_KEY, 0)
        viewModel.action.observe(viewLifecycleOwner) {
            val data = it.getDataSafely() ?: return@observe
            doAction(data)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(
            "TAG",
            "onViewCreated: savedCurrency: ${mAVM.getSelectedCurrency()}, savedExchange: ${mAVM.getExRate()} "
        )

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
                googleLogout()
                viewModel.onLogoutButtonClicked()
                (requireActivity() as MainActivity).setUserOnline(false)
                val addPostVM =
                    ViewModelProvider(requireActivity() as MainActivity)[AddPostFragmentViewModel::class.java]
                addPostVM.clearFields()
            }

            greetingsTV.text = getString(R.string.hello_user, viewModel.displayName)

            addPostButton.setOnClickListener {
                findNavController().navigate(MyProfileFragmentDirections.actionMyProfileFragmentToAddPostFragment())
            }

            myPostsLayout.setOnClickListener {
                val action = MyProfileFragmentDirections.actionMyProfileFragmentToUserPostsFragment(
                    viewModel.getUserModel(),
                    null
                )
                findNavController().navigate(action)
            }

            changeDisplayNameLayout.setOnClickListener {
                findNavController().navigate(MyProfileFragmentDirections.actionMyProfileFragmentToChangeDisplayNameFragment())
            }

            changeDisplayNameLayout.setOnClickListener {
                findNavController().navigate(MyProfileFragmentDirections.actionMyProfileFragmentToChangeDisplayNameFragment())
            }

            yourBookingsLayout.setOnClickListener {
                val action =
                    MyProfileFragmentDirections.actionMyProfileFragmentToYourBookingsFragment(
                        RentalMode.TENANT_MODE
                    )
                findNavController().navigate(action)
            }

            yourRentalsLayout.setOnClickListener {
                val action =
                    MyProfileFragmentDirections.actionMyProfileFragmentToYourBookingsFragment(
                        RentalMode.LANDLORD_MODE
                    )
                findNavController().navigate(action)
            }

            changeLanguageLayout.setOnClickListener {
                showPickerDialog(
                    requireContext(),
                    R.string.change_language,
                    R.array.language_array,
                    getPosForLanguage(
                        requireContext(),
                        sharedPrefs?.getString("language_code", "en") ?: "en"
                    ),
                    object : LanguageDialogListener {
                        override fun onPositiveClick(pos: Int) {
                            languagePickerReceived(pos)
                        }
                    })
            }

            currencyText.text = mAVM.getSelectedCurrency()

            changeCurrencyLayout.setOnClickListener {
                showPickerDialog(
                    requireContext(),
                    R.string.change_currency,
                    R.array.array_currency_codes,
                    getPosForCurrency(requireContext(), mAVM.getSelectedCurrency()),
                    object : LanguageDialogListener {
                        override fun onPositiveClick(pos: Int) {
                            currencyPickerReceived(pos)
                        }
                    })
            }

            profileImage.setOnClickListener {
                imageResultLauncher.launch("image/*")
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.myProfileFragmentEvent.collect { event ->
                when (event) {
                    is NavigateToLoginScreen -> {
                        navigateToLoginScreen()
                    }
                    is UpdateUserName -> binding.greetingsTV.text = getString(R.string.hello_user, event.userName)
                    is SendToastMessage -> Toast
                        .makeText(requireContext(), event.message, Toast.LENGTH_LONG)
                        .show()
                }

            }
        }
    }

    private fun doAction(a: Action) {
        when (a.type) {
            TOGGLE_LOADING -> switchActivityLoading(a.bool ?: false)
            CURRENCY_RECEIVED -> currencyReceived(a.string ?: "$")
            CURRENCY_CALL_FAILED -> currencyCallFailed()
            LOAD_IMAGE -> loadProfilePic(a.storageRef)
        }
    }

    private fun currencyReceived(currencyCode: String) {
        switchActivityLoading(false)
        binding.currencyText.text = currencyCode
    }

    private fun currencyCallFailed() {
        switchActivityLoading(false)
        sendLongToast(R.string.currency_failed_toast)
    }

    private fun switchActivityLoading(isLoading: Boolean) {
        (requireActivity() as? MainActivity)?.switchLoading(isLoading)
    }

    private fun languagePickerReceived(pos: Int) {
        val code = when (pos) {
            0 -> "en"
            1 -> "pl"
            2 -> "ru"
            else -> "en"
        }
        (requireActivity() as MainActivity).setLocale(code)
        findNavController().navigate(R.id.myProfileFragment)
    }

    private fun currencyPickerReceived(pos: Int) {
        val array = resources.getStringArray(R.array.array_currency_codes)
        val currency = array[pos]
        viewModel.getExchangeRate(currency)
    }

    private fun loadProfilePic(storageRef: StorageReference?) {
        GlideApp.with(requireContext())
            .load(storageRef)
            .centerCrop()
            .error(R.drawable.ic_profile_picture)
            .placeholder(R.drawable.ic_profile_picture)
            .into(binding.profileImage)
    }

    private fun determineChangePasswordLayoutVisibility() {
        if (viewModel.loggedThroughGoogle)
            if (this::binding.isInitialized) binding.changePasswordLayout.visibility = View.GONE
    }

    private fun navigateToLoginScreen() {
        findNavController().navigate(R.id.action_myProfileFragment_to_loginFragment)
    }
}