package com.cloudlevi.ping.ui.addPost

import android.content.ContentValues.TAG
import android.gesture.GestureLibraries
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.iterator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cloudlevi.ping.data.AddImageModel
import com.cloudlevi.ping.databinding.ImageAddItemBinding
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide

class AddImageAdapter(val listener: OnCLickedListener): ListAdapter<AddImageModel, AddImageAdapter.AddImageViewHolder>(DiffCallBack()){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddImageViewHolder {
        val binding = ImageAddItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddImageViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class AddImageViewHolder(private val binding: ImageAddItemBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(addImageModel: AddImageModel){

            if (addImageModel.uri != Uri.EMPTY) Glide.with(itemView)
                .load(addImageModel.uri)
                .centerCrop()
                .into(binding.imageAddButton)

            addImageModel.viewID = adapterPosition

            binding.apply {
                imageAddButton.setOnClickListener {
                    listener.onAddButtonClicked(addImageModel)
                }
                if (addImageModel.filledIn){
                    imageDeleteButton.apply{
                        setOnClickListener {
                            listener.onRemoveButtonClicked(addImageModel)
                        }
                        visibility = View.VISIBLE
                    }
                }else imageDeleteButton.visibility = View.GONE

                for (child in root) child.isEnabled = addImageModel.enabledStatus
            }
        }

    }

    interface OnCLickedListener{
        fun onAddButtonClicked(addImageModel: AddImageModel)
        fun onRemoveButtonClicked(addImageModel: AddImageModel)
    }

    class DiffCallBack: DiffUtil.ItemCallback<AddImageModel>(){
        override fun areItemsTheSame(oldItem: AddImageModel, newItem: AddImageModel): Boolean =
            oldItem.uri == newItem.uri

        override fun areContentsTheSame(oldItem: AddImageModel, newItem: AddImageModel): Boolean =
            oldItem == newItem

    }
}