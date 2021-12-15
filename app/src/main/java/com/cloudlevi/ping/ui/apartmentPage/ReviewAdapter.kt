package com.cloudlevi.ping.ui.apartmentPage

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.RatingModel
import com.cloudlevi.ping.databinding.ItemReviewBinding
import com.cloudlevi.ping.ext.howLongAgo
import com.cloudlevi.ping.ext.makeGone
import com.cloudlevi.ping.ext.makeVisible
import com.cloudlevi.ping.ext.sizeTheSame

class ReviewAdapter(val vm: ApartmentPageViewModel) :
    RecyclerView.Adapter<ReviewAdapter.ReviewVH>() {

    private var currentList: List<RatingModel> = vm.currentApartmentModel.ratingsList.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewVH {
        return ReviewVH(
            ItemReviewBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    fun update() {
        val oldList = currentList.toList()
        val newList = vm.currentApartmentModel.ratingsList.toList()

        currentList = vm.currentApartmentModel.ratingsList.toList()

        if (sizeTheSame(oldList, newList)) {
            notifyItemRangeChanged(0, currentList.size)
            return
        }

        when {
            //one item was inserted
            newList.size - oldList.size == 1 -> {
                when {
                    oldList.isEmpty() -> notifyItemInserted(0)
                    newList.first().userID != oldList.first().userID ->
                        notifyItemInserted(0)
                    newList[newList.lastIndex - 1].userID == oldList.last().userID ->
                        notifyItemInserted(newList.lastIndex)
                    else -> notifyDataSetChanged()
                }
                return
            }
            //one item was removed
            oldList.size - newList.size == 1 -> {
                var missingPos = 0
                kotlin.run lit@{
                    oldList.forEachIndexed{ index, model ->
                        if (model.userID != newList.getOrNull(index)?.userID) {
                            missingPos = index
                            return@lit
                        }
                    }
                }
                notifyItemRemoved(missingPos)
            }
            else -> notifyDataSetChanged()
        }
    }

    override fun onBindViewHolder(holder: ReviewVH, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemCount() = currentList.size

    inner class ReviewVH(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                vm.reviewClicked(currentList.getOrNull(bindingAdapterPosition))
            }
        }

        fun bind(rModel: RatingModel) {
            binding.apply {
                userNameTV.text = rModel.displayName
                rating.rating = rModel.rating.toFloat()
                ratingTV.text = rModel.rating.toString()
                timeStampTV.text = howLongAgo(itemView.context, rModel.timeStamp?: System.currentTimeMillis())

                if (rModel.comment.isNullOrEmpty()){
                    (detailsLayout.layoutParams as ConstraintLayout.LayoutParams)
                        .bottomToBottom = profileImage.id
                    commentTV.makeGone()
                } else {
                    (detailsLayout.layoutParams as ConstraintLayout.LayoutParams)
                        .bottomToBottom = 0
                    commentTV.text = rModel.comment
                    commentTV.makeVisible()
                }
            }
        }

        fun updateImage(imageUrl: String?){
            if (imageUrl.isNullOrEmpty()){
                binding.profileImage.setImageResource(R.drawable.ic_profile_picture)
            } else Glide.with(itemView.context).load(imageUrl).into(binding.profileImage)
        }
    }
}