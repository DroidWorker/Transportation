package com.app.transportation.ui.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.R
import com.app.transportation.core.blurRenderScript
import com.app.transportation.data.database.entities.Advert
import com.app.transportation.databinding.*
import com.google.android.material.snackbar.Snackbar

class AdvertisementsAdapter : ListAdapter<Advert, RecyclerView.ViewHolder>(DiffCallback()) {

    var onClick: (Advert.() -> Unit)? = null
    lateinit var ctx : Context
    var iscustomer = false
    var isSearchResult = false

    init {
        setHasStableIds(true)
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        ctx = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return if(isSearchResult)
            SearchResultViewHolder(ItemServiceSearchBinding.inflate(layoutInflater, parent, false))
        else if (viewType == 0)
            FirstViewHolder(ItemServiceFirstBinding.inflate(layoutInflater, parent, false))
        else if(viewType == 1)
            SecondViewHolder(ItemServiceSecondBinding.inflate(layoutInflater, parent, false))
        else
            fillerViewHolder(ItemNoadvertsFillerBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        with(getItem(position)) {
            if(isSearchResult)
                (holder as SearchResultViewHolder).bind(this)
            else if (viewType == 0)
                (holder as FirstViewHolder).bind(this)
            else if(viewType == 1)
                (holder as SecondViewHolder).bind(this)
            else
                (holder as fillerViewHolder).bind(this)
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
                        try {
                            val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                            photo.setImageBitmap(bitmap)
                            val blurred: Bitmap? = blurRenderScript(ctx, bitmap, 25)//second parametre is radius//second parametre is radius
                            photo.setBackgroundDrawable(BitmapDrawable(blurred))
                        }
                        catch (ex : Exception){
                        }
                    }


                item.options.forEach{
                    if(it.option_id=="2"&&it.status=="ON")
                        backLayout.setBackgroundResource(R.drawable.card_gradient)
                }
                Unit
            }

    }

    inner class SecondViewHolder(private val binding: ItemServiceSecondBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Advert) = with(binding) {
            root.setOnClickListener { onClick?.invoke(item) }
            title.text = item.title
            location.text = item.fromCity+" "+item.fromRegion+" "+item.fromPlace
            date.text = item.date
            time.text = item.time
            price.isVisible = item.price != null && item.price != "null"
            time.updateLayoutParams<ConstraintLayout.LayoutParams> {
                horizontalBias = if (price.isVisible) 0.5F else 1F
            }
            price.text = item.price.toString()
            if (price.text == "-1") price.visibility = View.GONE
            item.options.forEach{
                if(it.option_id=="2"&&it.status=="ON")
                    backLayout.setBackgroundResource(R.drawable.card_gradient)
            }
            if (date.text=="null") date.visibility=View.GONE
            if (time.text=="null") time.visibility=View.GONE
        }

    }

    inner class SearchResultViewHolder(private val binding: ItemServiceSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Advert) = with(binding) {
            root.setOnClickListener { onClick?.invoke(item) }
            title.text = item.title
            price.isVisible = item.price != null && item.price != "null"
            price.text = item.price.toString()
            if (price.text == "-1") price.visibility = View.GONE
        }

    }


    inner class fillerViewHolder (private val binding: ItemNoadvertsFillerBinding) ://call if list empty
    RecyclerView.ViewHolder(binding.root){

        fun bind(item : Advert) = with(binding){
            titla.text = item.title
            titlb.text = item.subtitle
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Advert>() {
        override fun areItemsTheSame(oldItem: Advert, newItem: Advert) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Advert, newItem: Advert) = oldItem == newItem
    }

}