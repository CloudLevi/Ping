package com.cloudlevi.ping.ui.addPost

import android.content.ContentValues.TAG
import android.gesture.GestureLibraries
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.core.view.iterator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cloudlevi.ping.data.AddImageModel
import com.cloudlevi.ping.databinding.ImageAddItemBinding
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.cloudlevi.ping.R

class AddImageAdapter(
    val listener: OnCLickedListener,
    var viewModel: AddPostFragmentViewModel
) : RecyclerView.Adapter<AddImageAdapter.AddImageViewHolder>() {

    private var currentList = arrayListOf<AddImageModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddImageViewHolder {
        val binding =
            ImageAddItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddImageViewHolder, position: Int) {
        val currentItem = currentList.getOrNull(position) ?: return
        holder.bind(currentItem)
    }

    fun update() {
        currentList = ArrayList(viewModel.imagesArray)
        notifyDataSetChanged()
    }

    inner class AddImageViewHolder(private val binding: ImageAddItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(addImageModel: AddImageModel) {

            if (addImageModel.uri != Uri.EMPTY) Glide.with(itemView)
                .load(addImageModel.uri)
                .centerCrop()
                .into(binding.imageAddButton)
            else binding.imageAddButton.setImageResource(0)

            addImageModel.viewID = adapterPosition

            binding.apply {
                imageAddButton.setOnClickListener {
                    listener.onAddButtonClicked(addImageModel)
                }
                if (addImageModel.filledIn) {
                    imageDeleteButton.apply {
                        setOnClickListener {
                            viewModel.onImageRemoveButtonClicked(addImageModel)
                        }
                        visibility = View.VISIBLE
                    }
                } else imageDeleteButton.visibility = View.GONE

                root.forEach { it.isEnabled = addImageModel.enabledStatus }
            }
        }

    }

    interface OnCLickedListener {
        fun onAddButtonClicked(addImageModel: AddImageModel)
        fun onRemoveButtonClicked(addImageModel: AddImageModel)
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}