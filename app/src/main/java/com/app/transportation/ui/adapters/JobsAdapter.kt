package com.app.transportation.ui.adapters

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.R
import com.app.transportation.core.blurRenderScript
import com.app.transportation.core.dpToPx
import com.app.transportation.data.api.BusinessLastItemDTO
import com.app.transportation.data.database.entities.Job
import com.app.transportation.databinding.ItemJobBinding
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject

class JobsAdapter : ListAdapter<BusinessLastItemDTO, JobsAdapter.ViewHolder>(DiffCallback()) {
    public var mode:Boolean = false;//day
    lateinit var ctx : Context
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

            fun bind(item: BusinessLastItemDTO) = with(binding) {
                if(!mode){
                    cardfilter.setImageResource(R.drawable.card_gradient)
                }
                if (bindingAdapterPosition == 0) {
                    root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        marginStart = itemView.context.dpToPx(25)
                    }
                }

                val base64String = item.photo.replace("data:image/jpg;base64,", "")
                try {
                    if (base64String.length>10) {
                        val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                        catimage.scaleType = ImageView.ScaleType.FIT_CENTER
                        catimage.setImageBitmap(bitmap)
                        val blurred: Bitmap? = blurRenderScript(
                            ctx,
                            bitmap,
                            25
                        )//second parametre is radius//second parametre is radius
                        catimage.setBackgroundDrawable(BitmapDrawable(blurred))
                    }
                    else{
                        catimage.setImageResource(R.drawable.ic_photo)
                        catimage.scaleType = ImageView.ScaleType.CENTER
                    }
                }
                catch (ex : Exception){
                    println("Error: "+ex.message.toString())
                }
                title.text = item.category
                price.text = item.price+" p"
            }

    }

    private class DiffCallback : DiffUtil.ItemCallback<BusinessLastItemDTO>() {
        override fun areItemsTheSame(oldItem: BusinessLastItemDTO, newItem: BusinessLastItemDTO) = oldItem == newItem
        override fun areContentsTheSame(oldItem: BusinessLastItemDTO, newItem: BusinessLastItemDTO) = oldItem == newItem
    }

}