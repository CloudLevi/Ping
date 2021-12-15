package com.cloudlevi.ping.ui.chats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.cloudlevi.ping.BaseFragment
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.ChatListItem
import com.cloudlevi.ping.databinding.FragmentChatsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import com.cloudlevi.ping.ui.chats.ChatsViewModel.UserChatsEvent.*

@AndroidEntryPoint
class ChatsFragment :
    BaseFragment<FragmentChatsBinding>(R.layout.fragment_chats, true) {

    private lateinit var binding: FragmentChatsBinding
    private val viewModel: ChatsViewModel by viewModels()

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentChatsBinding =
        FragmentChatsBinding::inflate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentChatsBinding.inflate(inflater, container, false)

        switchLoading(true)
        viewModel.fragmentCreate()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isLoading.observe(viewLifecycleOwner) {
            switchLoading(it)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.userChatsEvent.collect {
                when (it) {
                    is OpenChat -> openChat(it.chatListItem)
                    is SendMessage -> sendLongToast(it.message)
                }
            }
        }

        binding.apply {
            messagesRecycler.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            messagesRecycler.adapter = viewModel.chatsAdapter

            (messagesRecycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }
    }

    private fun openChat(chatItem: ChatListItem?) {
        val action = ChatsFragmentDirections.actionChatsFragmentToUserChatFragment(
            chatItem?.userModel, chatItem
        )
        findNavController().navigate(action)
    }

    override fun onDestroy() {
        switchLoading(false)
        viewModel.onDestroy()
        super.onDestroy()
    }
}