package com.app.transportation.ui.create_order_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.databinding.FragmentCreatingOrderGpBinding

class CreatingOrderGpFragment : Fragment() {

    private var binding: FragmentCreatingOrderGpBinding? = null
    private val b get() = binding!!

    private val id by lazy { arguments?.getLong("id") ?: 0L }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        /*b.addOfficeItems.setOnClickListener {
            findNavController().navigate(R.id.serviceListFragment)
        }
        b.addStorageItems.setOnClickListener {

        }
        b.addGarageItems.setOnClickListener {

        }*/
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