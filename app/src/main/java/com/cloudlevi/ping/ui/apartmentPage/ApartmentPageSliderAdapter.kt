package com.cloudlevi.ping.ui.apartmentPage

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.firebase.storage.StorageReference

class ApartmentPageSliderAdapter(fragment: Fragment, private var imagesList: Map<Int, StorageReference>) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return imagesList.size
    }

    override fun createFragment(position: Int): Fragment {
        return ApartmentPageSliderItem.getInstance(position, imagesList[position]!!)
    }

    fun submitList(newImagesList: Map<Int, StorageReference>){
        if (newImagesList != imagesList){
            imagesList = newImagesList
            notifyDataSetChanged()
        }
    }

}