package com.cloudlevi.ping.ui.home

import android.content.ContentValues.TAG
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.databinding.ApartmentPostGridviewItemBinding
import com.cloudlevi.ping.databinding.ApartmentPostListviewItemBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import java.text.DecimalFormat

class PostsAdapter(val listType: Int, val listener: OnPostClickedListener) :
    ListAdapter<ApartmentHomePost, PostsAdapter.PostsViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostsViewHolder {
        when (listType) {
            HOMEFRAGMENT_LISTVIEW -> {
                return PostsViewHolder(
                    ApartmentPostListviewItemBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    ), listType
                )
            }
            HOMEFRAGMENT_GRIDVIEW -> {
                return PostsViewHolder(
                    ApartmentPostGridviewItemBinding.inflate(
                        LayoutInflater.from(
                            parent.context
                        ), parent, false
                    ), listType
                )
            }
        }
        return PostsViewHolder(
            ApartmentPostListviewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), listType
        )
    }

    override fun onBindViewHolder(holder: PostsViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class PostsViewHolder(private var binding: ViewBinding, private val listType: Int) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                listener.onItemClickedListener(adapterPosition)
            }
        }

        private val titleTextView = binding.root.findViewById<TextView>(R.id.titleTextView)
        private val locationTextView = binding.root.findViewById<TextView>(R.id.locationTextView)
        private val priceTextView = binding.root.findViewById<TextView>(R.id.priceTextView)
        private val priceTypeTextView = binding.root.findViewById<TextView>(R.id.priceTypeTextView)
        private val apartmentTypeImage = binding.root.findViewById<ImageView>(R.id.apartmentType)
        private val ratingTextView = binding.root.findViewById<TextView>(R.id.ratingTextView)
        private val aptImageView = binding.root.findViewById<ShapeableImageView>(R.id.aptImageView)

        fun bind(aptPost: ApartmentHomePost) {

            val df = DecimalFormat("#.#")
            val locationString = aptPost.city + ", " + aptPost.address
            var priceType = ""
            var apartmentDrawable = 0
            when (aptPost.priceType) {
                PRICE_TYPE_PER_DAY -> priceType = "/${itemView.context.getString(R.string.day)}"
                PRICE_TYPE_PER_WEEK -> priceType = "/${itemView.context.getString(R.string.week)}"
                PRICE_TYPE_PER_MONTH -> priceType = "/${itemView.context.getString(R.string.month)}"
            }
            when (aptPost.aptType) {
                APT_TYPE_FLAT -> apartmentDrawable = R.drawable.ic_residential_block
                APT_TYPE_HOUSE -> apartmentDrawable = R.drawable.ic_home_24
            }

            titleTextView.text = aptPost.title
            locationTextView.text = locationString
            priceTextView.text = aptPost.getPricingText()
            priceTypeTextView.text = priceType
            apartmentTypeImage.setImageResource(apartmentDrawable)
            ratingTextView.text = df.format(aptPost.calculateAverageRating())

            Glide.with(itemView)
                .load(aptPost.firstImageReference)
                .centerCrop()
                .placeholder(R.drawable.progress_animation_small)
                .into(aptImageView)

            aptImageView.shapeAppearanceModel = aptImageView.shapeAppearanceModel.toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, 20f)
                .build()

//            when(listType){
//                HOMEFRAGMENT_LISTVIEW -> {
//                    (binding as ApartmentPostListviewItemBinding).apply {
//                        titleTextView.text = aptPost.title
//                        locationTextView.text = locationString
//                        priceTextView.text = addRearSymbol(aptPost.price.toString(), "$")
//                        priceTypeTextView.text = priceType
//                        apartmentType.setImageResource(apartmentDrawable)
//                        ratingTextView.text = df.format(aptPost.calculateAverageRating())
//
//                        Glide.with(itemView)
//                            .load(aptPost.firstImageReference)
//                            .centerCrop()
//                            .placeholder(R.drawable.progress_animation_small)
//                            .into(aptImageView)
//
//                        aptImageView.shapeAppearanceModel = aptImageView.shapeAppearanceModel.toBuilder()
//                            .setAllCorners(CornerFamily.ROUNDED, 20f)
//                            .build()
//                    }
//                }
//                HOMEFRAGMENT_GRIDVIEW -> {
//                    (binding as ApartmentPostGridviewItemBinding).apply {
//                        titleTextView.text = aptPost.title
//                        locationTextView.text = locationString
//                        priceTextView.text = addRearSymbol(aptPost.price.toString(), "$")
//                        priceTypeTextView.text = priceType
//                        apartmentType.setImageResource(apartmentDrawable)
//                        ratingTextView.text = df.format(aptPost.calculateAverageRating())
//
//                        Glide.with(itemView)
//                            .load(aptPost.firstImageReference)
//                            .centerCrop()
//                            .placeholder(R.drawable.progress_animation_small)
//                            .into(aptImageView)
//
//                        aptImageView.shapeAppearanceModel = aptImageView.shapeAppearanceModel.toBuilder()
//                            .setAllCorners(CornerFamily.ROUNDED, 20f)
//                            .build()
//                    }
//                }
//            }
        }

        private fun addRearSymbol(text: String, symbol: String): String {
            return "$text$symbol"
        }

        private fun determineRoomText(roomAmount: Int): String {
            return if (roomAmount % 10 == 1) "$roomAmount room"
            else "$roomAmount rooms"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ApartmentHomePost>() {
        override fun areItemsTheSame(oldItem: ApartmentHomePost, newItem: ApartmentHomePost) =
            oldItem.apartmentPostID == newItem.apartmentPostID

        override fun areContentsTheSame(oldItem: ApartmentHomePost, newItem: ApartmentHomePost) =
            oldItem == newItem

    }

    interface OnPostClickedListener {
        fun oldOnItemClickedListener(apartmentID: String)
        fun onItemClickedListener(pos: Int)
    }
}