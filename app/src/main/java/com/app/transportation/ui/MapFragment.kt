package com.app.transportation.ui

import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.dpToPx
import com.app.transportation.data.RVDecoration
import com.app.transportation.data.database.entities.Advertisement
import com.app.transportation.data.database.entities.Job
import com.app.transportation.databinding.FragmentMainBinding
import com.app.transportation.databinding.FragmentMapBinding
import com.app.transportation.ui.adapters.JobsAdapter
import com.app.transportation.ui.adapters.MainAdvertisementsAdapter
import com.app.transportation.ui.adapters.ServiceTypeAdapter
import com.google.android.material.snackbar.Snackbar
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.filterNotNull
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class MapFragment : Fragment() {

    private var binding: FragmentMapBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private var isCustomer: Boolean?
        get() = viewModel.isCustomer.value
        set(value) {
            viewModel.isCustomer.value = value
            prefs.edit { putBoolean("isCustomer", value ?: false) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Экран"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)
        b.search.clearFocus()

        if (arguments?.getBoolean("needToUpdate") == true) {
            arguments?.putBoolean("needToUpdate", false)
            viewModel.updateMainFragmentData()
        }

        applyObservers()

        applyListeners()
    }

    private fun applyObservers() {

    }

    private fun applyListeners() {

        b.mapFilter.setOnClickListener {

        }

        b.myLocation.setOnClickListener {

        }

        b.search2.setOnEditorActionListener(TextView.OnEditorActionListener() {v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                if (b.search2.text.length<2){
                    Snackbar.make(b.search, "минимальная длина запроса 2 символа!", Snackbar.LENGTH_LONG).show()
                    return@OnEditorActionListener true
                }
                //viewModel.getSearchResult("ер")
                /*findNavController().navigate(R.id.advertisementsFragment,
                    bundleOf("categoryId" to lastCheckedCategoryId, "type" to 3, "searchText" to b.search.text.toString()))
                b.search.setText("")*/
                true
            } else {
                false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}