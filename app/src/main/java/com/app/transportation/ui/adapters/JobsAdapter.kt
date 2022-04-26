package com.app.transportation.ui.adapters

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.R
import com.app.transportation.core.dpToPx
import com.app.transportation.data.database.entities.Job
import com.app.transportation.databinding.ItemJobBinding
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject

class JobsAdapter : ListAdapter<Job, JobsAdapter.ViewHolder>(DiffCallback()) {
    public var mode:Boolean = false;//day
    init {
        //setHasStableIds(true)
        //stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemJobBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemJobBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: Job) = with(binding) {
                if(!mode){
                    cardfilter.setImageResource(R.drawable.card_gradient)
                }
                if (bindingAdapterPosition == 0) {
                    root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        marginStart = itemView.context.dpToPx(25)
                    }
                }

                title.text = item.title
                price.text = item.price
            }

    }

    private class DiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Job, newItem: Job) = oldItem == newItem
    }

}