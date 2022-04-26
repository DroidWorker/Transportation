package com.app.transportation.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.data.database.entities.ServiceType
import com.app.transportation.databinding.ItemPmServicesFilterTextBinding

class PopupMenuServicesFilterAdapter :
    ListAdapter<ServiceType, PopupMenuServicesFilterAdapter.ViewHolder>(DiffCallback()) {

    var necessary = false
    var onClick: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemPmServicesFilterTextBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    fun getItem(id: Long): ServiceType? = currentList.find { it.id.toLong() == id }

    override fun getItemId(position: Int): Long = currentList[position].id.toLong()

    fun getItemPosition(id: Long): Int = currentList.indexOfFirst { it.id.toLong() == id }

    inner class ViewHolder(private val binding: ItemPmServicesFilterTextBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: ServiceType) {
                with(binding) {
                    root.setOnClickListener { if (necessary) onClick?.invoke(item.id) }
                    text.text = item.title
                }
            }

    }

    private class DiffCallback : DiffUtil.ItemCallback<ServiceType>() {
        override fun areItemsTheSame(oldItem: ServiceType, newItem: ServiceType) = oldItem == newItem
        override fun areContentsTheSame(oldItem: ServiceType, newItem: ServiceType) = oldItem == newItem
    }

}