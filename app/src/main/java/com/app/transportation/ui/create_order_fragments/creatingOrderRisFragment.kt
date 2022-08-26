package com.app.transportation.ui.create_order_fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.*
import com.app.transportation.databinding.FragmentCreatingOrderRisBinding
import com.app.transportation.ui.MainViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.io.ByteArrayOutputStream
import java.io.File

class CreatingOrderRisFragment : Fragment() {

    private var binding: FragmentCreatingOrderRisBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private var ctx : Context? = null

    private val categoryId by lazy { arguments?.getInt("id", 1) ?: 1 }
    private val isEdit by lazy {arguments?.getInt("isEdit", 0) ?: 0}

    private val obtainPhotoUriLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { uri -> viewModel.cafApplyPhotoByUri(uri) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = activity
        binding = FragmentCreatingOrderRisBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Профиль"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)

        if (isEdit==1) {
            b.orderCreationTitle.text = "Редактирование заказа"
            b.order.text = "Применить"

            viewModel.userOrdersAdvertsFlow.collectWithLifecycle(this) {
                it.forEach { item ->
                    if (item.id==categoryId) {
                        b.toCity.setText(item.toCity)
                        b.toArea.setText(item.toRegion)
                        b.toPlace.setText(item.toPlace)
                        b.comment.setText(item.description)
                        b.selectDateTime.setText(item.time)
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

        b.toName.setText(viewModel.profileFlow.value?.name)
        b.toTelNumber.setText(viewModel.profileFlow.value?.telNumber)
        var strings = viewModel.profileFlow.value?.cityArea?.split("[","/","*","-","+",",","&","$","#","@","!","^","&","\\","|","]")
        b.toCity.setText(strings?.get(0))
        if(strings?.size!! >1) {
            b.toArea.setText(strings?.get(strings?.size-1))
        }

        applyListeners()
    }

    private fun applyListeners() {
        b.selectDateTime.setOnClickListener{
            showDatePicker()
        }
        b.order.setOnClickListener {
            if (isEdit==0) {
                val isFilled = allFieldsFilled()
                if (isFilled==2) {
                    viewModel.messageEvent.tryEmit("Заполнены не все поля!")
                    return@setOnClickListener
                }
                if (isFilled==1) {
                    viewModel.messageEvent.tryEmit("фото не добавлено")
                    return@setOnClickListener
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

                viewModel.createOrder(
                    ctx=context,
                    category = categoryId.toString(),
                    description = b.comment.text.toString(),
                    fromCity = "",
                    fromRegion = "",
                    fromPlace = "",
                    fromDateTime = viewModel.dateTime,
                    toCity = b.toCity.text.toString(),
                    toRegion = b.toArea.text.toString(),
                    toPlace = b.toPlace.text.toString(),
                    name = b.toName.text.toString(),
                    phone = b.toTelNumber.text.toString(),
                    payment = "cash",
                    photos = photos
                )
            }
            else{
                viewModel.editOrder(
                    orderId = categoryId.toString(),
                    category = categoryId.toString(),
                    description = if(b.comment.text.isNotBlank()) b.comment.text.toString() else null,
                    fromCity = null,
                    fromRegion = null,
                    fromPlace = null,
                    fromDateTime = if(viewModel.dateTime != "") viewModel.dateTime else null,
                    toCity = if(b.toCity.text.isNotBlank()) b.toCity.text.toString() else null,
                    toRegion = if(b.toArea.text.isNotBlank()) b.toArea.text.toString() else null,
                    toPlace = if(b.toPlace.text.isNotBlank()) b.toPlace.text.toString() else null,
                    name = if(b.toName.text.isNotBlank()) b.toName.text.toString() else null,
                    phone = if (b.toTelNumber.text.isNotBlank()) b.toTelNumber.text.toString() else null,
                    payment = "cash")
            }

            findNavController().navigateUp()
        }
        b.photo.setOnClickListener{
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
    }

    private fun applyCollectors() = viewLifecycleOwner.repeatOnLifecycle {
        if (1==1)
            viewModel.addAdvertScreenCategoriesFlowFourthLevel(categoryId).collectWithLifecycle(viewLifecycleOwner) {
                var data : ArrayList<String> = ArrayList()
                data.add("выбрать из списка")
                it.forEach{item ->
                    data.add(item.name)
                }
                val adapter: ArrayAdapter<String> = ArrayAdapter<String>(ctx!!, android.R.layout.simple_spinner_item, data)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                b.spinnerSelectCategory6.adapter = adapter

                if (data.size>1)
                    b.spinnerSelectCategory6.visibility = View.VISIBLE
            }
    }

    private fun allFieldsFilled(): Int {
        val isCommentPresent = b.comment.text.isNotBlank()
        val isFromDateTime = viewModel.dateTime.isNotBlank()
        val isToCityPresent = b.toCity.text.isNotBlank()
        val isToAreaPresent = b.toArea.text.isNotBlank()
        val isToPlacePresent = b.toPlace.text.isNotBlank()
        val isToNamePresent = b.toName.text.isNotBlank()
        val isToTelNumberPresent = b.toTelNumber.text.isNotBlank()
        val isPhotoSet =  b.photo.tag==1

        return if(isCommentPresent && isToCityPresent && isToAreaPresent && isToPlacePresent && isFromDateTime &&
            isToNamePresent && isToTelNumberPresent && isPhotoSet){
            0
        } else if (!isPhotoSet){
            1
        } else 2
    }

    private fun showDatePicker() {
        MaterialDatePicker.Builder.datePicker()
            .setSelection(System.currentTimeMillis())
            .build()
            .apply { addOnPositiveButtonClickListener { millisDate -> onDateSelected(millisDate) } }
            .show(parentFragmentManager, MaterialDatePicker::class.java.canonicalName)
    }

    private fun onDateSelected(dateInMillis: Long) {
        viewModel.dateTime = dateInMillis.millisToString("yyyy/MM/dd")
        showTimePicker()
    }

    private fun showTimePicker() {
        val currentTime = currentTime
        MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(currentTime.first)
            .setMinute(currentTime.second)
            .build()
            .apply { addOnPositiveButtonClickListener { onTimeSelected(hour, minute) } }
            .show(parentFragmentManager, MaterialTimePicker::class.java.canonicalName)
    }

    private fun importViaSystemFE() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
        runCatching { obtainPhotoUriLauncher.launch(intent) }
    }

    private fun onTimeSelected(hour: Int, minute: Int) {
        val hourAsText = if (hour < 10) "0$hour" else hour
        val minuteAsText = if (minute < 10) "0$minute" else minute

        viewModel.dateTime += " $hourAsText:$minuteAsText"

        val dateTime = requireContext().formatDate(viewModel.dateTime, "yyyy/MM/dd HH:mm", true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //b.officeRV.adapter = null
        //b.storageRV.adapter = null
        //b.garageRV.adapter = null
        binding = null
        //popupWindow = null
    }

}