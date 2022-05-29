package com.app.transportation.ui

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
import com.app.transportation.databinding.FragmentPingInfoFeedbackBinding
import com.app.transportation.databinding.FragmentPingInfoRequestsBinding

class pingInfoFeedbackFragment : Fragment() {

    private var binding: FragmentPingInfoFeedbackBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private var popupWindow: PopupWindow? = null

    private val id by lazy { arguments?.getInt("id") ?: 0L }


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
                if (advert.id==id) {
                    advert?.apply {
                        b.orderName3.text = advert.title
                        b.priceView.text = advert.price
                        b.date3.text = advert.date
                        b.time3.text = advert.time
                        b.comment3.text = advert.description
                        b.name3.text = "имя не получено"
                        b.telNumber3.text = "номер не получен"
                    }
                    return@collectWithLifecycle
                }
            }
        }
        viewModel.cachedOrderFeedbackPing.collectWithLifecycle(viewLifecycleOwner) { adverts ->
            adverts.forEach{ advert->
                if (advert.id==id) {
                    advert?.apply {
                        b.orderName3.text = advert.title
                        b.fromLocation3.text = advert.fromCity+", "+advert.fromRegion+", "+advert.fromPlace
                        b.toLocation3.text = advert.toCity+", "+advert.toRegion+", "+advert.toPlace
                        b.date3.text = advert.date
                        b.time3.text = advert.time
                        b.comment3.text = advert.description
                        b.name3.text = "имя не получено"
                        b.telNumber3.text = "номер не получен"
                        if (advert.price=="0") b.priceView.visibility=View.INVISIBLE
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
        b.accept.setOnClickListener{
            b.pingStatus.text = "в работе"
            b.accept.visibility = View.GONE
            b.cancel.visibility = View.GONE
            b.done.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        popupWindow = null
    }

}