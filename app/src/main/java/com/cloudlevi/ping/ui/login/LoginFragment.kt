package com.cloudlevi.ping.ui.login

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cloudlevi.ping.*
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
import com.cloudlevi.ping.ui.login.ActionType.*

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>(R.layout.fragment_login, false) {

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private val viewModel: LoginFragmentViewModel by viewModels()
    private lateinit var binding: FragmentLoginBinding

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentLoginBinding =
        FragmentLoginBinding::inflate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        viewModel.action.observe(viewLifecycleOwner) {
            val data = it.getDataSafely()?: return@observe
            doAction(data)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        viewModel.checkIfLoggedIn()

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            }

        //viewModel.setupGSO(requireActivity(), requireContext())

        binding.apply {
            loginEditText.addTextChangedListener {
                viewModel.loginText = it.toString().trim()
            }
            passwordEditText.addTextChangedListener {
                viewModel.passwordText = it.toString().trim()
            }
            loginButton.setOnClickListener {
                viewModel.onLoginClick()
            }
            signUpTextView.setOnClickListener {
                navigateToSignUpScreen()
            }
            googleSignInButton.setOnClickListener {
                //viewModel.onGoogleSignInClick()
                changeProgressStatus(View.VISIBLE)
                startActivityForResult(getGoogleSignInIntent())
            }
            googleSignInButton.setSize(SignInButton.SIZE_WIDE)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.loginFragmentEvent.collect { event ->
                when (event) {
                    is ShowToastMessage -> {
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                    }

                    is RequestErrorField -> {
                        binding.apply {
                            if (event.request_id == REQUEST_ERROR_EMAIL_FIELD) loginEditText.error =
                                "Error"
                            if (event.request_id == REQUEST_ERROR_PASSWORD_FIELD) passwordEditText.error =
                                "Error"
                        }
                    }
                    is ChangeProgress -> {
                        changeProgressStatus(event.status)
                    }
                }

            }
        }
    }

    private fun navigateToSignUpScreen() {
        findNavController().navigate(actionLoginFragmentToRegisterFragment())
    }

    private fun navigateToHomeScreen() {
        getMainActivity().setUserOnline(true)
        findNavController().navigate(actionLoginFragmentToHomeFragment())
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

    private fun doAction(a: Action){
        when(a.type){
            NAVIGATE_TO_HOME_SCREEN -> navigateToHomeScreen()
        }
    }

    private fun changeProgressStatus(status: Int) {
        binding.apply {
            progressBar.visibility = status

            when (status) {
                View.VISIBLE -> {
                    loginEditText.isEnabled = false
                    passwordEditText.isEnabled = false
                    loginButton.isEnabled = false
                    googleSignInButton.isEnabled = false
                    signUpTextView.isEnabled = false
                    mainRelativeLayout.foreground =
                        ContextCompat.getDrawable(requireContext(), R.color.black_transparent)
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

    private fun startActivityForResult(intent: Intent) {
        resultLauncher.launch(intent)
    }
}