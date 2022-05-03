package com.app.transportation.ui.create_order_fragments

import android.R.attr.data
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collect
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.repeatOnLifecycle
import com.app.transportation.databinding.FragmentCreatingAdvertisementBinding
import com.app.transportation.ui.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File


class CreatingAdvertisementFragment : Fragment() {

    private var binding: FragmentCreatingAdvertisementBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private var ctx : Context? = null

    private val categoryId by lazy { arguments?.getInt("id", 1) ?: 1 }
    private val mode by lazy { arguments?.getInt("mode", 1) ?: 0 }//if mode 1 category has 4level item show spinner

    private val obtainPhotoUriLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { uri -> viewModel.cafApplyPhotoByUri(uri) }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreatingAdvertisementBinding.inflate(inflater, container, false)
        ctx = activity
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Профиль"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)

        b.name.text = viewModel.profileFlow.value?.name
        b.telNumber.text = viewModel.profileFlow.value?.telNumber

        applyListeners()

        applyCollectors()
    }

    private fun applyListeners() {
        b.addDescription.setOnClickListener {
            runDescriptionEditor()
        }
        b.addAdvert.setOnClickListener {
            when {
                b.name.text.isBlank() && b.telNumber.text.isBlank() -> {
                    viewModel.messageEvent.tryEmit("В профиле отсутствуют имя и телефон")
                    return@setOnClickListener
                }
                b.name.text.isBlank() -> {
                    viewModel.messageEvent.tryEmit("В профиле отсутствует имя")
                    return@setOnClickListener
                }
                b.telNumber.text.isBlank() -> {
                    viewModel.messageEvent.tryEmit("В профиле отсутствует телефон")
                    return@setOnClickListener
                }
                b.advertTitle.text.isBlank() && b.price.text.isBlank() -> {
                    viewModel.messageEvent.tryEmit("Не заполнены поля с названием и ценой")
                    return@setOnClickListener
                }
                b.advertTitle.text.isBlank() -> {
                    viewModel.messageEvent.tryEmit("Не заполнено поле с названием")
                    return@setOnClickListener
                }
                b.price.text.isBlank() -> {
                    viewModel.messageEvent.tryEmit("Не заполнено поле с ценой")
                    return@setOnClickListener
                }
            }
            val photos = mutableListOf<String>()
            viewModel.cafTempPhotoUris.value.second.firstOrNull()?.let {
                val contentResolver = requireContext().applicationContext.contentResolver
                contentResolver.openInputStream(it)?.use {
                    val base64String = Base64.encodeToString(it.readBytes(), Base64.DEFAULT)
                    File(requireContext().cacheDir.path + "/ttt.txt").writeText(base64String)
                }
            }
            viewModel.cafTempPhotoUris.value.second.forEach { uri ->
                val contentResolver = requireContext().applicationContext.contentResolver
                contentResolver.openInputStream(uri)?.use {
                    val base64String = Base64.encodeToString(it.readBytes(), Base64.DEFAULT)
                    photos.add("'data:image/jpg;base64,$base64String'")
                }
            }

            viewModel.createAdvert(
                title = b.advertTitle.text.toString(),
                price = b.price.text.toString(),
                description = b.description.text.toString(),
                categoryId = categoryId.toString(),
                photos = photos
            )

            findNavController().navigateUp()
        }
        b.description.setOnClickListener {
            runDescriptionEditor()
        }
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
        b.photo.setOnClickListener {
            val position = viewModel.cafTempPhotoUris.value.first
            val currentValue = viewModel.cafTempPhotoUris.value.second.getOrNull(position)
            currentValue?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удалить фото?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.cafRemoveCurrentPhoto()
                    }
                    .setNegativeButton("Нет") { _, _ -> }
                    .show()
            } ?: importViaSystemFE()
        }
        b.prevPhoto.setOnClickListener {
            viewModel.cafPrevPhoto()
        }
        b.nextPhoto.setOnClickListener {
            viewModel.cafNextPhoto()
        }
    }

    private fun applyCollectors() = viewLifecycleOwner.repeatOnLifecycle {
        viewModel.cafTempPhotoUris.collect(this) {
            it.second.getOrNull(it.first)?.let { uri ->
                b.photo.scaleType = ImageView.ScaleType.FIT_XY
                b.photo.setImageURI(uri)
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
        if (mode==1)
            viewModel.addAdvertScreenCategoriesFlowFourthLevel(categoryId).collectWithLifecycle(viewLifecycleOwner) {
                var data : ArrayList<String> = ArrayList()
                data.add("выбрать из списка")
                it.forEach{item ->
                    data.add(item.name)
                }
                val adapter: ArrayAdapter<String> = ArrayAdapter<String>(ctx!!, android.R.layout.simple_spinner_item, data)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                b.spinnerSelectCategory.adapter = adapter

                if (data.size>1)
                    b.spinnerSelectCategory.visibility = View.VISIBLE
            }
    }

    private fun runDescriptionEditor() {
        setFragmentResultListener("Description") { _, bundle ->
            val text = bundle.getString("description") ?: return@setFragmentResultListener
            b.descriptionGroup.isVisible = text.isNotBlank()
            b.addDescription.isVisible = text.isBlank()
            b.description.text = text
        }
        findNavController().navigate(
            R.id.descriptionEditorFragment,
            bundleOf("text" to b.description.text)
        )
    }

    private fun importViaSystemFE() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
        runCatching { obtainPhotoUriLauncher.launch(intent) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.cafTempPhotoUris.value = 0 to mutableListOf()
    }

}