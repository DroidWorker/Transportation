package com.app.transportation.ui.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.R
import com.app.transportation.data.database.entities.Advert
import com.app.transportation.databinding.ItemServiceFirstBinding
import com.app.transportation.databinding.ItemServiceSecondBinding

class AdvertisementsAdapter : ListAdapter<Advert, RecyclerView.ViewHolder>(DiffCallback()) {

    var onClick: (Advert.() -> Unit)? = null

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == 0)
            FirstViewHolder(ItemServiceFirstBinding.inflate(layoutInflater, parent, false))
        else
            SecondViewHolder(ItemServiceSecondBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        with(getItem(position)) {
            if (viewType == 0)
                (holder as FirstViewHolder).bind(this)
            else
                (holder as SecondViewHolder).bind(this)
        }

    override fun getItemViewType(position: Int) = getItem(position).viewType

    fun getItem(id: Long): Advert? = currentList.find { it.id.toLong() == id }

    override fun getItemCount(): Int = currentList.size

    override fun getItemId(position: Int): Long = currentList[position].id.toLong()

    fun getItemPosition(id: Long): Int = currentList.indexOfFirst { it.id.toLong() == id }

    inner class FirstViewHolder(private val binding: ItemServiceFirstBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: Advert) = with(binding) {
                root.setOnClickListener { onClick?.invoke(item) }

                title.text = item.title
                date.text = item.date
                time.text = item.time
                price.isVisible = item.price != null && item.price != "null"
                time.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    horizontalBias = if (price.isVisible) 0.5F else 1F
                }
                price.text = item.price.toString()
                item.photo.firstOrNull()?.let { base64String ->
                    val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    photo.setImageBitmap(bitmap)
                }
                Unit
            }

    }

    inner class SecondViewHolder(private val binding: ItemServiceSecondBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Advert) = with(binding) {
            root.setOnClickListener { onClick?.invoke(item) }

            title.text = item.title
            location.text = item.subtitle
            date.text = item.date
            time.text = item.time
            price.isVisible = item.price != null && item.price != "null"
            time.updateLayoutParams<ConstraintLayout.LayoutParams> {
                horizontalBias = if (price.isVisible) 0.5F else 1F
            }
            price.text = item.price.toString()
        }

    }

    private class DiffCallback : DiffUtil.ItemCallback<Advert>() {
        override fun areItemsTheSame(oldItem: Advert, newItem: Advert) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Advert, newItem: Advert) = oldItem == newItem
    }

}