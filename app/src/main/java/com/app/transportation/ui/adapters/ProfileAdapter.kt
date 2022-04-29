package com.app.transportation.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.data.database.entities.ProfileRvItem
import com.app.transportation.databinding.ItemCategoryBinding
import com.app.transportation.databinding.ItemProfileAddItemBinding
import com.app.transportation.databinding.ItemProfileAdvertBinding

class ProfileAdapter :
    ListAdapter<ProfileRvItem, RecyclerView.ViewHolder>(DiffCallback()) {

    var mode = 0;

    var onClick: (Int.() -> Unit)? = null
    var onEditClick: ((realId : Int, advId : Int) -> Unit)? = null
    var onDeleteClick: ((isOrder: Boolean, id: Int, position: Int) -> Unit)? = null
    var onAddItemClick: (Int.() -> Unit)? = null

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            1 -> CategoryViewHolder(ItemCategoryBinding.inflate(layoutInflater, parent, false))
            2 -> AdvertViewHolder(ItemProfileAdvertBinding.inflate(layoutInflater, parent, false))
            else -> AddItemViewHolder(ItemProfileAddItemBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        with(getItem(position)) {
            when (viewType) {
                1 -> (holder as CategoryViewHolder).bind(this)
                2 -> (holder as AdvertViewHolder).bind(this)
                else -> {
                    (holder as AddItemViewHolder).bind(this)
                }
            }

        }

    override fun getItemViewType(position: Int) = getItem(position).viewType

    fun getItem(id: Long): ProfileRvItem? = currentList.find { it.id == id }

    override fun getItemId(position: Int): Long = currentList[position].id

    fun getItemPosition(id: Long): Int = currentList.indexOfFirst { it.id == id }

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfileRvItem) = with(binding) {
            button.text = item.title
        }

    }

    inner class AdvertViewHolder(private val binding: ItemProfileAdvertBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfileRvItem) = with(binding) {
            title.text = item.title

            root.setOnClickListener { item.realId?.let { id -> onClick?.invoke(id) } }
            edit.setOnClickListener { item.realId?.let { id -> onEditClick?.invoke(item.realId, item.categoryId!!) } }
            delete.setOnClickListener {
                it.isEnabled = false
                item.realId?.let { id ->
                    onDeleteClick?.invoke("(заказ)" in item.title, id, bindingAdapterPosition)
                }
            }
        }

    }

    inner class AddItemViewHolder(private val binding: ItemProfileAddItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfileRvItem) = with(binding) {
            if (mode==1)
                this.addItemTitle.text = "открыть категории"
            root.setOnClickListener {
                item.categoryId?.let { id -> onAddItemClick?.invoke(id) }
            }
        }

    }

    private class DiffCallback : DiffUtil.ItemCallback<ProfileRvItem>() {
        override fun areItemsTheSame(oldItem: ProfileRvItem, newItem: ProfileRvItem) = oldItem == newItem
        override fun areContentsTheSame(oldItem: ProfileRvItem, newItem: ProfileRvItem) = oldItem == newItem
    }

}