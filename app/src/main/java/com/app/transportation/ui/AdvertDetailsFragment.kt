package com.app.transportation.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.blurRenderScript
import com.app.transportation.core.collect
import com.app.transportation.core.repeatOnLifecycle
import com.app.transportation.databinding.FragmentAdvertDetailsBinding


class AdvertDetailsFragment : Fragment() {

    private var binding: FragmentAdvertDetailsBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private var popupWindow: PopupWindow? = null

    private val id by lazy { arguments?.getLong("id") ?: 0L }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdvertDetailsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Все категории"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)

        applyObservers()

        applyListeners()
    }

    private fun applyObservers() = viewLifecycleOwner.repeatOnLifecycle {
        viewModel.cachedAdvert.collect(this) { advert ->
            if (advert!=null) viewModel.getProfile(advert?.userId ?: "")
            advert?.apply {
                b.progressBar4.visibility = View.GONE
                b.title.text = title
                b.addToFavourites.setColorFilter(0)
                b.addToFavourites.tag = id
                b.name.text = "Какое-то имя"
                b.telNumber.text = "Какой-то номер"
                b.location.text = fromCity
                b.imageNumber.text = ""
                b.price.text = price
                b.description.text = description
                viewModel.applyAdfPhotos(photo)
                if (options.isNotEmpty()){
                    options.forEach{
                        if(it.option_id=="-1"&&it.status=="ACTIVE"){
                            b.addToFavourites.visibility = View.VISIBLE
                            return@forEach
                        }
                    }
                }
            }
        }
        viewModel.cachedProfile.collect(this){
            b.name.text = it.firstName
            //b.location.text = it.location
            b.telNumber.text = it.phone
        }
        viewModel.adfTempPhotoUris.collect(this) {
            it.second.getOrNull(it.first)?.let { bitmap ->
                b.photo.scaleType = ImageView.ScaleType.FIT_CENTER
                b.photo.setImageBitmap(bitmap)
                val blurred: Bitmap? = blurRenderScript(requireContext(), bitmap, 25)//second parametre is radius//second parametre is radius
                b.photo.setBackgroundDrawable(BitmapDrawable(blurred))
            } ?: kotlin.run {
                b.photo.scaleType = ImageView.ScaleType.CENTER_INSIDE
                b.photo.setImageResource(R.drawable.ic_photo)
            }
            b.imageNumber.text =
                if (it.second.size > 1) {
                    if (it.first != it.second.size)
                        "${it.first+1}/${it.second.size}"
                    else
                        "${it.first}/${it.second.size}"
                }
                else
                    ""
            b.prevPhoto.isVisible = it.first > 0
            b.nextPhoto.isGone = it.first+1 > it.second.size
        }
    }

    private fun applyListeners() {
        b.descriptionExpandText.setOnClickListener {
            val isCollapsed = b.description.maxLines == 4

            if (isCollapsed) {
                b.description.maxLines = Int.MAX_VALUE
                b.description.ellipsize = null
                b.descriptionExpandText.text = getString(R.string.collapse_text)
            } else {
                b.description.maxLines = 4
                b.description.ellipsize = TextUtils.TruncateAt.END
                b.descriptionExpandText.text = getString(R.string.expand_text)
            }
        }
        b.addToFavourites.setOnClickListener {
            b.addToFavourites.setColorFilter(1)
            viewModel.addAdvertFavorite(b.addToFavourites.tag.toString())
        }
        b.acceptService.setOnClickListener {
            viewModel.addAdvertPing(b.addToFavourites.tag.toString())
        }
        b.prevPhoto.setOnClickListener {
            viewModel.adfPrevPhoto()
        }
        b.nextPhoto.setOnClickListener {
            viewModel.adfNextPhoto()
        }
        b.telNumber.setOnClickListener{
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${b.telNumber.text}"))
            //if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent)
            //}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        popupWindow = null
    }

}