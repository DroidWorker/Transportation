package com.app.transportation.ui.create_order_fragments

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.MutableList
import kotlin.collections.emptyList
import kotlin.collections.firstOrNull
import kotlin.collections.forEach
import kotlin.collections.getOrNull
import kotlin.collections.getValue
import kotlin.collections.isNotEmpty
import kotlin.collections.mutableListOf
import kotlin.collections.set
import kotlin.collections.toList
import kotlin.collections.toMutableList


class CreatingAdvertisementFragment : Fragment() {

    private var binding: FragmentCreatingAdvertisementBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private var ctx : Context? = null

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private val userId : String? = prefs.getString("myId", null).takeIf { it != "" }

    private var userEmail = "";

    private val categoryId by lazy { arguments?.getInt("id", 1) ?: 1 }
    private val mode by lazy { arguments?.getInt("mode", 1) ?: 0 }//if mode 1 category has 4level item show spinner
    private  val isEdit by lazy { arguments?.getInt("isEdit", 0) ?: 0}
    private var EditCatId : Int? = 0

    private var catsID : HashMap<String, String> = HashMap<String, String>()
    private var selectedCat : String = ""
    private var spinnerPosition = 0

    private var flagDataLoaded = false
    private var newImagesCount = 0//use for switch internal/external photo

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
                b.title.text = "Профиль"
            else
                b.title.text = "Редактирование"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        if (!flagDataLoaded) {
            viewModel.getMyAdverts()
            viewModel.adfTempPhotoUris.value = 0 to mutableListOf()
        }

        super.onViewCreated(view, savedInstanceState)

