package com.app.transportation.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.core.dateToString
import com.app.transportation.data.database.entities.FeedbackRequest
import com.app.transportation.databinding.ItemFeedbackBinding
import com.app.transportation.databinding.ItemRequestBinding

class FeedbacksRequestsAdapter :
    ListAdapter<FeedbackRequest, RecyclerView.ViewHolder>(DiffCallback()) {
    var onDeleteClick: ((Int) -> Unit)? = null
    var onClick: ((Int) -> Unit)? = null
    var onFeedbackClick: ((Int) -> Unit)? = null

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == 0)
            RequestViewHolder(ItemRequestBinding.inflate(layoutInflater, parent, false))
        else
            FeedbackViewHolder(ItemFeedbackBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        with(getItem(position)) {
            if (viewType == 0)
                (holder as RequestViewHolder).bind(this)
            else
                (holder as FeedbackViewHolder).bind(this)
        }

    override fun getItemViewType(position: Int) = getItem(position).viewType

    fun getItem(id: Long): FeedbackRequest? = currentList.find { it.id == id }

    override fun getItemId(position: Int): Long = getItem(position).id

    fun getItemPosition(id: Long): Int = currentList.indexOfFirst { it.id == id }

    inner class RequestViewHolder(private val binding: ItemRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeedbackRequest) = with(binding) {
            root.setOnClickListener { onClick?.invoke(item.id.toInt()) }
            delete.setOnClickListener{ onDeleteClick?.invoke(item.id.toInt())}
            title.text = item.title
            location.text = item.subtitle
            date.text = item.dateTime.dateToString("dd/MM/yyyy")
            time.text = item.dateTime.dateToString("HH:mm")
            if (item.price!=0)
                price.text = item.price.toString() + " руб"
            else
                price.visibility= View.GONE
        }

    }

    inner class FeedbackViewHolder(private val binding: ItemFeedbackBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FeedbackRequest) = with(binding) {
            root.setOnClickListener { onFeedbackClick?.invoke(item.id.toInt()) }
            title.text = item.title
            subtitle.text = item.subtitle
            if (item.price!=0)
                price.text = item.price.toString() + " руб"
            else
                priceCardView.visibility= View.GONE
        }

    }

    private class DiffCallback : DiffUtil.ItemCallback<FeedbackRequest>() {
        override fun areItemsTheSame(oldItem: FeedbackRequest, newItem: FeedbackRequest) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FeedbackRequest, newItem: FeedbackRequest) = oldItem == newItem
    }

}