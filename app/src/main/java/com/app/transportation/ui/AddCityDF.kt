package com.app.transportation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.fragment.app.DialogFragment
import com.app.transportation.databinding.FragmentAddCityDialogBinding

class AddCityDF : DialogFragment() {

    private var binding: FragmentAddCityDialogBinding? = null
    private val b get() = binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddCityDialogBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireDialog().window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}