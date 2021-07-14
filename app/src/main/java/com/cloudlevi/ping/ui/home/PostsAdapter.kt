package com.cloudlevi.ping.ui.home

import android.content.ContentValues.TAG
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.databinding.ApartmentPostGridviewItemBinding
import com.cloudlevi.ping.databinding.ApartmentPostListviewItemBinding
import com.google.android.material.shape.CornerFamily
import java.text.DecimalFormat

class PostsAdapter(val listType: Int, val listener: OnPostClickedListener): ListAdapter<ApartmentHomePost, PostsAdapter.PostsViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostsViewHolder {
        when(listType){
            HOMEFRAGMENT_LISTVIEW -> {
                return PostsViewHolder(ApartmentPostListviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), listType)
            }
            HOMEFRAGMENT_GRIDVIEW -> {
                return PostsViewHolder(ApartmentPostGridviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), listType)
            }
        }
        return PostsViewHolder(ApartmentPostListviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), listType)
    }

    override fun onBindViewHolder(holder: PostsViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    inner class PostsViewHolder(private var binding: ViewBinding, private val listType: Int) : RecyclerView.ViewHolder(binding.root) {

        fun bind(aptPost: ApartmentHomePost){

            val df = DecimalFormat("#.#")
            val locationString = aptPost.city + ", " + aptPost.address
            val acreageString = aptPost.acreage.toString() + " square feet"
            val roomAmountString = determineRoomText(aptPost.roomAmount)
            var priceType = ""
            var apartmentDrawable = 0
            when(aptPost.priceType){
                PRICE_TYPE_PER_DAY -> priceType = "/day"
                PRICE_TYPE_PER_WEEK -> priceType = "/week"
                PRICE_TYPE_PER_MONTH -> priceType = "/month"
            }
            when(aptPost.aptType){
                APT_TYPE_FLAT -> apartmentDrawable = R.drawable.ic_residential_block
                APT_TYPE_HOUSE -> apartmentDrawable = R.drawable.ic_home_24
            }

            when(listType){
                HOMEFRAGMENT_LISTVIEW -> {
                    (binding as ApartmentPostListviewItemBinding).apply {
                        titleTextView.text = aptPost.title
                        locationTextView.text = locationString
                        priceTextView.text = addRearSymbol(aptPost.price.toString(), "$")
                        priceTypeTextView.text = priceType
                        apartmentType.setImageResource(apartmentDrawable)
                        ratingTextView.text = df.format(aptPost.rating)

                        Glide.with(itemView)
                            .load(aptPost.firstImageReference)
                            .centerCrop()
                            .placeholder(R.drawable.progress_animation_small)
                            .into(aptImageView)

                        aptImageView.shapeAppearanceModel = aptImageView.shapeAppearanceModel.toBuilder()
                            .setAllCorners(CornerFamily.ROUNDED, 20f)
                            .build()

                        root.setOnClickListener {
                            listener.OnItemClickedListener(aptPost.apartmentPostID)
                        }
                    }
                }
                HOMEFRAGMENT_GRIDVIEW -> {
                    (binding as ApartmentPostGridviewItemBinding).apply {
                        titleTextView.text = aptPost.title
                        locationTextView.text = locationString
//                acreageTextView.text = acreageString
//                roomTextView.text = roomAmountString
                        priceTextView.text = addRearSymbol(aptPost.price.toString(), "$")
                        priceTypeTextView.text = priceType
                        apartmentType.setImageResource(apartmentDrawable)
                        ratingTextView.text = df.format(aptPost.rating)

                        Glide.with(itemView)
                            .load(aptPost.firstImageReference)
                            .centerCrop()
                            .placeholder(R.drawable.progress_animation_small)
                            .into(aptImageView)

                        aptImageView.shapeAppearanceModel = aptImageView.shapeAppearanceModel.toBuilder()
                            .setAllCorners(CornerFamily.ROUNDED, 20f)
                            .build()

                        root.setOnClickListener {
                            listener.OnItemClickedListener(aptPost.apartmentPostID)
                        }
                    }
                }
            }
        }

        private fun addRearSymbol(text: String, symbol: String): String{
            return "$text$symbol"
        }

        private fun determineRoomText(roomAmount: Int): String{
            return if (roomAmount % 10 == 1) "$roomAmount room"
            else "$roomAmount rooms"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ApartmentHomePost>() {
        override fun areItemsTheSame(oldItem: ApartmentHomePost, newItem: ApartmentHomePost) =
            oldItem.apartmentPostID == newItem.apartmentPostID

        override fun areContentsTheSame(oldItem: ApartmentHomePost, newItem: ApartmentHomePost) = oldItem == newItem

    }

    interface OnPostClickedListener{
        fun OnItemClickedListener(apartmentID: String)
    }
}