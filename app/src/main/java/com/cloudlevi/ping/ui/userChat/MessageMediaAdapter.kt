package com.cloudlevi.ping.ui.userChat

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.cloudlevi.ping.MediaAdapterVM
import com.cloudlevi.ping.R
import com.cloudlevi.ping.databinding.ItemSimpleImageBinding
import com.cloudlevi.ping.di.GlideApp
import com.cloudlevi.ping.ext.sizeTheSame
import com.google.firebase.storage.StorageReference

class MessageMediaAdapter(
    val vm: MediaAdapterVM,
    private var id: String? = null,
    val doCrop: Boolean = true
) :
    RecyclerView.Adapter<MessageMediaAdapter.MediaViewHolder>() {

    private var currentList = mutableListOf<StorageReference>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return MediaViewHolder(
            ItemSimpleImageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemCount() = currentList.size

    fun updateList(pos: Int? = null) {
        val newList = if (pos == null) vm.getCurrentListByID(id).toMutableList()
        else vm.getCurrentListByPos(pos).toMutableList()

        if (sizeTheSame(currentList, newList)) {
            newList.forEachIndexed { index, uri ->
                if (currentList[index] != uri) {
                    currentList[index] = uri
                    notifyItemChanged(index)
                }
            }
        } else {
            currentList.clear()
            currentList.addAll(newList)
            notifyDataSetChanged()
        }
    }

    fun updateID(newID: String) {
        id = newID
    }

    fun updateList(list: List<StorageReference>) {
        val newList = list.toMutableList()

        if (sizeTheSame(currentList, newList)) {
            newList.forEachIndexed { index, uri ->
                if (currentList[index] != uri) {
                    currentList[index] = uri
                    notifyItemChanged(index)
                }
            }
        } else {
            currentList.clear()
            currentList.addAll(newList)
            notifyDataSetChanged()
        }
    }

    fun applyListOnly(newList: List<StorageReference>) {
        currentList.clear()
        currentList.addAll(newList)
    }

    inner class MediaViewHolder(val binding: ItemSimpleImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                vm.onMediaImageClick(id, bindingAdapterPosition)
            }
        }

        fun bind(imageRef: StorageReference?) {
            var gRequest = GlideApp.with(itemView.context)
                .load(imageRef)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)

            if (doCrop) gRequest = gRequest.centerCrop()
            gRequest.into(binding.imageView)
        }
    }
}