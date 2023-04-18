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
import com.app.transportation.ui.adapters.JobsAdapter
import com.app.transportation.ui.adapters.MainAdvertisementsAdapter
import com.app.transportation.ui.adapters.ServiceTypeAdapter
import com.google.android.material.snackbar.Snackbar
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.filterNotNull
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class MainFragment : Fragment() {

    private var binding: FragmentMainBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private val jobsAdapter by lazy { JobsAdapter() }
    private val serviceTypeAdapter by lazy { ServiceTypeAdapter() }
    private val mainAdvertisementsAdapter by lazy { MainAdvertisementsAdapter() }

    private var isCustomer: Boolean?
        get() = viewModel.isCustomer.value
        set(value) {
            viewModel.isCustomer.value = value
            prefs.edit { putBoolean("isCustomer", value ?: false) }
        }

    private var lastCheckedCategoryId
        get() = prefs.getInt(
            "lastCheckedServiceTypeId",
            viewModel.secondLevelCategoriesFlow.value.firstOrNull()?.id ?: 4
        )
        set(value) = prefs.edit { putInt("lastCheckedServiceTypeId", value) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
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
        viewModel.getAdvertCatsList()
        viewModel.getBusinessLast()
        println("steeeep1")
        viewModel.geetNews()
        viewModel.getOrderCountNews()

        applyVPAdapter()

        applyRVAdapter()

        applyAdvertisementsAdapter()

        applyObservers()

        applyListeners()
    }

    private fun applyVPAdapter() {
        val pageMarginPx = resources.getDimensionPixelOffset(R.dimen.pageMargin)
        val offsetPx = resources.getDimensionPixelOffset(R.dimen.offset)
        b.viewPager.addItemDecoration(RVDecoration(25, false))
        when(prefs.getString("appTheme", "system")){
            "light"->jobsAdapter.mode=false
            "dark"->jobsAdapter.mode=true
        }
        b.viewPager.adapter = jobsAdapter
        jobsAdapter.ctx = requireContext()
        jobsAdapter.apply {
            onClick = {
                viewModel.cachedAdvert.tryEmit(null)
                viewModel.getAdvertById(this)
                findNavController().navigate(R.id.advertDetailsFragment)
            }
        }
    }

    private fun applyRVAdapter() {
        b.serviceTypesRV.addItemDecoration(RVDecoration(25, true))
        b.serviceTypesRV.adapter = serviceTypeAdapter
        serviceTypeAdapter.lastCheckedCategoryId = lastCheckedCategoryId
        viewModel.cachedOrderNews.collectWithLifecycle(viewLifecycleOwner){
            serviceTypeAdapter.newsCount = it.toMutableMap()
        }
        serviceTypeAdapter.onClick = {
            viewModel.resetOrderCountNews(this.toString())
            lastCheckedCategoryId = this
            serviceTypeAdapter.lastCheckedCategoryId = this
            serviceTypeAdapter.notifyDataSetChanged()
            findNavController().navigate(R.id.advertisementsFragment,
                bundleOf("categoryId" to lastCheckedCategoryId, "type" to 0, "clickedItemId" to this))
            viewModel.cachedOrderNews.tryEmit(serviceTypeAdapter.newsCount)
        }
    }

    private fun applyAdvertisementsAdapter() {

        b.advertisementsVP.addItemDecoration(RVDecoration(25, false))
        when(prefs.getString("appTheme", "system")){
            "light"->mainAdvertisementsAdapter.mode=false
            "dark"->mainAdvertisementsAdapter.mode=true
        }
        b.advertisementsVP.adapter = mainAdvertisementsAdapter
    }

    private fun applyObservers() {
        viewModel.secondLevelCategoriesFlow.collectWithLifecycle(viewLifecycleOwner) {
            serviceTypeAdapter.submitList(it)
        }

        viewModel.cachedBussinessLast.collectWithLifecycle(viewLifecycleOwner){
            jobsAdapter.submitList(it)
        }

        viewModel.cachedNews.collectWithLifecycle(viewLifecycleOwner){
            var l: ArrayList<Advertisement> = ArrayList()
            var index = 0
            try {
                it.forEach {
                    val b64 = it.backImage.replace("data:image/jpeg;base64,", "")
                    val byteArray = Base64.decode(b64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    l.add(Advertisement(index.toLong(), it.title, it.subtitle, bitmap))
                    index++
                }
            }
            catch (ex : Exception){viewModel.messageEvent.tryEmit("can't load news")}
            mainAdvertisementsAdapter.submitList(
                l.toList()
            )
        }

        viewModel.isCustomer.filterNotNull().collectWithLifecycle(viewLifecycleOwner) {
            viewModel.clearCachedAdverts()
            when (it) {
                true -> {
                    b.imExecutor.apply {
                        if (paddingTop != 0) {
                            updatePaddingRelative(0, 0, 0, 0)
                            setBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.button_inactive_color)
                            )
                        }
                    }
                    val padding = requireContext().dpToPx(18)
                    b.imCustomer.apply {
                        updatePaddingRelative(top = padding, bottom = padding)
                        setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.primary_color)
                        )
                    }
                }
                false -> {
                    b.imCustomer.apply {
                        if (paddingTop != 0) {
                            updatePaddingRelative(0, 0, 0, 0)
                            setBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.button_inactive_color)
                            )
                        }
                    }
                    val padding = requireContext().dpToPx(18)
                    b.imExecutor.apply {
                        updatePaddingRelative(top = padding, bottom = padding)
                        setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.primary_color)
                        )
                    }
                }
            }
            prefs.edit { putBoolean("isCustomer", it) }
        }
    }

    private fun applyListeners() {

        b.imCustomer.setOnClickListener {
            isCustomer = true
            findNavController().navigate(R.id.advertisementsFragment,
                bundleOf("categoryId" to lastCheckedCategoryId, "type" to 0))
        }

        b.imExecutor.setOnClickListener {
            isCustomer = false
            findNavController().navigate(R.id.advertisementsFragment,
                bundleOf("categoryId" to -1, "type" to 1))
        }

        b.openMap.setOnClickListener{
            findNavController().navigate(R.id.mapFragment)
        }

        b.search.setOnEditorActionListener(TextView.OnEditorActionListener() {v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                if (b.search.text.length<2){
                    Snackbar.make(b.search, "минимальная длина запроса 2 символа!", Snackbar.LENGTH_LONG).show()
                    return@OnEditorActionListener true
                }
                //viewModel.getSearchResult("ер")
                findNavController().navigate(R.id.advertisementsFragment,
                    bundleOf("categoryId" to lastCheckedCategoryId, "type" to 3, "searchText" to b.search.text.toString()))
                b.search.setText("")
                true
            } else {
                false
            }
        });
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b.viewPager.adapter = null
        b.serviceTypesRV.adapter = null
        b.advertisementsVP.adapter = null
        binding = null
    }

}