package com.cloudlevi.ping.ui.home

import android.location.Geocoder
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
import com.cloudlevi.ping.di.GlideApp
import com.cloudlevi.ping.ext.getAddress
import com.cloudlevi.ping.ext.getStreet
import com.cloudlevi.ping.ext.makeGone
import com.cloudlevi.ping.ext.makeVisible
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.firebase.storage.FirebaseStorage
import java.text.DecimalFormat
import kotlin.system.measureTimeMillis

class PostsAdapter(
    val listType: Int,
    val listener: OnPostClickedListener,
    val geocoder: Geocoder
) :
    ListAdapter<ApartmentHomePost, PostsAdapter.PostsViewHolder>(DiffCallback()) {

    private val fileStorageReference =
        FirebaseStorage.getInstance().getReference("ApartmentUploads")

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

    inner class PostsViewHolder(private val binding: ViewBinding, private val listType: Int) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                listener.onItemClickedListener(bindingAdapterPosition)
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
            //val locationString = aptPost.createLatLng().getAddress(itemView.context, geocoder)
            val locationString = aptPost.locationString
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
            priceTextView.text = aptPost.mGetPricingText()
            priceTypeTextView.text = priceType
            apartmentTypeImage.setImageResource(apartmentDrawable)

            val avgRating = aptPost.calculateAverageRating()
            if (avgRating == 0.0) {
                ratingTextView.makeGone()
            } else {
                ratingTextView.text = df.format(avgRating)
                ratingTextView.makeVisible()
            }

            if (aptPost.firstImageReference.isNullOrEmpty()) {
                GlideApp.with(itemView)
                    .load(fileStorageReference.child(aptPost.apartmentPostID).child("0"))
                    .centerCrop()
                    .into(aptImageView)
            } else {
                //legacy code
                Glide.with(itemView)
                    .load(aptPost.firstImageReference)
                    .centerCrop()
                    .into(aptImageView)
            }

            aptImageView.shapeAppearanceModel = aptImageView.shapeAppearanceModel.toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, 20f)
                .build()
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