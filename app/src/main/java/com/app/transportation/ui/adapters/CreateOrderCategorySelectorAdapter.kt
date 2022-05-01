package com.app.transportation.ui.adapters

import com.app.transportation.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.MainActivity
import com.app.transportation.data.database.entities.SelectorCategory
import com.app.transportation.databinding.EmptyItemBinding
import com.app.transportation.databinding.ItemAdvertCategoryTextItemBinding
import com.app.transportation.databinding.ItemCategoryBinding
import com.app.transportation.databinding.ItemCategoryImageBinding


 class CreateOrderCategorySelectorAdapter :
    ListAdapter<SelectorCategory, RecyclerView.ViewHolder>(DiffCallback()) {
    var mode = 0//0-call from cabinet 1-call from main screen 2-show 4level categories 4-CategoryImageAdapter
    var onClick: ((Int, Int) -> Unit)? = null

    var viewState : HashMap<Int, Boolean> = HashMap<Int, Boolean>()
    var parentIds : ArrayList<Int> = ArrayList<Int>()

    var openID = 0//used with 4level categories

    lateinit var ctx : Context
     var imgTableIds : HashMap<Int, Int> = HashMap()
     init {
         imgTableIds.put(4,0)
         imgTableIds.put(8,1)
         imgTableIds.put(12,2)
         imgTableIds.put(13,3)
         imgTableIds.put(14,4)
         imgTableIds.put(24,5)
     }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
ctx = parent.getContext()
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == 2&&mode!=4)
            CategoryViewHolder(ItemCategoryBinding.inflate(layoutInflater, parent, false))
        else if(viewType==2&&mode==4)
            CategoryImageViewHolder(ItemCategoryImageBinding.inflate(layoutInflater, parent, false))
        else if(viewType == 3&&mode!=2)
            TextViewHolder(ItemAdvertCategoryTextItemBinding.inflate(layoutInflater, parent, false))
        else if (mode!=2)
            EmptyViewHolder(EmptyItemBinding.inflate(layoutInflater, parent, false))
        else if (mode==2&&viewType!=4)
            CategoryViewHolder(ItemCategoryBinding.inflate(layoutInflater, parent, false))
        else
            TextViewHolder(ItemAdvertCategoryTextItemBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        with(getItem(position)) {
            if (level == 2&&mode!=4)
                (holder as CategoryViewHolder).bind(this)
            else if(level == 2&& mode==4) {
                parentIds.add(parentId)
                (holder as CategoryImageViewHolder).bind(this)
            }
            else if(level == 3&&mode!=2)
                (holder as TextViewHolder).bind(this)
            else if(mode!=2){
                parentIds.add(parentId)
                (holder as EmptyViewHolder).bind(this)
            }
            else if (mode==2&&level!=4)
                (holder as CategoryViewHolder).bind(this)
            else
                (holder as TextViewHolder).bind(this)
        }

    override fun getItemViewType(position: Int) = getItem(position).level

    fun getItem(id: Long): SelectorCategory? = currentList.find { it.id.toLong() == id }

    override fun getItemId(position: Int): Long = currentList[position].id.toLong()

    fun getItemPosition(id: Long): Int = currentList.indexOfFirst { it.id.toLong() == id }

    inner class TextViewHolder(private val binding: ItemAdvertCategoryTextItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SelectorCategory) = with(binding) {
            //root.setOnClickListener { onClick?.invoke(item.realId) }
            root.setOnClickListener { onClick?.invoke(item.parentId, item.realId) }
            text.text = item.name
            if ((mode==1||mode==4)&&viewState[item.parentId]==false){
                text.visibility=View.GONE
            }
            else
                text.visibility=View.VISIBLE
            if(mode==2&&item.parentId==openID){
                text.visibility=View.VISIBLE
            }
            else if (mode==2&&item.parentId!=openID)
                text.visibility=View.GONE
        }
    }

    inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SelectorCategory) {
            if (mode==2&&item.realId!=openID)
                binding.button.visibility = View.GONE
            else if (mode==2&&item.realId==openID)
                binding.button.visibility = View.VISIBLE

            if (mode==1&&!viewState.containsKey(item.realId)){
                viewState[item.realId] = false
            }
            if (mode==1){
                binding.button.setOnClickListener(View.OnClickListener {
                    viewState[item.realId] = viewState[item.realId] != true
                    notifyDataSetChanged()
                })
            }
            binding.button.text = item.name
        }
    }

    inner class CategoryImageViewHolder(private  val binding: ItemCategoryImageBinding):
    RecyclerView.ViewHolder(binding.root){

        @SuppressLint("ResourceType")
        fun bind(item: SelectorCategory){
            if (mode==4&&!viewState.containsKey(item.realId)){
                viewState[item.realId] = false
            }
            if (mode==4){
                binding.frameLayout.setOnClickListener(View.OnClickListener {
                    viewState[item.realId] = viewState[item.realId] != true
                    notifyDataSetChanged()
                })
            }
            var imgs : TypedArray = ctx.getResources().obtainTypedArray(R.array.myDrawables);

            binding.catimage.setImageResource(imgs.getResourceId(imgTableIds.getValue(item.realId), 0))
            binding.category.text = item.name
        }
    }

    inner class EmptyViewHolder(private val binding: EmptyItemBinding) :
    RecyclerView.ViewHolder(binding.root){
        fun bind(item: SelectorCategory){

        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SelectorCategory>() {
        override fun areItemsTheSame(oldItem: SelectorCategory, newItem: SelectorCategory) = oldItem == newItem
        override fun areContentsTheSame(oldItem: SelectorCategory, newItem: SelectorCategory) = oldItem == newItem
    }

}