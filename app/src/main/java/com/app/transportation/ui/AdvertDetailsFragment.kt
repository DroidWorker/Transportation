package com.app.transportation.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.TextView
import android.widget.Toast
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

    var isFullscreen = false

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
            if (advert!=null) {
                viewModel.getProfile(advert?.userId ?: "")
            }
            advert?.apply {
                b.progressBar4.visibility = View.GONE
                b.title.text = title
                b.addToFavourites.setColorFilter(0)
                b.addToFavourites.tag = id
                //b.name.text = "Какое-то имя"
                //b.telNumber.text = "Какой-то номер"
                b.location.text = fromCity
                b.imageNumber.text = ""
                b.price.text = price
                b.description.text = description
                viewModel.applyAdfPhotos(photo)
                if (options.isNotEmpty()){
                    options.forEach{
                        if(it.option_id=="-1"&&it.status=="ACTIVE"){
                            b.addToFavourites.visibility = View.VISIBLE
                        }
                        else if(it.option_id=="4"&&it.status=="ON"){
                            b.advertTarifMode.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
        viewModel.cachedProfile.collect(this){
            b.name.text = it.firstName
            if(b.location.text.length<2)b.location.text = it.location
            b.telNumber.text = it.phone
        }
        viewModel.adfTempPhotoUris.collect(this) {
            it.second.getOrNull(it.first)?.let { bitmap ->
                if (isFullscreen) {
                    b.fullscreenImage.setImageBitmap(bitmap)
                }
                b.photo.scaleType = ImageView.ScaleType.FIT_CENTER
                b.photo.setImageBitmap(bitmap)
                val blurred: Bitmap? = blurRenderScript(requireContext(), bitmap, 25)//second parametre is radius//second parametre is radius
                b.photo.setBackgroundDrawable(BitmapDrawable(blurred))
            } ?: kotlin.run {
                /*b.photo.scaleType = ImageView.ScaleType.CENTER_INSIDE
                b.photo.setImageResource(R.drawable.ic_photo)*/
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
            b.nextPhoto.isGone = it.first+2 > it.second.size
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
        b.photo.setOnClickListener {
            b.fullscreenCV.visibility = View.VISIBLE
            viewModel.adfTempPhotoUris.value.second.getOrNull(viewModel.adfTempPhotoUris.value.first)?.let {
                b.fullscreenImage.setImageBitmap(it)
            }
            isFullscreen = true
        }
        b.fullscreenCV.setOnClickListener {
            b.fullscreenCV.visibility = View.GONE
            isFullscreen = false
        }
        b.fullscreenNext.setOnClickListener {
            viewModel.adfNextPhoto()
        }
        b.fullscreenPrev.setOnClickListener {
            viewModel.adfPrevPhoto()
        }
        b.telNumber.setOnClickListener{
            /*val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${b.telNumber.text}"))
            startActivity(intent)*/
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Выберите действие")
            builder.setItems(arrayOf("Позвонить", "Написать в мессенджер")) { _, which ->
                val phone = b.telNumber.text.toString().replace("+", "").replace(" ", "")
                try{
                    when (which) {
                        0 -> {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone}"))
                            startActivity(intent)
                        }
                        1 -> {
                            val url = "https://api.whatsapp.com/send?phone=$phone"
                            try {
                                val pm = context!!.packageManager
                                pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
                                val i = Intent(Intent.ACTION_VIEW)
                                i.data = Uri.parse(url)
                                startActivity(i)
                            } catch (e: PackageManager.NameNotFoundException) {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse("viber://chat?number=$phone")
                                startActivity(intent)
                            }
                        }
                    }}catch (ex : Exception){
                    Toast.makeText(requireContext(), "ошибка", Toast.LENGTH_SHORT).show()
                }
            }
            builder.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        popupWindow = null
    }

}