package com.cloudlevi.ping.ui.yourBookings

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudlevi.ping.R
import com.cloudlevi.ping.data.BookingModel
import com.cloudlevi.ping.databinding.ItemYourBookingBinding
import com.cloudlevi.ping.di.GlideApp
import com.cloudlevi.ping.ext.*
import com.cloudlevi.ping.ui.userChat.MessageMediaAdapter

class YourBookingsAdapter(val vm: YourBookingsViewModel) :
    RecyclerView.Adapter<YourBookingsAdapter.BookingVH>() {

    private var currentList = vm.bookingsList.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingVH {
        return BookingVH(
            ItemYourBookingBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    fun update() {
        val oldList = currentList.toMutableList()
        currentList = vm.bookingsList.toMutableList()

        if (sizeTheSame(oldList, vm.bookingsList)) {
            oldList.forEachIndexed { index, bookingModel ->
                if (bookingModel != vm.bookingsList[index])
                    notifyItemChanged(index)
            }
        } else notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: BookingVH, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemCount() = currentList.size

    inner class BookingVH(val binding: ItemYourBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false
        private var savedScrollPos = 0

        fun updateScrollPos(pos: Int) {
            binding.apply {
                imagesRecycler.post {
                    savedScrollPos = pos
                    imagesRecycler.scrollToPosition(savedScrollPos)
                }
            }
        }

        init {
            binding.apply {
                root.setOnClickListener {
                    if (isExpanded) {
                        vm.notifyRecyclerResize()
                        isExpanded = false
                        expandableLayout.makeGone()
                    } else {
                        vm.notifyRecyclerResize()
                        isExpanded = true
                        expandableLayout.makeVisible()
                    }
                    animateArrow()
                }
            }
        }

        fun bind(b: BookingModel?) {
            b ?: return
            binding.apply {

                if (imagesRecycler.adapter == null) {
                    val adapter = MessageMediaAdapter(vm, b.bookingID)
                    imagesRecycler.adapter = adapter
                    imagesRecycler.attachSnapHelper()
                    imagesRecycler.removeAnimations()
                    adapter.updateList(bindingAdapterPosition)
                } else (imagesRecycler.adapter as MessageMediaAdapter).updateList(bindingAdapterPosition)

                expandableLayout.visibleOrGone(isExpanded)
                expandArrow.rotation = if (isExpanded) 180f else 0f

                specialWishesTV.text = b.extraInfo
                specialWishesLayout.visibleOrGone(!b.extraInfo.isNullOrEmpty())

                val checkInText = ((b.checkInDate ?: 0) + (b.checkInTime ?: 0)).showDateTime()
                val totalText = b.mGetPricingText()
                val cityCountryText = b.aLatLng?.getCityCountry(itemView.context)
                val acreageText = String.format(root.context.getString(R.string.m2), b.aAcreage)

                statusTV.text = b.parsePaymentStatusText(itemView.context)
                titleTV.text = b.aTitle
                checkInTV.text = checkInText
                checkOutTV.text = b.checkOutDate?.showDateTime()
                priceTV.text = totalText
                locationTV.text = b.aLatLng?.getAddress(itemView.context)
                cityTV.text = cityCountryText
                acreageTV.text = HtmlCompat.fromHtml(acreageText, HtmlCompat.FROM_HTML_MODE_LEGACY)
                roomCountTV.text = b.roomCountString(root.context)
                furnishmentTV.text = if (b.aFurniture) root.context.getString(R.string.furnished)
                else root.context.getString(R.string.no_furniture)
                ratingTV.text = b.aRating.toString()

                landLordName.text = b.landLordDisplayName
                landLordUserName.text = b.landLordUserName

                GlideApp.with(root.context)
                    .load(vm.getUserImageRef(b.landlordID))
                    .error(R.drawable.ic_profile_picture)
                    .into(profileImage)

                if (b.aImagesList.size > 1) {
                    val initialText = "1/${b.aImagesList?.size?:0}"
                    counterTV.text = initialText
                    counterTV.makeVisible()

                    imagesRecycler.setOnScrollChangeListener { view, i, i2, i3, i4 ->
                        val layoutManager =
                            imagesRecycler.layoutManager as? LinearLayoutManager
                                ?: return@setOnScrollChangeListener

                        val firstVisible = layoutManager.findFirstVisibleItemPosition()
                        val lastVisible = layoutManager.findLastVisibleItemPosition()

                        savedScrollPos = lastVisible

                        val text = "${firstVisible + 1}/${b.aImagesList?.size?:0}"
                        counterTV.text = text
                    }
                } else counterTV.makeGone()
            }
        }

        fun notifyImageItemChanged(index: Int) {
            Log.d("DEBUG", "notifyImageUpdated: index: $index")
            (binding.imagesRecycler.adapter as MessageMediaAdapter).updateList(index)
        }

        private fun animateArrow() {
            binding.apply {
                expandArrow.animate().cancel()
                expandArrow.animate().rotation(if (isExpanded) 180f else 0f).setDuration(150)
                    .start()
            }
        }

    }
}