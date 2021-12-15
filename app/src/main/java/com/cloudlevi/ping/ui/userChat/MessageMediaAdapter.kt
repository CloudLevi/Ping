package com.cloudlevi.ping.ui.userChat

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudlevi.ping.MediaAdapterVM
import com.cloudlevi.ping.R
import com.cloudlevi.ping.databinding.ItemSimpleImageBinding
import com.cloudlevi.ping.ext.sizeTheSame

class MessageMediaAdapter(val vm: MediaAdapterVM, private var id: String? = null) :
    RecyclerView.Adapter<MessageMediaAdapter.MediaViewHolder>() {

    private var currentList = mutableListOf<Uri>()

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
        Log.d("DEBUG", "updateList: pos: $pos, id: $id")
        val newList = if (pos == null) vm.getCurrentListByID(id).toMutableList()
        else vm.getCurrentListByPos(pos).toMutableList()

        Log.d("DEBUG", "updateList: newList: $newList")

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

    fun updateID(newID: String){
        id = newID
    }

    fun updateList(list: List<Uri>) {
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

    fun applyListOnly(newList: MutableList<Uri>) {
        currentList.clear()
        currentList.addAll(newList)
    }

//    fun itemUpdated(pos: Int) {
//        currentList[pos] = vm.getCurrentListByID(id)[pos]
//        notifyItemChanged(pos)
//    }

    inner class MediaViewHolder(val binding: ItemSimpleImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                vm.onMediaImageClick(id, bindingAdapterPosition)
            }
        }

        fun bind(imageURI: Uri?) {
            if (imageURI == null || imageURI == Uri.EMPTY)
                Glide.with(itemView.context)
                    .load(R.drawable.placeholder)
                    .centerCrop()
                    .into(binding.imageView)
            else Glide.with(itemView.context)
                .load(imageURI)
                .centerCrop()
                .into(binding.imageView)
        }
    }
}