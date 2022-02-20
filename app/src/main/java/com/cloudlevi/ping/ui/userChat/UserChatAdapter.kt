package com.cloudlevi.ping.ui.userChat

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.Message
import com.cloudlevi.ping.databinding.ItemOtherMessageBinding
import com.cloudlevi.ping.databinding.ItemUserMessageBinding
import com.cloudlevi.ping.ext.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.stfalcon.imageviewer.StfalconImageViewer
import com.stfalcon.imageviewer.loader.ImageLoader
import kotlin.math.log

class UserChatAdapter(
    val vm: UserChatViewModel
) : RecyclerView.Adapter<UserChatAdapter.MessageVH>() {

    private var currentList = mutableListOf<Message>()
    private var hashMapSavedPos = hashMapOf<Int, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserChatAdapter.MessageVH {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == CURRENT_USER) MessageVH(
            ItemUserMessageBinding.inflate(inflater, parent, false)
        )
        else MessageVH(
            ItemOtherMessageBinding.inflate(inflater, parent, false)
        )
    }

    override fun onBindViewHolder(holder: UserChatAdapter.MessageVH, position: Int) {
        val message = currentList[position]
        holder.bind(message)
    }

    override fun getItemViewType(position: Int): Int {
        return if (currentList[position].senderID == vm.userID) CURRENT_USER
        else OTHER_USER
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update() {
        val oldList = currentList.toList()
        val newList = vm.messages.toList()

        currentList = vm.messages.toMutableList()

        if (sizeTheSame(oldList, newList)) {
            currentList.forEachIndexed { index, message ->
                if (oldList[index] != message)
                    notifyItemChanged(index)
            }
            //notifyItemRangeChanged(0, currentList.size)
            return
        }
        when {
            //one item was inserted
            newList.size - oldList.size == 1 -> {
                when {
                    oldList.isEmpty() -> notifyItemInserted(0)
                    newList.first().messageID != oldList.first().messageID ->
                        notifyItemInserted(0)
                    newList[newList.lastIndex - 1].messageID == oldList.last().messageID ->
                        notifyItemInserted(newList.lastIndex)
                    else -> notifyDataSetChanged()
                }
                return
            }
            else -> notifyDataSetChanged()
        }
    }

    fun updateImageForMessageAtPos(messagePos: Int, imagePos: Int, newImageRef: StorageReference) {
        currentList[messagePos].imagesList[imagePos] = newImageRef
    }

    fun updateImagesForPos(messagePos: Int, list: List<StorageReference>) {
        currentList[messagePos].imagesList = list.toMutableList()
    }

    inner class MessageVH(val binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

        val imagesRecycler = binding.root.findViewById<RecyclerView>(R.id.imagesRecycler)
        private val counterText = binding.root.findViewById<TextView>(R.id.counterText)
        private val messageTV = binding.root.findViewById<TextView>(R.id.messageTV)
        private val timeStampText = binding.root.findViewById<TextView>(R.id.timeStampText)
        var mediaCount = 0

        private val scrollListener: RecyclerView.OnScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager =
                        imagesRecycler.layoutManager as? LinearLayoutManager
                            ?: return

                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    val lastVisible = layoutManager.findLastVisibleItemPosition()

                    hashMapSavedPos[bindingAdapterPosition] = lastVisible
                    //savedScrollPos = lastVisible

                    val text = "${firstVisible + 1}/$mediaCount"
                    counterText.text = text
                }
            }

        private var imageAdapter = MessageMediaAdapter(vm)

        fun updateSavedScrollPos(newPos: Int) {
            hashMapSavedPos[bindingAdapterPosition] = newPos
            //savedScrollPos = newPos
            imagesRecycler.post {
                imagesRecycler.scrollToPosition(hashMapSavedPos[bindingAdapterPosition]?: 0)
            }
        }

        fun bind(message: Message?) {
            if (message?.hasMedia() == true) {
                imagesRecycler.removeOnScrollListener(scrollListener)
                mediaCount = message.mediaCount ?: 0

                imageAdapter.updateID(message.messageID ?: "")
                imageAdapter.updateList(message.imagesList)

                setupRecyclerView(
                    imagesRecycler,
                    counterText,
                    message
                )

                if ((message.mediaCount ?: 0) > 1)
                    counterText.makeVisible()
                else counterText.makeGone()

                imagesRecycler.makeVisible()

            } else {
                imagesRecycler.makeGone()
                counterText.makeGone()
            }

            messageTV.text = message?.message
            timeStampText.text = message?.timeStamp?.showTime()

            binding.root.requestLayout()
        }

        fun notifyImageItemChanged(index: Int) {
            imageAdapter.updateList(currentList[index].imagesList)
        }

        private fun setupRecyclerView(
            recycler: RecyclerView,
            counterTV: TextView,
            message: Message
        ) {
            val mediaCount = message.mediaCount ?: 0
//            if (recycler.adapter == null) {
//                recycler.adapter = MessageMediaAdapter(vm, message.messageID)
//                    .also { it.updateList(message.imagesList) }
//            } else (recycler.adapter as MessageMediaAdapter).updateList(message.imagesList)

            recycler.post {
                recycler.adapter = imageAdapter
                val counterPos = "${hashMapSavedPos[bindingAdapterPosition]?:0 + 1}/$mediaCount"
                counterText.text = counterPos
                Log.d(TAG, "scrolling recycler to ${hashMapSavedPos[bindingAdapterPosition]?:0} for message: ${message.message}")
                recycler.scrollToPosition(hashMapSavedPos[bindingAdapterPosition]?:0)
                recycler.removeAnimations()
                recycler.attachSnapHelper()

                if (mediaCount > 1) {
                    recycler.addOnScrollListener(scrollListener)
//                    recycler.setOnScrollChangeListener { view, _, _, _, _ ->
//                        val layoutManager =
//                            recycler.layoutManager as? LinearLayoutManager
//                                ?: return@setOnScrollChangeListener
//
//                        val firstVisible = layoutManager.findFirstVisibleItemPosition()
//                        val lastVisible = layoutManager.findLastVisibleItemPosition()
//
//                        Log.d(
//                            "DEBUG1",
//                            "setOnScrollChangeListener called, newPos: $lastVisible, message: ${message.message}"
//                        )
//                        savedScrollPos = lastVisible
//
//                        val text = "${firstVisible + 1}/$mediaCount"
//                        counterTV.text = text
//                    }
                }
            }
        }
    }
}

const val CURRENT_USER = 0
const val OTHER_USER = 1