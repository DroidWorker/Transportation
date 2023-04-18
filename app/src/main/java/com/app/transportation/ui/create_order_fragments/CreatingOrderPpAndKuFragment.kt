package com.app.transportation.ui.create_order_fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.*
import com.app.transportation.databinding.FragmentCreatingOrderPpAndKuBinding
import com.app.transportation.ui.MainViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class CreatingOrderPpAndKuFragment : Fragment() {

    private var binding: FragmentCreatingOrderPpAndKuBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()
    private var ctx : Context? = null

    private val categoryId by lazy { arguments?.getInt("id", 1) ?: 1 }
    private val isEdit by lazy {arguments?.getInt("isEdit", 0) ?: 0}
    private var catsID : HashMap<String, String> = HashMap<String, String>()
    var selectedCat: String = ""
    var selectedCatId = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx=activity
        binding = FragmentCreatingOrderPpAndKuBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Профиль"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }
        viewModel.getMyOrders()

        super.onViewCreated(view, savedInstanceState)

        if (isEdit==1) {
            b.orderCreationTitle.text = "Редактирование заказа"
            b.order.text = "Применить"

            viewModel.cachedOrdersSF.collectWithLifecycle(viewLifecycleOwner) {
                it.forEach { item ->
                    if (item.id==categoryId) {
                        viewModel.dateTime = ""
                        b.fromCity.setText(item.fromCity)
                        b.fromArea.setText(item.fromRegion)
                        b.fromPlace.setText(item.fromPlace)
                        b.fromTime.setText(item.time)
                        b.toCity.setText(item.toCity)
                        b.toArea.setText(item.toRegion)
                        b.toPlace.setText(item.toPlace)
                        b.comment.setText(item.description)
                    }
                }
            }
        }

        b.toName.setText(viewModel.profileFlow.value?.name)
        b.toTelNumber.setText(viewModel.profileFlow.value?.telNumber)
        var strings = viewModel.profileFlow.value?.cityArea?.split("[","/","*","-","+",",","&","$","#","@","!","^","&","\\","|","]")
        b.fromCity.setText(strings?.get(0))
        b.toCity.setText(strings?.get(0))
        if(strings?.size!! >1) {
            b.toArea.setText(strings?.get(strings!!.size-1))
            b.fromArea.setText(strings?.get(strings!!.size-1))
        }

        b.fromTime.text = viewModel.dateTime

        applyListeners()
        applyCollectors()
    }

    private fun applyListeners() {
        b.fromTime.setOnClickListener {
            showDatePicker()
        }
        b.order.setOnClickListener {
            if(isEdit==0) {
                val isFilled = allFieldsFilled()
                if (isFilled==2) {
                    viewModel.messageEvent.tryEmit("Заполнены не все поля!")
                    return@setOnClickListener
                }
                if (isFilled==1) {
                    viewModel.messageEvent.tryEmit("фото не добавлено")
                    return@setOnClickListener
                }

                viewModel.createOrder(
                    ctx = context,
                    category = if (b.spinnerSelectCategory5.isVisible)
                        catsID.getValue(selectedCat!!)
                    else
                        categoryId.toString(),
                    description = b.comment.text.toString(),
                    fromCity = b.fromCity.text.toString(),
                    fromRegion = b.fromArea.text.toString(),
                    fromPlace = b.fromPlace.text.toString(),
                    fromDateTime = viewModel.dateTime,
                    toCity = b.toCity.text.toString(),
                    toRegion = b.toArea.text.toString(),
                    toPlace = b.toPlace.text.toString(),
                    name = b.toName.text.toString(),
                    phone = b.toTelNumber.text.toString(),
                    payment = "cash",
                    photos = emptyList()
                )
            }
            else{
                viewModel.editOrder(
                    orderId =  categoryId.toString(),
                    category = categoryId.toString(),
                    description = if(b.comment.text.isNotBlank()) b.comment.text.toString() else null,
                    fromCity = if(b.fromCity.text.isNotBlank()) b.fromCity.text.toString() else null,
                    fromRegion = if(b.fromArea.text.isNotBlank()) b.fromArea.text.toString() else null,
                    fromPlace = if(b.fromPlace.text.isNotBlank()) b.fromPlace.text.toString() else null,
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
    }

    private fun applyCollectors() = viewLifecycleOwner.repeatOnLifecycle {
        if (1==1)
            viewModel.addAdvertScreenCategoriesFlowFourthLevel(categoryId).collectWithLifecycle(viewLifecycleOwner) {
                var data: ArrayList<String> = ArrayList()
                data.add("выбрать из списка")
                it.forEach { item ->
                    data.add(item.name)
                    catsID[item.name]=item.realId.toString()
                }
                val adapter: ArrayAdapter<String> =
                    ArrayAdapter<String>(ctx!!, android.R.layout.simple_spinner_item, data)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                b.spinnerSelectCategory5.adapter = adapter
                if(b.spinnerSelectCategory5.count>selectedCatId)b.spinnerSelectCategory5.setSelection(selectedCatId)

                if (data.size > 1) {
                    b.spinnerSelectCategory5.visibility = View.VISIBLE
                    b.spinnerSelectCategory5.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(parent: AdapterView<*>?) {

                            }

                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                if (position != 0) {
                                    selectedCatId = position
                                    selectedCat =
                                        b.spinnerSelectCategory5.getItemAtPosition(position)
                                            .toString()
                                }
                            }

                        }
                }
            }
    }

    private fun allFieldsFilled(): Int {
        val isSpinnerSelected = !(b.spinnerSelectCategory5.visibility==View.VISIBLE&&selectedCat.isBlank())
        val isCommentPresent = b.comment.text.isNotBlank()
        val isFromCityPresent = b.fromCity.text.isNotBlank()
        val isFromAreaPresent = b.fromArea.text.isNotBlank()
        val isFromPlacePresent = b.fromPlace.text.isNotBlank()
        val isFromDateTime = viewModel.dateTime.isNotBlank()
        val isToCityPresent = b.toCity.text.isNotBlank()
        val isToAreaPresent = b.toArea.text.isNotBlank()
        val isToPlacePresent = b.toPlace.text.isNotBlank()
        val isToNamePresent = b.toName.text.isNotBlank()
        val isToTelNumberPresent = b.toTelNumber.text.isNotBlank()

        return if(isSpinnerSelected && isCommentPresent && isFromCityPresent && isFromAreaPresent && isFromPlacePresent &&
            isToCityPresent && isToAreaPresent && isToPlacePresent && isFromDateTime &&
            isToNamePresent && isToTelNumberPresent){
            0
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
        viewModel.dateTime = dateInMillis.millisToString("dd/MM/yyyy")
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

    private fun onTimeSelected(hour: Int, minute: Int) {
        val hourAsText = if (hour < 10) "0$hour" else hour
        val minuteAsText = if (minute < 10) "0$minute" else minute

        viewModel.dateTime += " $hourAsText:$minuteAsText"

        val dateTime = requireContext().formatDate(viewModel.dateTime, "dd/MM/yyyy HH:mm", true)
        b.fromTime.text = dateTime
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