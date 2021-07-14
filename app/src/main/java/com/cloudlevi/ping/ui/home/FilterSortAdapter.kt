package com.cloudlevi.ping.ui.home

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cloudlevi.ping.R
import com.cloudlevi.ping.databinding.ChipItemBinding

class FilterSortAdapter(
    var currentList: ArrayList<String>,
    private val listener: FilterListener,
    private val initSort: Int
): RecyclerView.Adapter<FilterSortAdapter.SortViewHolder>() {

    var currentChoice: Int = initSort


    var oldChoice: Int = -1
    var arrayOfViewHolders = arrayListOf<SortViewHolder>()

    inner class SortViewHolder(private val binding: ChipItemBinding): RecyclerView.ViewHolder(binding.root){

        init {
            binding.chip.setOnClickListener {
                if (currentChoice != adapterPosition){
                    oldChoice = currentChoice
                    currentChoice = adapterPosition
                    listener.sortTypeSelected(currentChoice)

                    binding.chip.chipBackgroundColor = ContextCompat.getColorStateList(binding.root.context, R.color.buttonColorActive)
                    notifyItemChanged(oldChoice)
                }
            }
        }

        fun bind(text: String){
            binding.chip.text = text
            if (adapterPosition != currentChoice)
                binding.chip.chipBackgroundColor = ContextCompat.getColorStateList(binding.root.context, R.color.light_gray)
            else binding.chip.chipBackgroundColor = ContextCompat.getColorStateList(binding.root.context, R.color.buttonColorActive)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortViewHolder {
        val viewHolder = SortViewHolder(ChipItemBinding
            .inflate(LayoutInflater
                .from(parent.context), parent, false))
        arrayOfViewHolders.add(viewHolder)
        return viewHolder
    }

    override fun onBindViewHolder(holder: SortViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    private fun getListOfFalse(): ArrayList<Boolean> = arrayListOf(false, false, false, false)
}