package com.app.transportation.ui

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.databinding.FragmentPingInfoFeedbackBinding
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

class pingInfoFeedbackFragment : Fragment() {

    private var binding: FragmentPingInfoFeedbackBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    var myId: String?
        get() = prefs.getString("myId", "0").takeIf { it != "" }
        set(value) = prefs.edit(true) { putString("myId", value ?: "") }

    var pingOwnerId : String = ""
    var resetStatusOrderId : String = ""

    private var popupWindow: PopupWindow? = null

    private val id by lazy { arguments?.getInt("id") ?: 0L }

    private var isAdvert = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPingInfoFeedbackBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = when (id) {
                0L -> "Пассажирские перевозки"
                1L -> "Грузоперевозки"
                else -> "Пассажирские перевозки"
            }
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)

        applyObservers()

        applyListeners()
    }

    private fun applyObservers() {
        viewModel.cachedAdvertFeedbackPing.collectWithLifecycle(viewLifecycleOwner) { adverts ->
            adverts.forEach{ advert->
                if (advert.id.toString()+advert.ping.entries.firstOrNull()?.key==id.toString()) {
                    isAdvert = true
                    resetStatusOrderId = advert.id.toString()
                    pingOwnerId = advert.ping.entries.firstOrNull()?.key ?: ""
                    advert.ping.entries.firstOrNull()?.let { viewModel.getProfile(it.key) }
                    viewModel.getAllCategoryAdverts(advert.categoryId)
                    advert.apply {
                        (activity as? MainActivity)?.apply {
                            b.title.text = advert.category
                        }
                        b.orderName3.text = advert.title
                        b.priceView.text = advert.price
                        b.date3.text = advert.date
                        b.time3.text = advert.time
                        b.comment3.text = advert.description
                        b.name3.text = ". . ."//advert.profile.firstOrNull()?.firstName+" "+advert.profile.firstOrNull()?.lastName
                        b.telNumber3.text = ". . ."//advert.profile.firstOrNull()?.phone
                        viewModel.cachedProfile.collectWithLifecycle(viewLifecycleOwner){
                            b.name3.text = it.firstName//+" "+it.lastName
                            b.telNumber3.text = it.phone
                        }
                        b.pingStatus.text = getStatusName(advert.ping.entries.firstOrNull()?.value)
                        if (b.pingStatus.text=="в работе"){
                            b.accept.visibility = View.GONE
                            b.done.visibility = View.VISIBLE
                        }
                        else if(b.pingStatus.text=="выполнена"||b.pingStatus.text=="отменена"){
                            b.accept.visibility = View.GONE
                            b.done.visibility = View.GONE
                            b.delete.visibility=View.VISIBLE
                        }
                    }
                    return@collectWithLifecycle
                }
            }
        }
        viewModel.cachedOrderFeedbackPing.collectWithLifecycle(viewLifecycleOwner) { adverts ->
            adverts.forEach{ advert->
                if (advert.id.toString()+advert.ping.entries.firstOrNull()?.key==id.toString()) {
                    resetStatusOrderId = advert.id.toString()
                    pingOwnerId = advert.ping.entries.firstOrNull()?.key ?: ""
                    advert.ping.entries.firstOrNull()?.let { viewModel.getProfile(it.key) }
                    viewModel.getAllCategoryAdverts(advert.categoryId)
                    advert.apply {
                        (activity as? MainActivity)?.apply {
                            b.title.text = advert.category
                        }
                        b.orderName3.text = advert.title
                        if (advert.fromCity==""&&advert.fromPlace==""&&advert.fromRegion=="") {
                            b.fromLocation3.visibility = View.GONE
                            b.fromLocationIcon3.visibility=View.GONE
                            b.locationArrow3.visibility=View.GONE
                        } else
                            b.fromLocation3.text = advert.fromCity+", "+advert.fromRegion+", "+advert.fromPlace
                        b.toLocation3.text = advert.toCity+", "+advert.toRegion+", "+advert.toPlace
                        b.date3.text = advert.date
                        b.time3.text = advert.time
                        b.comment3.text = advert.description
                        b.name3.text = ". . ."//advert.profile.firstOrNull()?.firstName+" "+advert.profile.firstOrNull()?.lastName
                        b.telNumber3.text = ". . ."//advert.profile.firstOrNull()?.phone
                        if (advert.price=="0") b.priceView.visibility=View.INVISIBLE
                        viewModel.cachedProfile.collectWithLifecycle(viewLifecycleOwner){
                            b.name3.text = it.firstName//+" "+it.lastName
                            b.telNumber3.text = it.phone
                        }
                        b.pingStatus.text = getStatusName(advert.ping.entries.firstOrNull()?.value)
                        if (b.pingStatus.text=="в работе"){
                            b.accept.visibility = View.GONE
                            b.done.visibility = View.VISIBLE
                        }
                        viewModel.getUserAdvertByCategory(advert.categoryId.toString(), pingOwnerId)
                    }
                    return@collectWithLifecycle
                }
            }
        }
        viewModel.cachedAdvertsSF.collectWithLifecycle(viewLifecycleOwner){
            it.forEach{advert->
                if (advert.userId==pingOwnerId){
                    b.comment3.text = b.comment3.text.toString() + ":\n" + advert.description
                    return@forEach
                }
            }
        }
    }

    private fun applyListeners() {
        b.comment3.setOnClickListener{
            findNavController().navigate(R.id.advertDetailsFragment)
        }
        b.cancel.setOnClickListener{
            if (isAdvert)
                viewModel.setPingStatus( pingOwnerId, null, resetStatusOrderId, "REJECTED")
            else
                viewModel.setPingStatus( pingOwnerId, resetStatusOrderId, null, "REJECTED")
            findNavController().navigateUp()
        }
        b.accept.setOnClickListener{
            b.pingStatus.text = "в работе"
            if (isAdvert)
                viewModel.setPingStatus( pingOwnerId, null, resetStatusOrderId, "PROGRESS")
            else
                viewModel.setPingStatus( pingOwnerId, resetStatusOrderId, null, "PROGRESS")
            b.accept.visibility = View.GONE
            b.done.visibility = View.VISIBLE
        }
        b.done.setOnClickListener{
            if (isAdvert)
                viewModel.setPingStatus( pingOwnerId, null, resetStatusOrderId, "DONE")
            else
                viewModel.deleteAdvert(!isAdvert, resetStatusOrderId.toInt(), -1)
                //viewModel.setPingStatus( pingOwnerId, resetStatusOrderId, null, "DONE")
            findNavController().navigateUp()
        }
        b.telNumber3.setOnClickListener{
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${b.telNumber3.text}"))
            //if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent)
            //}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        popupWindow = null
    }

    private fun getStatusName(statusCode : String?): String{
        if (statusCode!=null){
            when(statusCode){
                "SEND"->return "отправлена"
                "REJECTED"->return "отменена"
                "PROGRESS"->return "в работе"
                "DONE"->return "выполнена"
            }
        }
        return "не определен"
    }
}