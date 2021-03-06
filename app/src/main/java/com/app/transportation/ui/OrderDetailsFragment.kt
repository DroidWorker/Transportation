package com.app.transportation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collect
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.databinding.FragmentOrderDetailsBinding

class OrderDetailsFragment : Fragment() {

    private var binding: FragmentOrderDetailsBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private var popupWindow: PopupWindow? = null

    private val id by lazy { arguments?.getLong("id") ?: 0L }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderDetailsBinding.inflate(inflater, container, false)
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
        viewModel.cachedOrder.collectWithLifecycle(viewLifecycleOwner) { advert ->
            viewModel.getProfile(advert?.userId ?: "")
            advert?.apply {
                b.orderName.text = title
                b.fromLocation.text = fromCity+" "+fromRegion+ " "+fromPlace
                b.toLocation.text = toCity+""+toRegion+""+toPlace
                b.addToFavourites2.setColorFilter(0)
                b.addToFavourites2.tag = id
                b.date.text = date
                b.time.text = time
                b.comment.text = title
                b.name.text = "Какое-то имя"
                b.telNumber.text = "Какой-то номер"
            }
        }
        viewModel.cachedProfile.collectWithLifecycle(viewLifecycleOwner){
            b.name.text = it.firstName
            b.telNumber.text = it.phone
        }
    }

    private fun applyListeners() {
        b.execute.setOnClickListener {
            viewModel.addOrderPing(b.addToFavourites2.tag.toString())
        }
        b.addToFavourites2.setOnClickListener{
            b.addToFavourites2.setColorFilter(1)
            viewModel.addOrderFavorite(b.addToFavourites2.tag.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        popupWindow = null
    }

}