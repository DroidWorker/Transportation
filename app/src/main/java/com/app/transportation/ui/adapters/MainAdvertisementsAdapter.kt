package com.app.transportation.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.R
import com.app.transportation.core.dpToPx
import com.app.transportation.data.database.entities.Advertisement
import com.app.transportation.databinding.ItemAdvertisementBinding
import com.app.transportation.main.core.selection.ItemDetailsLookup
import com.app.transportation.main.core.selection.SelectionTracker

class MainAdvertisementsAdapter :
    ListAdapter<Advertisement, MainAdvertisementsAdapter.ViewHolder>(DiffCallback()) {
    public var mode:Boolean = false;//day
    var selectionTracker: SelectionTracker<Long>? = null

    init {
        setHasStableIds(true)
        //stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemAdvertisementBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    fun getItem(id: Long): Advertisement? = currentList.find { it.id == id }

    override fun getItemId(position: Int): Long = currentList[position].id

    fun getItemPosition(id: Long): Int = currentList.indexOfFirst { it.id == id }

    inner class ViewHolder(private val binding: ItemAdvertisementBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: Advertisement) {
                with(binding) {
                    if (bindingAdapterPosition == 0) {
                        root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            marginStart = itemView.context.dpToPx(25)
                        }
                    }
                    if (item.title1.isNotBlank()) {
                        title1.text = item.title1
                        title1.isVisible = true
                    }
                    if (item.title2.isNotBlank()) {
                        title2.text = item.title2
                        title2.isVisible = true
                    }
                    if (item.iamge!=null) {
                        catimage.setImageBitmap(item.iamge)
                    }

                    selectionTracker?.isSelected(item.id)?.let { root.isActivated = it }
                }
            }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = bindingAdapterPosition
                override fun getSelectionKey(): Long = getItemId(bindingAdapterPosition)
            }

    }

    private class DiffCallback : DiffUtil.ItemCallback<Advertisement>() {
        override fun areItemsTheSame(oldItem: Advertisement, newItem: Advertisement) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Advertisement, newItem: Advertisement) = oldItem == newItem
    }

}