package com.cloudlevi.ping.ui.registration

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cloudlevi.ping.*
import com.cloudlevi.ping.databinding.FragmentRegisterBinding
import com.cloudlevi.ping.ui.registration.RegisterFragmentDirections.actionRegisterFragmentToLoginFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import com.cloudlevi.ping.ui.registration.RegisterFragmentEvent.*

@AndroidEntryPoint
class RegisterFragment: Fragment(R.layout.fragment_register) {

    private val viewModel: RegisterFragmentViewModel by viewModels()
    private lateinit var binding: FragmentRegisterBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentRegisterBinding.bind(view)

        binding.apply {
            registerUserName.addTextChangedListener {
                viewModel.registerUserName = it.toString().trim()
            }
            registerEmail.addTextChangedListener {
                viewModel.registerEmail = it.toString().trim()
            }
            registerPassword.addTextChangedListener {
                viewModel.registerPassword = it.toString().trim()
            }
            registerConfirmPassword.addTextChangedListener {
                viewModel.registerConfirmPassword = it.toString().trim()
            }
            registerButton.setOnClickListener {
                viewModel.onRegisterClick()
            }
            signInTextView.setOnClickListener {
                navigateToLoginScreen()
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.registerFragmentEvent.collect { event ->
                when (event){
                    is SendToastMessage -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                    }
                    is NavigateToLoginScreen -> {
                        navigateToLoginScreen()
                    }
                    is RequestErrorField -> {
                        binding.apply {
                            if(event.request_id == REQUEST_ERROR_EMAIL_FIELD) registerEmail.error = "Error"
                            if(event.request_id == REQUEST_ERROR_PASSWORD_FIELD) registerPassword.error = "Error"
                            if(event.request_id == REQUEST_ERROR_CONFIRM_PASSWORD_FIELD) registerConfirmPassword.error = "Error"
                            if(event.request_id == REQUEST_ERROR_USERNAME_FIELD) registerUserName.error = "Error"

                        }
                    }
                    is ChangeProgress -> changeProgress(event.status)
                }
            }
        }
    }

    private fun navigateToLoginScreen(){
        findNavController().navigate(actionRegisterFragmentToLoginFragment())
    }

    private fun changeProgress(status: Int){
        binding.apply {
            progressBar.visibility = status

            when(status){
                View.VISIBLE -> {
                    registerUserName.isEnabled = false
                    registerEmail.isEnabled = false
                    registerPassword.isEnabled = false
                    registerConfirmPassword.isEnabled = false
                    registerButton.isEnabled = false
                    signInTextView.isEnabled = false
                    mainRelativeLayout.foreground = ContextCompat.getDrawable(requireContext(), R.color.black_transparent)
                }
                View.GONE -> {
                    registerUserName.isEnabled = true
                    registerEmail.isEnabled = true
                    registerPassword.isEnabled = true
                    registerConfirmPassword.isEnabled = true
                    registerButton.isEnabled = true
                    signInTextView.isEnabled = true
                    mainRelativeLayout.foreground = null
                }
            }
        }
    }
}