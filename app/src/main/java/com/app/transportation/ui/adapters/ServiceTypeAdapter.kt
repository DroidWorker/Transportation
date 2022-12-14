package com.app.transportation.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.R
import com.app.transportation.data.database.entities.AdvertisementCategory
import com.app.transportation.data.database.entities.ServiceType
import com.app.transportation.databinding.ItemServiceTypeBinding
import com.app.transportation.main.core.selection.ItemDetailsLookup
import com.app.transportation.ui.MainViewModel

class ServiceTypeAdapter : ListAdapter<AdvertisementCategory, ServiceTypeAdapter.ViewHolder>(DiffCallback()) {

    var onClick: (Int.() -> Unit)? = null

    var lastCheckedCategoryId = 0

    var newsCount : MutableMap<Int, Int> = emptyMap<Int, Int>().toMutableMap()

    init {
        setHasStableIds(true)
        //stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemServiceTypeBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getItem(id: Long): AdvertisementCategory? = currentList.find { it.id.toLong() == id }

    override fun getItemId(position: Int): Long = currentList[position].id.toLong()

    fun getItemPosition(id: Long): Int = currentList.indexOfFirst { it.id.toLong() == id }

    inner class ViewHolder(private val binding: ItemServiceTypeBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: AdvertisementCategory) = with(binding) {
                cardView.setCardBackgroundColor(
                    if (lastCheckedCategoryId == item.id)
                        ContextCompat.getColor(itemView.context, R.color.primary_color)
                    else
                        ContextCompat.getColor(itemView.context, R.color.service_type_background)
                )

                root.setOnClickListener {
                    newsCount[item.id] = 0
                    onClick?.invoke(item.id)

                }
                //root.setOnClickListener{                }

                serviceType.text = item.name

                serviceNumber.text = newsCount[item.id].toString()
                serviceNumber.isVisible = (serviceNumber.text != "0" && serviceNumber.text != "null")

                //selectionTracker?.isSelected(item.id)?.let { root.isActivated = it }
            }

        fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int = bindingAdapterPosition
                override fun getSelectionKey(): Long = getItemId(bindingAdapterPosition)
            }

    }

    private class DiffCallback : DiffUtil.ItemCallback<AdvertisementCategory>() {
        override fun areItemsTheSame(
            oldItem: AdvertisementCategory,
            newItem: AdvertisementCategory
        ) = oldItem == newItem
        override fun areContentsTheSame(
            oldItem: AdvertisementCategory,
            newItem: AdvertisementCategory
        ) = oldItem == newItem
    }

}