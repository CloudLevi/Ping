package com.cloudlevi.ping.ui.chats

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.ChatListItem
import com.cloudlevi.ping.databinding.ItemChatBinding
import com.cloudlevi.ping.di.GlideApp
import com.cloudlevi.ping.ext.*

class ChatsAdapter(val vm: ChatsViewModel) : RecyclerView.Adapter<ChatsAdapter.ChatsViewHolder>() {

    private var currentList: MutableList<ChatListItem?> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsViewHolder {
        return ChatsViewHolder(
            ItemChatBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ChatsViewHolder, position: Int) {
        val chatListItem = currentList.getOrNull(position) ?: return
        holder.bind(chatListItem)
    }

    override fun getItemCount() = currentList.size

    @SuppressLint("NotifyDataSetChanged")
    fun update() {
        val oldListSize = currentList.size
        vm.isLoading.value = false
        currentList = ArrayList(vm.allChats)
        val newListSize = currentList.size

        if (oldListSize == newListSize)
            notifyItemRangeChanged(0, itemCount)
        else notifyDataSetChanged()
    }

    fun updateSoft(pos: Int) {
        vm.isLoading.value = false
        currentList = ArrayList(vm.allChats)
        notifyItemChanged(pos)
    }

    inner class ChatsViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                vm.chatClicked(currentList.getOrNull(adapterPosition))
                currentList.getOrNull(adapterPosition)?.messages?.forEach {
                    if (it.senderID != vm.userID) {
                        it.read = true
                        currentList[adapterPosition]?.decreaseUnreadCount()
                    }
                }
            }
        }

        fun bind(chatListItem: ChatListItem) {
            binding.apply {
                val userModel = chatListItem.userModel
                titleTV.text =
                    userModel?.displayName ?: userModel?.username ?: userModel?.email ?: "Chat"

                if (chatListItem.messages.last().senderID == vm.userID) {
                    messageTV.text = itemView.context.getText(R.string.you_message)
                    messageTV.append(chatListItem.lastMessage)
                } else messageTV.text = chatListItem.lastMessage

                setTime(chatListItem.lastTimeStamp ?: 0L)

                unreadCount.visibility = if (chatListItem.unreadCount == 0) View.GONE
                else {
                    unreadCount.text = chatListItem.unreadCount.toString()
                    View.VISIBLE
                }

                animateOnlineStatus(binding.statusCircle, chatListItem.userModel?.userOnline?: false)

                GlideApp.with(itemView.context)
                    .load(vm.storageRefFromString(chatListItem.userModel?.imageRefString))
                    .centerCrop()
                    .placeholder(R.drawable.ic_profile_picture)
                    .error(R.drawable.ic_profile_picture)
                    .into(profileImage)
            }
        }

        private fun animateOnlineStatus(statusCircle: ImageView, userOnline: Boolean) {
            statusCircle.animate().cancel()

            val endScale = if (userOnline) 1f
            else 0f

            if (statusCircle.scaleX != endScale) {
                statusCircle.animate().cancel()
                statusCircle.animate()
                    .scaleX(endScale)
                    .scaleY(endScale)
                    .setDuration(200)
                    .start()
            }

        }

        private fun setTime(timeStamp: Long) {
            binding.timeTV.text = when {
                timeStamp.isDateToday() -> timeStamp.showTime()
                timeStamp.isSameYear() -> timeStamp.showDateNoYear()
                else -> timeStamp.showDateFull()
            }
        }
    }
}