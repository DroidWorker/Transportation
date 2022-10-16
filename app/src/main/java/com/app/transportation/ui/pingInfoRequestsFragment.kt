package com.app.transportation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.databinding.FragmentOrderDetailsBinding
import com.app.transportation.databinding.FragmentPingInfoRequestsBinding

class pingInfoRequestsFragment : Fragment() {

    private var binding: FragmentPingInfoRequestsBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private var popupWindow: PopupWindow? = null

    private val id by lazy { arguments?.getInt("id") ?: 0L }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPingInfoRequestsBinding.inflate(inflater, container, false)
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
        viewModel.cachedOrderPing.collectWithLifecycle(viewLifecycleOwner) { adverts ->
            adverts.forEach{ advert->
                if (advert.id.toString()+advert.profile.firstOrNull()?.userId==id.toString()) {
                    viewModel.getProfile(advert.userId)
                    advert?.apply {
                        (activity as? MainActivity)?.apply {
                            b.title.text = advert.category
                        }
                        b.priceView.visibility=View.GONE
                        b.orderName3.text = advert.title
                        b.fromLocation3.text = advert.fromCity+", "+advert.fromRegion+", "+advert.fromPlace
                        b.toLocation3.text = advert.toCity+", "+advert.toRegion+", "+advert.toPlace
                        b.date3.text = advert.date
                        b.time3.text = advert.time
                        b.comment3.text = advert.description
                        /*b.name3.text = advert.profile[0].firstName
                        b.telNumber3.text = advert.profile[0].phone*/
                        viewModel.cachedProfile.collectWithLifecycle(viewLifecycleOwner){
                            b.name3.text = it.firstName//+" "+it.lastName
                            b.telNumber3.text = it.phone
                        }
                        b.pingStatus.text = getStatusName(advert.profile[0].status)
                    }
                    return@collectWithLifecycle
                }
            }
        }
        viewModel.cachedAdvertPing.collectWithLifecycle(viewLifecycleOwner) { adverts ->
            adverts.forEach{ advert->
                if (advert.id.toString()+advert.profile.firstOrNull()?.userId==id.toString()) {
                    viewModel.getProfile(advert.userId)
                    advert?.apply {
                        (activity as? MainActivity)?.apply {
                            b.title.text = advert.category
                        }
                        b.orderName3.text = advert.title
                        b.priceView.text=advert.price
                        b.date3.text = advert.date
                        b.time3.text = advert.time
                        b.comment3.text = advert.description
                        /*b.name3.text = advert.profile[0].firstName+" "+advert.profile[0].lastName
                        b.telNumber3.text = advert.profile[0].phone*/
                        viewModel.cachedProfile.collectWithLifecycle(viewLifecycleOwner){
                            b.name3.text = it.firstName//+" "+it.lastName
                            b.telNumber3.text = it.phone
                        }
                        b.pingStatus.text = getStatusName(advert.profile[0].status)
                    }
                    return@collectWithLifecycle
                }
            }
        }
    }

    private fun applyListeners() {
        b.cancel.setOnClickListener{
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