        b.name.text = viewModel.profileFlow.value?.name
        b.location.text = viewModel.profileFlow.value?.cityArea
        b.telNumber.text = viewModel.profileFlow.value?.telNumber
        if (isEdit==1) {
            b.addAdvert.text = "Применить"
            b.addToFavourites.visibility = View.GONE
            b.photoInShowcase.visibility = View.GONE
            b.photoInShowcaseCB.visibility = View.GONE
            b.colorHighlighting.visibility = View.GONE
            b.colorHighlightingCB.visibility = View.GONE
            b.newOrderNotification.visibility = View.GONE
            b.newOrderNotificationCB.visibility = View.GONE

            viewModel.cachedAdvertsSF.collectWithLifecycle(this) {
                if (flagDataLoaded) return@collectWithLifecycle
                else if(it.isNotEmpty())flagDataLoaded = true
                it.forEach { item ->
                    if (item.id==categoryId) {
                        EditCatId = item.id
                        b.advertTitle.setText(item.title)
                        b.price.setText(item.price)
                        b.location.text = item.fromCity
                        if (b.description.text.isEmpty()) {
                            b.descriptionGroup.isVisible = item.description.isNotBlank()
                            b.addDescription.isVisible = item.description.isBlank()
                            b.description.text = item.description
                        }
                        if (item.photo.size>1){
                            val plist: MutableList<Bitmap> = emptyList<Bitmap>().toMutableList()
                            item.photo.forEach{
                                val byteArray = Base64.decode(it, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                                plist.add(bitmap)
                            }
                            viewModel.adfTempPhotoUris.tryEmit(Pair(1, plist.toMutableList()))
                        }else{
                        item.photo.firstOrNull()?.let { base64String ->
                            try {
                                val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                                b.photo.tag = 0
                                b.photo.scaleType = ImageView.ScaleType.CENTER_INSIDE
                                b.photo.setImageBitmap(bitmap)
                                val blurred: Bitmap? = blurRenderScript(ctx, bitmap, 25)//second parametre is radius//second parametre is radius
                                b.photo.setBackgroundDrawable(BitmapDrawable(blurred))
                            }
                            catch (ex : Exception){
                                println("Error: "+ex.message.toString())
                            }
                        }}
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
                        viewModel.messageEvent.tryEmit("Не выбрана категория")
                        return@setOnClickListener
                    }
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
                    b.description.text.isBlank() ->{
                        viewModel.messageEvent.tryEmit("Не заполнено описание")
                        return@setOnClickListener
                    }
                }
                val photos = mutableListOf<String>()
                /*viewModel.cafTempPhotoUris.value.second.firstOrNull()?.let {
                    val contentResolver = requireContext().applicationContext.contentResolver
                    contentResolver.openInputStream(it)?.use {
                        val base64String = Base64.encodeToString(it.readBytes(), Base64.DEFAULT)
                        File(requireContext().cacheDir.path + "/ttt.txt").writeText(base64String)
                    }
                }*/
                viewModel.cafTempPhotoUris.value.second.forEach { uri ->
                    val contentResolver = requireContext().applicationContext.contentResolver
                    contentResolver.openInputStream(uri)?.use {
                        var resultBitmap : Bitmap? = decodeSampledBitmapFromResource(it.readBytes(), 300, 200)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        resultBitmap?.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
                        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
                        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
                        photos.add("${photos.size}:'data:image/jpg;base64,$base64String'")
                    }
                }
                var optionList: ArrayList<String> = ArrayList()
                var summ: Int = 0
                if (b.photoInShowcaseCB.isChecked) {
                    optionList.add("1")
                    summ+=5000
                }
                if (b.colorHighlightingCB.isChecked) {
                    optionList.add("2")
                    summ+=3000
                }
                if (b.newOrderNotificationCB.isChecked) {
                    optionList.add("3")
                    summ+=5000
                }
                //payment realisation
                viewModel.createAdvert(
                    ctx = context,
                    title = b.advertTitle.text.toString(),
                    price = b.price.text.toString(),
                    city = viewModel.profileFlow.value?.cityArea ?: "",
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
                    payIntent.putExtra("mode", 2)
                    payIntent.putExtra("id", userId?.toInt())
                    payIntent.putExtra("email", userEmail)
                    startActivity(payIntent)
                }
                //findNavController().navigateUp()
                findNavController().popBackStack(R.id.mainFragment, false)
            }
            else{
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
                var photos : MutableList<String>? = mutableListOf<String>()
                if(b.photo.tag==1) {
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
                            resultBitmap?.compress(Bitmap.CompressFormat.JPEG, 40, byteArrayOutputStream)
                            val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
                            val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
                            photos?.add("${photos?.size}:'data:image/jpg;base64,$base64String'")
                        }
                    }
                    if (photos?.size==0) photos = null
                }else photos = null
                var optionList: ArrayList<String> = ArrayList()
                var summ: Int = 0
                if (b.photoInShowcaseCB.isChecked) {
                    optionList.add("1")
                    summ+=5000
                }
                if (b.colorHighlightingCB.isChecked) {
                    optionList.add("2")
                    summ+=3000
                }
                if (b.newOrderNotificationCB.isChecked) {
                    optionList.add("3")
                    summ+=5000
                }
                viewModel.editAdvert(
                    id = EditCatId.toString(),
                    title = b.advertTitle.text.toString(),
                    price = b.price.text.toString(),
                    description = b.description.text.toString(),
                    photos = photos,
                    optionList.toList()
                )
                if (summ>0) {
                    val payIntent = Intent(activity, PaymentActivity::class.java)
                    payIntent.putExtra("summ", summ);
                    payIntent.putExtra("mode", 2)
                    payIntent.putExtra("id", userId?.toInt())
                    payIntent.putExtra("email", userEmail)
                    startActivity(payIntent)
                }
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
                    .setTitle("Удалить фото?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.cafRemoveCurrentPhoto()
                    }
                    .setNegativeButton("Нет") { _, _ ->
                        importViaSystemFE()
                    }
                    .show()
            } ?: importViaSystemFE()
        }
        b.prevPhoto.setOnClickListener {
            viewModel.cafPrevPhoto()
            if (isEdit == 1)viewModel.adfPrevPhoto()
        }
        b.nextPhoto.setOnClickListener {
            viewModel.cafNextPhoto()
            if (isEdit == 1)viewModel.adfNextPhoto()
        }
    }

    private fun applyCollectors() = viewLifecycleOwner.repeatOnLifecycle {
        viewModel.profileFlow.collect(this) { profile ->
            userEmail = if(profile!=null)  profile!!.email else ""
        }

        viewModel.cafTempPhotoUris.collect(this) {
            if (newImagesCount==0&& it.second.size==0)return@collect
            it.second.getOrNull(it.first)?.let { uri ->
                b.photo.scaleType = ImageView.ScaleType.FIT_CENTER
                b.photo.setImageURI(uri)
                var bitmap: Bitmap? = null
                val contentResolver: ContentResolver? = ctx?.contentResolver
                try {
                    bitmap = if (Build.VERSION.SDK_INT < 28) {
                        MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    } else {
                        val source: ImageDecoder.Source =
                            ImageDecoder.createSource(contentResolver!!, uri)
                        ImageDecoder.decodeBitmap(source)
                    }
                    val blurred: Bitmap? = blurRenderScript(ctx, bitmap!!, 25)//second parametre is radius//second parametre is radius
                    b.photo.setBackgroundDrawable(BitmapDrawable(blurred))
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                b.photo.tag = 1
            } ?: kotlin.run {
                if (b.photo.tag!=1&&isEdit==0){
                    b.photo.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    b.photo.setImageResource(R.drawable.ic_photo)
                    b.photo.tag = 0
                }
            }
            b.imageNumber.text =
                if (it.second.size > 1) {
                    if (it.first != it.second.size)
                        "${it.first+1}/${it.second.size}"
                    else
                        "${it.first}/${it.second.size}"
                }
                else ""
            b.prevPhoto.isVisible = it.first > 0
            b.nextPhoto.isGone = it.first+1 >= it.second.size
            newImagesCount = it.second.size
        }
        if(isEdit==1&&newImagesCount==0)viewModel.adfTempPhotoUris.collect(this) {
            it.second.getOrNull(it.first)?.let { bmp ->
                b.photo.scaleType = ImageView.ScaleType.FIT_CENTER
                b.photo.setImageBitmap(bmp)
                val blurred: Bitmap? = blurRenderScript(ctx, bmp, 25)//second parametre is radius//second parametre is radius
                b.photo.setBackgroundDrawable(BitmapDrawable(blurred))
                b.photo.tag = 1
            } ?: kotlin.run {
                if (b.photo.tag!=1&&isEdit==0){
                    b.photo.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    b.photo.setImageResource(R.drawable.ic_photo)
                    b.photo.tag = 0
                }
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
                    catsID[item.name] = item.realId.toString()
                }
                val adapter: ArrayAdapter<String> = ArrayAdapter<String>(ctx!!, android.R.layout.simple_spinner_item, data)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                b.spinnerSelectCategory.adapter = adapter
                if(selectedCat.isNotBlank()&&b.spinnerSelectCategory.count>spinnerPosition) b.spinnerSelectCategory.setSelection(spinnerPosition)

                if (data.size>1) {
                    b.spinnerSelectCategory.visibility = View.VISIBLE
                    b.spinnerSelectCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                        override fun onNothingSelected(parent: AdapterView<*>?) {

                        }

                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (position!=0) {
                                selectedCat = b.spinnerSelectCategory.getItemAtPosition(position).toString()
                                spinnerPosition = position
                            }
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
        flagDataLoaded = false
        viewModel.cafTempPhotoUris.value = 0 to mutableListOf()
    }

}