package com.app.transportation.ui.create_order_fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.repeatOnLifecycle
import com.app.transportation.databinding.FragmentCreatingOrderGpBinding
import com.app.transportation.ui.MainViewModel

class CreatingOrderGpFragment : Fragment() {

    private var binding: FragmentCreatingOrderGpBinding? = null
    private val b get() = binding!!

    private val id by lazy { arguments?.getLong("id") ?: 0L }
    private val isEdit by lazy {arguments?.getInt("isEdit", 0) ?: 0}

    private val viewModel by activityViewModels<MainViewModel>()

    private var ctx : Context? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ctx = activity
        binding = FragmentCreatingOrderGpBinding.inflate(inflater, container, false)
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
                    if (item.id==id.toInt()) {
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

        applyInitialData()

        applyAdapters()

        applyListeners()
    }

    private fun applyInitialData() {
        //b.name.text = "Николай"
        //b.telNumber.text = "+7 (495) 510-55-55"
    }

    private fun applyAdapters() {
        /*b.officeRV.adapter = officeAdapter
        b.storageRV.adapter = storageAdapter
        b.garageRV.adapter = garageAdapter

        officeAdapter.apply {
            onEditClick = {}
            onDeleteClick = {}
            submitList(
                listOf(Specialization(0, "Плиточник"), Specialization(1, "Репетитор"))
            )
        }
        storageAdapter.apply {
            onEditClick = {}
            onDeleteClick = {}
            submitList(
                listOf(Specialization(0, "Плиточник"), Specialization(1, "Репетитор"))
            )
        }
        garageAdapter.apply {
            onEditClick = {}
            onDeleteClick = {}
            submitList(
                listOf(Specialization(0, "Плиточник"), Specialization(1, "Репетитор"))
            )
        }*/
    }

    private fun applyListeners() {
        b.order.setOnClickListener{
            if (isEdit==0){

            }
            else{
                viewModel.editOrder(
                    orderId = id.toString(),
                    category = id.toString(),
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
        }
        /*b.addOfficeItems.setOnClickListener {
            findNavController().navigate(R.id.serviceListFragment)
        }
        b.addStorageItems.setOnClickListener {

        }
        b.addGarageItems.setOnClickListener {

        }*/
    }

    private fun applyCollectors() = viewLifecycleOwner.repeatOnLifecycle {
        if (1==1)
            viewModel.addAdvertScreenCategoriesFlowFourthLevel(id.toInt()).collectWithLifecycle(viewLifecycleOwner) {
                var data : ArrayList<String> = ArrayList()
                data.add("выбрать из списка")
                it.forEach{item ->
                    data.add(item.name)
                }
                val adapter: ArrayAdapter<String> = ArrayAdapter<String>(ctx!!, android.R.layout.simple_spinner_item, data)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                b.spinnerSelectCategory3.adapter = adapter

                if (data.size>1)
                    b.spinnerSelectCategory3.visibility = View.VISIBLE
            }
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