package com.cloudlevi.ping.ui.userPosts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.cloudlevi.ping.BaseFragment
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.databinding.FragmentUserPostsBinding
import com.cloudlevi.ping.ext.makeVisible
import com.cloudlevi.ping.ext.toggleAllViewsEnabled
import com.cloudlevi.ping.ext.visibleOrGone
import dagger.hilt.android.AndroidEntryPoint
import com.cloudlevi.ping.ui.userPosts.UserPostsEvent.*
import kotlinx.coroutines.flow.collect
import com.cloudlevi.ping.ui.userPosts.UserPostsViewModel.Action.*
import com.cloudlevi.ping.ui.userPosts.UserPostsViewModel.ActionType.*

@AndroidEntryPoint
class UserPostsFragment :
    BaseFragment<FragmentUserPostsBinding>(R.layout.fragment_user_posts, true),
    UserPostsAdapter.OnPostClickedListener {

    private lateinit var binding: FragmentUserPostsBinding
    private val viewModel: UserPostsViewModel by viewModels()
    private val adapter = UserPostsAdapter(arrayListOf(), this)

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentUserPostsBinding =
        FragmentUserPostsBinding::inflate


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentUserPostsBinding.inflate(inflater, container, false)
        viewModel.fragmentCreate()

        if (arguments != null) {
            val args = UserPostsFragmentArgs.fromBundle(requireArguments())
            val userModel = args.userModel
            val userID = args.userID

            when {
                userModel != null -> viewModel.getPosts(userModel)
                userID != null -> viewModel.getPosts(userID)
                else -> sendLongToast(getString(R.string.nothing_found))
            }
        }

        viewModel.action.observe(viewLifecycleOwner) {
            val data = it.getDataSafely() ?: return@observe
            doAction(data)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            userListsRecycler.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            userListsRecycler.setHasFixedSize(true)
            userListsRecycler.adapter = adapter

            sendMessage.setOnClickListener {
                val action = UserPostsFragmentDirections.actionUserPostsFragmentToUserChatFragment(
                    viewModel.otherUserModel, null
                )
                findNavController().navigate(action)
            }
        }
    }

    private fun doAction(a: UserPostsViewModel.Action) {
        when (a.type) {
            TOGGLE_LOADING -> toggleLoading(a.bool ?: false)
            LIST_UPDATED -> listUpdated()
            SEND_TOAST -> sendLongToast(a.string ?: "")
        }
    }

    private fun listUpdated() {
        loadUserInfo(viewModel.otherUserModel)
        adapter.apartmentList = ArrayList(viewModel.currentUserLists)
        adapter.notifyDataSetChanged()

        toggleLoading(false)
    }

    private fun toggleLoading(isLoading: Boolean) {
        binding.apply {
            toggleAllViewsEnabled(!isLoading, root)
            progressLayout.visibleOrGone(isLoading)
        }
    }


    private fun loadUserInfo(userModel: User?) {
        binding.apply {
            if (userModel == null || userModel.userID == viewModel.currentUserID) {
                titleTV.text = getString(R.string.my_posts)
                return
            }
            landLordName.text = userModel.displayName
            landLordUserName.text = userModel.username
            titleTV.text = getString(R.string.user_posts)

            userInfoLayout.makeVisible()

            Glide.with(requireContext())
                .load(userModel.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.progress_animation_small)
                .into(profileImage)
        }
    }

    override fun onItemClickedListener(apHomePost: ApartmentHomePost) {
        val action =
            UserPostsFragmentDirections.actionUserPostsFragmentToApartmentPageFragment(apHomePost)
        action.fromUserLists = true
        findNavController().navigate(action)
    }
}