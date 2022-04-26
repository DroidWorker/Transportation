package com.app.transportation.main.core

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.ui.adapters.ServiceTypeAdapter
import com.app.transportation.main.core.selection.ItemDetailsLookup
import com.app.transportation.main.core.selection.ItemKeyProvider

class TextKeyProvider(private val adapter: ServiceTypeAdapter) : ItemKeyProvider<Long>(SCOPE_CACHED) {
    override fun getKey(position: Int) = adapter.getItemId(position)
    override fun getPosition(key: Long) = adapter.getItemPosition(key)
}

class TextDetailsLookup(private val recyclerView: RecyclerView): ItemDetailsLookup<Long>() {

    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        return view?.let {
            (recyclerView.getChildViewHolder(it) as ServiceTypeAdapter.ViewHolder).getItemDetails()
        }
    }

}

/*class TaskKeyProvider(private val adapter: TasksAdapter) : ItemKeyProvider<Long>(SCOPE_CACHED) {
    override fun getKey(position: Int) = adapter.getItemId(position)
    override fun getPosition(key: Long) = adapter.getItemPosition(key)
}

class TaskDetailsLookup(private val recyclerView: RecyclerView): ItemDetailsLookup<Long>() {

    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        return view?.let {
            (recyclerView.getChildViewHolder(it) as TasksAdapter.ViewHolder).getItemDetails()
        }
    }

}

class RestoreItemKeyProvider(private val adapter: RestoreItemsAdapter) : ItemKeyProvider<Long>(SCOPE_CACHED) {
    override fun getKey(position: Int) = adapter.getItemId(position)
    override fun getPosition(key: Long) = adapter.getItemPosition(key)
}

class RestoreItemDetailsLookup(private val recyclerView: RecyclerView): ItemDetailsLookup<Long>() {

    override fun getItemDetails(event: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(event.x, event.y)
        return view?.let {
            (recyclerView.getChildViewHolder(it) as RestoreItemsAdapter.ViewHolder).getItemDetails()
        }
    }

}*/