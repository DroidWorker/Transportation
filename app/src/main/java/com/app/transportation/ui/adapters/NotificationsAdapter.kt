package com.app.transportation.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.R
import com.app.transportation.data.database.entities.Notification
import com.app.transportation.databinding.ItemNotificationBinding

class NotificationsAdapter :
    ListAdapter<Notification, NotificationsAdapter.ViewHolder>(DiffCallback()) {

    var onItemClick: ((Int) -> Unit)? = null

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemNotificationBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = currentList[position].id

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: Notification) = with(binding) {
                imageViewDeleteNotification.setOnClickListener{onItemClick?.invoke(item.id.toInt())}
                if (item.id==0L) imageViewDeleteNotification.visibility = View.GONE
                title.text = item.title
                description.text = item.description
                if(!item.isRead){
                    imageStatus.setImageResource(R.drawable.ic_notification_unread)
                }
            }

    }

    private class DiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Notification, newItem: Notification) =
            oldItem == newItem
    }

}