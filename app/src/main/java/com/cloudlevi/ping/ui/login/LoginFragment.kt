package com.cloudlevi.ping.ui.login

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.iterator
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cloudlevi.ping.R
import com.cloudlevi.ping.REQUEST_ERROR_EMAIL_FIELD
import com.cloudlevi.ping.REQUEST_ERROR_PASSWORD_FIELD
import com.cloudlevi.ping.databinding.FragmentLoginBinding
import com.cloudlevi.ping.ui.login.LoginFragmentDirections.actionLoginFragmentToHomeFragment
import com.cloudlevi.ping.ui.login.LoginFragmentDirections.actionLoginFragmentToRegisterFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.cloudlevi.ping.ui.login.LoginFragmentEvent.*
import com.google.android.gms.tasks.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect


@AndroidEntryPoint
class LoginFragment: Fragment(R.layout.fragment_login) {

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private val viewModel: LoginFragmentViewModel by viewModels()
    private lateinit var binding: FragmentLoginBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentLoginBinding.bind(view)

//        viewModel.checkIfLoggedIn()

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result: ActivityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }

        viewModel.setupGSO(requireActivity(), requireContext())

        binding.apply {
            loginEditText.addTextChangedListener {
                viewModel.loginText = it.toString().trim()
            }
            passwordEditText.addTextChangedListener {
                viewModel.passwordText = it.toString().trim()
            }
            loginButton.setOnClickListener{
                viewModel.onLoginCLick()
            }
            signUpTextView.setOnClickListener {
                viewModel.onSignUpClick()
            }
            googleSignInButton.setOnClickListener {
                viewModel.onGoogleSignInClick()
            }
            googleSignInButton.setSize(SignInButton.SIZE_WIDE)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.loginFragmentEvent.collect { event ->
                when(event) {
                    is ShowToastMessage -> {
                        //Snackbar.make(requireView(), event.message, Snackbar.LENGTH_LONG).show()
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                    }

                    is NavigateToSignUpScreen -> {
                        findNavController().navigate(actionLoginFragmentToRegisterFragment())
                    }

                    is StartGoogleSignIn -> {
                        changeProgressStatus(View.VISIBLE)
                        startActivityForResult(event.intent)
                    }

                    is NavigateToHomeScreen -> {
                        findNavController().navigate(actionLoginFragmentToHomeFragment())
                    }

                    is RequestErrorField -> {
                        binding.apply {
                            if(event.request_id == REQUEST_ERROR_EMAIL_FIELD) loginEditText.error = "Error"
                            if(event.request_id == REQUEST_ERROR_PASSWORD_FIELD) passwordEditText.error = "Error"
                        }
                    }
                    is ChangeProgress -> {
                        changeProgressStatus(event.status)
                    }
                }

            }
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            viewModel.onGoogleSignInComplete(completedTask.getResult(ApiException::class.java)!!)
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            changeProgressStatus(View.GONE)
            Toast.makeText(requireContext(), "Failed to sign in.", Toast.LENGTH_LONG).show()
        }
    }

    private fun changeProgressStatus(status: Int){
        binding.apply {
            progressBar.visibility = status

            when(status){
                View.VISIBLE -> {
                    loginEditText.isEnabled = false
                    passwordEditText.isEnabled = false
                    loginButton.isEnabled = false
                    googleSignInButton.isEnabled = false
                    signUpTextView.isEnabled = false
                    mainRelativeLayout.foreground = ContextCompat.getDrawable(requireContext(), R.color.black_transparent)
                }
                View.GONE -> {
                    loginEditText.isEnabled = true
                    passwordEditText.isEnabled = true
                    loginButton.isEnabled = true
                    googleSignInButton.isEnabled = true
                    signUpTextView.isEnabled = true
                    mainRelativeLayout.foreground = null
                }
            }
        }
    }

    private fun startActivityForResult(intent: Intent){
        resultLauncher.launch(intent)
    }
}