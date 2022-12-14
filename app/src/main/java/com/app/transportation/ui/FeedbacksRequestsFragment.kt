package com.app.transportation.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
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
    var myId: String?
        get() = prefs.getString("myId", "0").takeIf { it != "" }
        set(value) = prefs.edit(true) { putString("myId", value ?: "") }

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
        viewModel.cachedOrderPing.tryEmit(emptyList())
        viewModel.cachedAdvertPing.tryEmit(emptyList())
        viewModel.cachedAdvertFeedbackPing.tryEmit(emptyList())
        viewModel.cachedOrderFeedbackPing.tryEmit(emptyList())

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
        adapter.onClick = {
            findNavController().navigate(R.id.pingInfoRequestsFragment, bundleOf("id" to it))
        }
        adapter.onFeedbackClick = {
            findNavController().navigate(R.id.pingInfoFeedbackFragment, bundleOf("id" to  it))
        }
        adapter.onAdvertDeleteClick = {
            viewModel.deleteAdvertPing(it)
        }
        adapter.onOrderDeleteClick = {
            viewModel.deleteOrderPing(it)
        }
        viewModel.getAdvertsPing()
        viewModel.getOrdersPing()
        viewModel.getFeedbackOrdersAdverts()
        b.progressBar2.visibility = View.VISIBLE
        applyList()
    }

    private fun applyList() {
        //var list : List<FeedbackRequest>? = listOf()
                if (feedbacksRequestsActiveTab == "requests") {
                        var arrlist : ArrayList<FeedbackRequest> = ArrayList()
                    if(view!=null)viewModel.cachedOrderPing.collectWithLifecycle(viewLifecycleOwner){
                            it.toList().forEach{ order ->
                                if (order.profile.firstOrNull()?.userId==myId)
                                    arrlist.add(FeedbackRequest((order.id.toString()+order.profile.firstOrNull()?.userId).toLong(), order.id,order.profile[0].status?: "",0, order.description, order.toCity+" "+order.toRegion+" "+order.toPlace, (order.date+" "+order.time).stringToDate("dd.mm.yyyy HH:MM"), 0))
                            }
                            adapter.submitList(arrlist.toList())
                        }
                    if(view!=null)viewModel.cachedAdvertPing.collectWithLifecycle(viewLifecycleOwner){
                        it.toList().forEach{ order ->
                            if (order.profile.firstOrNull()?.userId==myId)
                                arrlist.add(FeedbackRequest((order.id.toString()+order.profile.firstOrNull()?.userId).toLong(), order.id, order.profile[0].status?: "",1, order.description, order.category, (order.date+" "+order.time).stringToDate("dd.mm.yyyy HH:MM"), Integer.parseInt(order.price)))
                        }
                        adapter.submitList(arrlist.toList())
                        arrlist.clear()
                    }
                }
                else{
                    var arrlist : ArrayList<FeedbackRequest> = ArrayList()
                    if(view!=null)viewModel.cachedAdvertFeedbackPing.collectWithLifecycle(viewLifecycleOwner){
                        it.toList().forEach{ order ->
                            if (order.ping.entries.firstOrNull()?.value!="REJECTED"&&order.ping.entries.firstOrNull()?.value!="DONE")
                                arrlist.add(FeedbackRequest((order.id.toString()+order.ping.entries.firstOrNull()?.key).toLong(), order.id, order.ping.entries.firstOrNull()?.value?: "", 2, order.title, order.description, (order.date+" "+order.time).stringToDate("dd.mm.yyyy HH:MM"), Integer.parseInt(order.price)))
                        }
                        adapter.submitList(arrlist.toList())
                    }
                    if(view!=null) viewModel.cachedOrderFeedbackPing.collectWithLifecycle(viewLifecycleOwner){
                        it.toList().forEach{ order ->
                            if (order.ping.entries.firstOrNull()?.value!="REJECTED"&&order.ping.entries.firstOrNull()?.value!="DONE")
                                arrlist.add(FeedbackRequest((order.id.toString()+order.ping.entries.firstOrNull()?.key).toLong(), order.id, order.ping.entries.firstOrNull()?.value?: "",2, order.title, order.description, (order.date+" "+order.time).stringToDate("dd.mm.yyyy HH:MM"), Integer.parseInt(order.price)))
                        }
                        adapter.submitList(arrlist.toList())
                        arrlist.clear()
                    }
        }
        b.progressBar2.visibility = View.GONE
        //adapter.submitList(list)
    }

    private fun applyListeners() {
        prefs.registerOnSharedPreferenceChangeListener(this)

        b.requestsTab.setOnClickListener { feedbacksRequestsActiveTab = "requests" }
        b.feedbacksTab.setOnClickListener { feedbacksRequestsActiveTab = "feedbacks" }
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