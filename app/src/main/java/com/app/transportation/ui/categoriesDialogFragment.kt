package com.app.transportation.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.data.database.entities.SelectorCategory
import com.app.transportation.ui.adapters.CreateOrderCategorySelectorAdapter

class CategoriesDialogFragment : DialogFragment() {
    private val viewModel by activityViewModels<MainViewModel>()
    private val adapter by lazy { CreateOrderCategorySelectorAdapter() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setContentView(R.layout.categories_dialog_layout)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.addAdvertScreenCategoriesFlowAll().collectWithLifecycle(viewLifecycleOwner) {
            adapter.mode = 1
            adapter.submitList(it)
            println("aaaaaaaaaa"+it.size)
        }
        view.findViewById<RecyclerView>(R.id.categoriesView).adapter = adapter
        adapter.submitList(listOf(SelectorCategory(1,1,1,"asd",2)))
        super.onViewCreated(view, savedInstanceState)
    }
}