package com.cloudlevi.ping.ui.userPosts

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.databinding.FragmentUserPostsBinding
import dagger.hilt.android.AndroidEntryPoint
import com.cloudlevi.ping.ui.userPosts.UserPostsEvent.*
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class UserPostsFragment:
    Fragment(R.layout.fragment_user_posts),
    UserPostsAdapter.OnPostClickedListener{

    private lateinit var binding: FragmentUserPostsBinding
    private val viewModel: UserPostsViewModel by viewModels()
    private val adapter = UserPostsAdapter(arrayListOf(), this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentUserPostsBinding.bind(view)

        if (arguments != null){
            viewModel.getPosts(UserPostsFragmentArgs.fromBundle(requireArguments()).userModel)
        }

        binding.apply {
            userListsRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            userListsRecycler.setHasFixedSize(true)
            userListsRecycler.adapter = adapter

            viewLifecycleOwner.lifecycleScope.launchWhenCreated {
                viewModel.homePostsLiveData.observe(viewLifecycleOwner){
                    adapter.apartmentList = it
                    adapter.notifyDataSetChanged()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.userPostsEvent.collect { event ->
                when(event){
                    is SendToastMessage -> Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
                    is DisplayUserInfo -> {
                        if (event.status) loadUserInfo(event.userModel)
                    }
                }
            }
        }
    }

    private fun loadUserInfo(userModel: User){
        binding.landLordName.visibility = View.VISIBLE
        binding.landLordName.text = userModel.displayName

        binding.landLordUserName.visibility = View.VISIBLE
        binding.landLordUserName.text = userModel.username

        binding.profileImage.visibility = View.VISIBLE
        Glide.with(requireContext())
            .load(userModel.imageUrl)
            .centerCrop()
            .placeholder(R.drawable.progress_animation_small)
            .into(binding.profileImage)
    }

    override fun onItemClickedListener(aptID: String) {
        val action = UserPostsFragmentDirections.actionUserPostsFragmentToApartmentPageFragment(aptID)
        action.fromUserLists = true
        findNavController().navigate(action)
    }
}