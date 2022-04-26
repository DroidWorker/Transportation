package com.app.transportation.ui.create_order_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.currentTime
import com.app.transportation.core.formatDate
import com.app.transportation.core.millisToString
import com.app.transportation.databinding.FragmentCreatingOrderPpAndKuBinding
import com.app.transportation.databinding.FragmentCreatingOrderRisBinding
import com.app.transportation.ui.MainViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class CreatingOrderRisFragment : Fragment() {

    private var binding: FragmentCreatingOrderRisBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private val categoryId by lazy { arguments?.getInt("id", 1) ?: 1 }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        b.toName.setText(viewModel.profileFlow.value?.name)
        b.toTelNumber.setText(viewModel.profileFlow.value?.telNumber)


        applyListeners()
    }

    private fun applyListeners() {
        b.order.setOnClickListener {
            if (!allFieldsFilled()) {
                viewModel.messageEvent.tryEmit("Заполнены не все поля!")
                return@setOnClickListener
            }

            viewModel.createOrder(
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
                payment = "cash"
            )

            findNavController().navigateUp()
        }
    }

    private fun allFieldsFilled(): Boolean {
        val isCommentPresent = b.comment.text.isNotBlank()
        val isFromDateTime = viewModel.dateTime.isNotBlank()
        val isToCityPresent = b.toCity.text.isNotBlank()
        val isToAreaPresent = b.toArea.text.isNotBlank()
        val isToPlacePresent = b.toPlace.text.isNotBlank()
        val isToNamePresent = b.toName.text.isNotBlank()
        val isToTelNumberPresent = b.toTelNumber.text.isNotBlank()

        return isCommentPresent && isToCityPresent && isToAreaPresent && isToPlacePresent && isFromDateTime &&
                isToNamePresent && isToTelNumberPresent
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