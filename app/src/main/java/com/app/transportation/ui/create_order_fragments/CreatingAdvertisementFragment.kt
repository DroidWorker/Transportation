package com.app.transportation.ui.create_order_fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.Settings.Global.putInt
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.PaymentActivity
import com.app.transportation.R
import com.app.transportation.core.*
import com.app.transportation.databinding.FragmentCreatingAdvertisementBinding
import com.app.transportation.ui.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.ByteArrayOutputStream
import java.io.File


class CreatingAdvertisementFragment : Fragment() {

    private var binding: FragmentCreatingAdvertisementBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private var ctx : Context? = null

    private val categoryId by lazy { arguments?.getInt("id", 1) ?: 1 }
    private val mode by lazy { arguments?.getInt("mode", 1) ?: 0 }//if mode 1 category has 4level item show spinner
    private  val isEdit by lazy { arguments?.getInt("isEdit", 0) ?: 0}
    private var EditCatId : Int? = 0

    private var catsID : HashMap<String, String> = HashMap<String, String>()
    private var selectedCat : String = ""

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
            if (isEdit==0)
                b.title.text = "??????????????"
            else
                b.title.text = "????????????????????????????"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)

        b.name.text = viewModel.profileFlow.value?.name
        b.telNumber.text = viewModel.profileFlow.value?.telNumber
        if (isEdit==1) {
            b.addAdvert.text = "??????????????????"
            b.addToFavourites.visibility = View.GONE

            viewModel.userOrdersAdvertsFlow.collectWithLifecycle(this) {
                it.forEach { item ->
                    if (item.id==categoryId) {
                        EditCatId = item.id
                        b.advertTitle.setText(item.title)
                        b.price.setText(item.price)
                        b.location.text = viewModel.profileFlow.value?.cityArea
                        if (b.description.text.isEmpty()) {
                            b.descriptionGroup.isVisible = item.description.isNotBlank()
                            b.addDescription.isVisible = item.description.isBlank()
                            b.description.text = item.description
                        }
                        item.photo.firstOrNull()?.let { base64String ->
                            try {
                                val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                                b.photo.setImageBitmap(bitmap)
                                val blurred: Bitmap? = blurRenderScript(ctx, bitmap, 25)//second parametre is radius//second parametre is radius
                                b.photo.setBackgroundDrawable(BitmapDrawable(blurred))
                            }
                            catch (ex : Exception){
                                println("Error: "+ex.message.toString())
                            }
                        }
                    }
                }
            }
        }

        applyListeners()

        applyCollectors()
    }

    private fun applyListeners() {
        b.addDescription.setOnClickListener {
            runDescriptionEditor()
        }
        b.addAdvert.setOnClickListener {
            if (isEdit==0) {
                when {
                    b.spinnerSelectCategory.isVisible&&selectedCat.isBlank() ->{
                        viewModel.messageEvent.tryEmit("???? ?????????????? ??????????????????")
                        return@setOnClickListener
                    }
                    b.name.text.isBlank() && b.telNumber.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("?? ?????????????? ?????????????????????? ?????? ?? ??????????????")
                        return@setOnClickListener
                    }
                    b.name.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("?? ?????????????? ?????????????????????? ??????")
                        return@setOnClickListener
                    }
                    b.telNumber.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("?? ?????????????? ?????????????????????? ??????????????")
                        return@setOnClickListener
                    }
                    b.advertTitle.text.isBlank() && b.price.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("???? ?????????????????? ???????? ?? ?????????????????? ?? ??????????")
                        return@setOnClickListener
                    }
                    b.advertTitle.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("???? ?????????????????? ???????? ?? ??????????????????")
                        return@setOnClickListener
                    }
                    b.price.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("???? ?????????????????? ???????? ?? ??????????")
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
                        var resultBitmap : Bitmap? = decodeSampledBitmapFromResource(it.readBytes(), 300, 200)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        resultBitmap?.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
                        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
                        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
                        photos.add("'data:image/jpg;base64,$base64String'")
                    }
                }
                var optionList: ArrayList<String> = ArrayList()
                var summ: Int = 0
                if (b.photoInShowcaseCB.isChecked) {
                    optionList.add("1")
                    summ+=50
                }
                if (b.colorHighlightingCB.isChecked) {
                    optionList.add("2")
                    summ+=30
                }
                if (b.newOrderNotificationCB.isChecked) {
                    optionList.add("3")
                    summ+=50
                }
                viewModel.createAdvert(
                    ctx=context,
                    title = b.advertTitle.text.toString(),
                    price = b.price.text.toString(),
                    description = b.description.text.toString(),
                    categoryId = if (b.spinnerSelectCategory.isVisible)
                        catsID.getValue(selectedCat!!)
                    else
                        categoryId.toString(),
                    photos = photos,
                    options = optionList.toList()
                )
                if (summ>0) {
                    val payIntent = Intent(activity, PaymentActivity::class.java)
                    payIntent.putExtra("summ", summ);
                    startActivity(payIntent)
                }
                findNavController().navigateUp()
            }
            else{
                when {
                    b.name.text.isBlank() && b.telNumber.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("?? ?????????????? ?????????????????????? ?????? ?? ??????????????")
                        return@setOnClickListener
                    }
                    b.name.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("?? ?????????????? ?????????????????????? ??????")
                        return@setOnClickListener
                    }
                    b.telNumber.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("?? ?????????????? ?????????????????????? ??????????????")
                        return@setOnClickListener
                    }
                    b.advertTitle.text.isBlank() && b.price.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("???? ?????????????????? ???????? ?? ?????????????????? ?? ??????????")
                        return@setOnClickListener
                    }
                    b.advertTitle.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("???? ?????????????????? ???????? ?? ??????????????????")
                        return@setOnClickListener
                    }
                    b.price.text.isBlank() -> {
                        viewModel.messageEvent.tryEmit("???? ?????????????????? ???????? ?? ??????????")
                        return@setOnClickListener
                    }
                    b.photo.tag != 1 -> {
                        viewModel.messageEvent.tryEmit("???????? ???? ??????????????????")
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
                viewModel.editAdvert(
                    id = EditCatId.toString(),
                    title = b.advertTitle.text.toString(),
                    price = b.price.text.toString(),
                    description = b.description.text.toString(),
                    photos = photos
                )
                findNavController().navigateUp()
            }
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
                    .setTitle("?????????????? ?????????")
                    .setPositiveButton("????") { _, _ ->
                        viewModel.cafRemoveCurrentPhoto()
                    }
                    .setNegativeButton("??????") { _, _ -> }
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
                b.photo.tag = 1
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
                data.add("?????????????? ???? ????????????")
                it.forEach{item ->
                    data.add(item.name)
                    catsID[item.name] = item.realId.toString()
                }
                val adapter: ArrayAdapter<String> = ArrayAdapter<String>(ctx!!, android.R.layout.simple_spinner_item, data)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                b.spinnerSelectCategory.adapter = adapter

                if (data.size>1) {
                    b.spinnerSelectCategory.visibility = View.VISIBLE
                    b.spinnerSelectCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }

                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (position!=0)
                                selectedCat = b.spinnerSelectCategory.getItemAtPosition(position).toString()
                        }

                    }
                }
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