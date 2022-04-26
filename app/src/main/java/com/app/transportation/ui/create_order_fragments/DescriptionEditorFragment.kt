package com.app.transportation.ui.create_order_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.databinding.FragmentDescriptionEditorBinding

class DescriptionEditorFragment : Fragment() {

    private var binding: FragmentDescriptionEditorBinding? = null
    private val b get() = binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDescriptionEditorBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Профиль"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)

        b.description.setText(arguments?.getString("text"))

        applyListeners()
    }

    private fun applyListeners() {
        b.save.setOnClickListener {
            setFragmentResult("Description", bundleOf("description" to b.description.text.toString()))
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}