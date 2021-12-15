package com.cloudlevi.ping.ui.userChat

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.MediaAttachment
import com.cloudlevi.ping.databinding.ItemAttachmentBinding
import com.cloudlevi.ping.ext.makeGone
import com.cloudlevi.ping.ext.makeVisible

class AttachmentAdapter(val vm: UserChatViewModel) :
    RecyclerView.Adapter<AttachmentAdapter.AttachmentViewHolder>() {

    private var currentList = arrayListOf<MediaAttachment>()

    fun update() {
        currentList.clear()
        currentList.addAll(vm.attachmentsList)
        notifyDataSetChanged()
    }

    fun itemAdded() {
        currentList.add(vm.attachmentsList.last())
        notifyItemInserted(itemCount - 1)
    }

    fun itemRemovedAt(pos: Int) {
        currentList.clear()
        currentList.addAll(vm.attachmentsList)
        notifyItemRemoved(pos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttachmentViewHolder {
        return AttachmentViewHolder(
            ItemAttachmentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: AttachmentViewHolder, position: Int) {
        holder.bind(currentList.getOrNull(position))
    }

    override fun getItemCount() = when {
        currentList.isEmpty() -> 0
        currentList.size + 1 > 10 -> currentList.size
        else -> currentList.size + 1
    }

    inner class AttachmentViewHolder(
        private val binding: ItemAttachmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.imageDeleteButton.setOnClickListener {
                vm.removeImageAt(adapterPosition)
            }

                binding.root.setOnClickListener {
                    if (adapterPosition > vm.attachmentsList.lastIndex)
                        vm.addImageClick()
                    else vm.replaceImageAt(adapterPosition)
                }
        }

        fun bind(attachment: MediaAttachment?) {
            binding.apply {
                if (attachment?.uri != null) {
                    Glide.with(itemView.context)
                        .load(attachment.uri)
                        .centerCrop()
                        .into(attachmentImage)
                    imageDeleteButton.makeVisible()
                } else {
                    imageDeleteButton.makeGone()
                    Glide.with(itemView.context)
                        .load(ContextCompat.getDrawable(itemView.context, R.drawable.add_circle_outline))
                        .centerInside()
                        .into(attachmentImage)
                }
            }
        }

    }
}