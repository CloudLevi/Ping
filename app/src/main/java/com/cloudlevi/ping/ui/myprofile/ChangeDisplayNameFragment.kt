package com.cloudlevi.ping.ui.myprofile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cloudlevi.ping.R
import com.cloudlevi.ping.databinding.FragmentChangeDisplaynameBinding
import dagger.hilt.android.AndroidEntryPoint
import com.cloudlevi.ping.ui.myprofile.MyProfileFragmentEvent.*
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class ChangeDisplayNameFragment: Fragment(R.layout.fragment_change_displayname) {

    private val viewModel: MyProfileFragmentViewModel by activityViewModels()
    private lateinit var binding: FragmentChangeDisplaynameBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentChangeDisplaynameBinding.bind(view)

        binding.apply {
            nameEditText.apply {
                addTextChangedListener {
                    viewModel.displayName = it.toString().trim()
                    val characterCount = "${it?.length ?: 0}/35"
                    nameCharacterCount.text = characterCount
                }
                setText(viewModel.displayName)
            }

            applyButton.setOnClickListener {
                if (nameEditText.text.toString().isEmpty())
                    sendToastMessage("Name can't be empty!")
                else viewModel.onApplyClicked()
            }

            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                viewModel.myProfileFragmentEvent.collect { event ->
                    when(event){
                        is DisplayNameChanged -> findNavController()
                            .navigate(ChangeDisplayNameFragmentDirections
                                .actionChangeDisplayNameFragmentToMyProfileFragment())
                        is SendDisplayNameToastMessage -> sendToastMessage(event.message)
                    }
                }
            }
        }
    }

    private fun sendToastMessage(message: String){
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}