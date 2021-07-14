package com.cloudlevi.ping.ui.userPosts

import android.content.ContentValues
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.databinding.ApartmentPostGridviewItemBinding
import com.cloudlevi.ping.databinding.ApartmentPostListviewItemBinding
import com.google.android.material.shape.CornerFamily
import java.text.DecimalFormat

class UserPostsAdapter(
    var apartmentList: ArrayList<ApartmentHomePost>,
    private val listener: OnPostClickedListener
    ): RecyclerView.Adapter<UserPostsAdapter.UserListsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserListsViewHolder {
        return UserListsViewHolder(ApartmentPostListviewItemBinding
            .inflate(LayoutInflater
                .from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: UserListsViewHolder, position: Int) {
        val item = apartmentList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return apartmentList.size
    }

    inner class UserListsViewHolder(private val binding: ApartmentPostListviewItemBinding)
        : RecyclerView.ViewHolder(binding.root){

        fun bind(aptPost: ApartmentHomePost) {

            val df = DecimalFormat("#.#")
            val locationString = aptPost.city + ", " + aptPost.address
            var priceType = ""
            var apartmentDrawable = 0
            when (aptPost.priceType) {
                PRICE_TYPE_PER_DAY -> priceType = "/day"
                PRICE_TYPE_PER_WEEK -> priceType = "/week"
                PRICE_TYPE_PER_MONTH -> priceType = "/month"
            }
            when (aptPost.aptType) {
                APT_TYPE_FLAT -> apartmentDrawable = R.drawable.ic_residential_block
                APT_TYPE_HOUSE -> apartmentDrawable = R.drawable.ic_home_24
            }
            binding.apply {
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
                    listener.onItemClickedListener(aptPost.apartmentPostID)
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

    interface OnPostClickedListener{
        fun onItemClickedListener(aptID: String)
    }
}