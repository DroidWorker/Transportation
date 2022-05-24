package com.app.transportation.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.stringToDate
import com.app.transportation.data.database.entities.FeedbackRequest
import com.app.transportation.databinding.FragmentFeedbacksRequestsBinding
import com.app.transportation.ui.adapters.FeedbacksRequestsAdapter
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class FeedbacksRequestsFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var binding: FragmentFeedbacksRequestsBinding? = null
    private val b get() = binding!!

    private val adapter by lazy { FeedbacksRequestsAdapter() }

    private val viewModel by activityViewModels<MainViewModel>()

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private var feedbacksRequestsActiveTab
        get() = prefs.getString("feedbacksRequestsActiveTab", null) ?: "requests"
        set(value) = prefs.edit { putString("feedbacksRequestsActiveTab", value) }
    //private var popupWindow: PopupWindow? = null

    private val id by lazy { arguments?.getLong("id") ?: 0L }

    private val selectedColor
        get() = ContextCompat.getColor(requireContext(), R.color.primary_color)
    private val unselectedColor
        get() = ContextCompat.getColor(requireContext(), R.color.button_inactive_color)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedbacksRequestsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = ""
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)

        applyTabColors()

        applyAdapters()

        applyListeners()
    }

    private fun applyTabColors() {
        if (feedbacksRequestsActiveTab == "requests") {
            (activity as? MainActivity)?.b?.title?.text = "Мои заказы"
            b.requestsTab.setCardBackgroundColor(selectedColor)
            b.feedbacksTab.setCardBackgroundColor(unselectedColor)
        } else {
            (activity as? MainActivity)?.b?.title?.text = "Мои отклики"
            b.requestsTab.setCardBackgroundColor(unselectedColor)
            b.feedbacksTab.setCardBackgroundColor(selectedColor)
        }
    }

    private fun applyAdapters() {
        b.feedbacksRequestsRV.adapter = adapter
        viewModel.getAdvertsPing()
        viewModel.getOrdersPing()
        b.progressBar2.visibility = View.VISIBLE
        applyList()
    }

    private fun applyList() {
        var list : List<FeedbackRequest>? = listOf()
                if (feedbacksRequestsActiveTab == "requests") {
                        var arrlist : ArrayList<FeedbackRequest> = ArrayList()
                        viewModel.cachedOrderPing.collectWithLifecycle(viewLifecycleOwner){
                            it.toList().forEach{ order ->
                                arrlist.add(FeedbackRequest((order.id).toLong(), 0, order.title, order.toCity+" "+order.toRegion+" "+order.toPlace, (order.date+" "+order.time).stringToDate("dd.mm.yyyy HH:MM"), 0))
                            }
                            list =arrlist.toList()
                            adapter.submitList(list)
                        }
                }
                else{
                    var arrlist : ArrayList<FeedbackRequest> = ArrayList()
                    viewModel.cachedAdvertPing.collectWithLifecycle(viewLifecycleOwner){
                        it.toList().forEach{ order ->
                            arrlist.add(FeedbackRequest((order.id).toLong(), 1, order.title, order.category, (order.date+" "+order.time).stringToDate("dd.mm.yyyy HH:MM"), Integer.parseInt(order.price)))
                        }
                        list =arrlist.toList()
                        adapter.submitList(list)
                    }
        }
        b.progressBar2.visibility = View.GONE
        adapter.submitList(list)
    }

    private fun applyListeners() {
        prefs.registerOnSharedPreferenceChangeListener(this)

        b.requestsTab.setOnClickListener { feedbacksRequestsActiveTab = "requests" }
        b.feedbacksTab.setOnClickListener { feedbacksRequestsActiveTab = "feedbacks" }

        /*b.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> feedbacksRequestsActiveTab = "requests"
                    1 -> feedbacksRequestsActiveTab = "feedbacks"
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })*/
        /*b.addOfficeItems.setOnClickListener {
            findNavController().navigate(R.id.serviceListFragment)
        }*/
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "feedbacksRequestsActiveTab") {
            applyList()
            applyTabColors()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b.feedbacksRequestsRV.adapter = null
        b.tabLayout.clearOnTabSelectedListeners()
        binding = null
        //popupWindow = null
    }

}