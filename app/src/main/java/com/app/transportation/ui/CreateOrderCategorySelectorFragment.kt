package com.app.transportation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.databinding.FragmentCreateOrderCategorySelectorBinding
import com.app.transportation.ui.adapters.AdvertisementsAdapter
import com.app.transportation.ui.adapters.CreateOrderCategorySelectorAdapter
import com.google.android.material.snackbar.Snackbar

class CreateOrderCategorySelectorFragment : Fragment() {


    private var binding: FragmentCreateOrderCategorySelectorBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private val adapter by lazy { CreateOrderCategorySelectorAdapter() }
    private val adapterAdvs by lazy { AdvertisementsAdapter() }

    private val categoryId by lazy { arguments?.getInt("id", 1) ?: 1 }
    private val mode by lazy { arguments?.getInt("mode", 0) ?: 0 }//0-standart 1-select executor 2-open 4level category

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreateOrderCategorySelectorBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            if(mode==3) {
                b.title.text = "Выберите категорию"
                Snackbar.make(view, "Выберите категорию", Snackbar.LENGTH_LONG).show()
            }
            else
                b.title.text = "Профиль"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)

        applyAdapters()

        applyFlowCollectors()

        applyListeners()
    }

    private fun applyAdapters() {
        b.rv.adapter = adapter

        if (mode==0) {//cabunet call
            adapter.mode=0
            adapter.onClick = { i: Int, i1: Int ->
                var ParentId: Int = i
                var ItemId: Int = i1
                var destinationRes = R.id.creatingOrderFragment

                if (viewModel.isCustomer.value == false) {
                    destinationRes = R.id.creatingAdvertisementFragment
                } else {
                    when (ParentId) {
                        12 -> destinationRes = R.id.creatingOrderFragment
                        13 -> destinationRes = R.id.creatingOrderFragment
                        4 -> {
                            if (ItemId == 5)
                                destinationRes = R.id.creatingOrderFragment
                            else if (ItemId == 6 || ItemId == 7 || ItemId == 37 || ItemId == 38 || ItemId == 39 || ItemId == 40 || ItemId == 41)
                                destinationRes = R.id.creatingOrderAtFragment
                            else if (ItemId == 45 || ItemId == 44)
                                destinationRes = R.id.creatingOrderPFragment
                        }
                        24 -> destinationRes = R.id.creatingOrderPFragment
                        8 -> destinationRes = R.id.creatingOrderPFragment
                        14 -> destinationRes = R.id.creatingOrderRisFragment
                    }
                }
                findNavController().navigate(destinationRes, bundleOf("id" to i1))
            }
        }
        else if(mode==1){//main screen call
            adapter.mode=1
            adapter.onClick = { i: Int, i1: Int ->
                var ParentId: Int = i
                var ItemId: Int = i1

                b.rv.adapter = adapterAdvs
                adapterAdvs.onClick = {
                    if (viewModel.isCustomer.value == true) {
                        viewModel.cachedAdvert.tryEmit(this)
                        findNavController().navigate(R.id.advertDetailsFragment)
                    } else {
                        viewModel.cachedOrder.tryEmit(this)
                        findNavController().navigate(R.id.orderDetailsFragment)
                    }
                }
            }
        }
        else if(mode==2){
            adapter.openID = categoryId
            adapter.mode=2
            adapter.onClick = { i: Int, i1: Int ->
                var ParentId: Int = i
                var ItemId: Int = i1
                findNavController().navigate(R.id.advertisementsFragment, bundleOf("categoryId" to ItemId, "type" to 2))
            }
        }
        else{
            adapter.mode=1
            adapter.onClick = { i: Int, i1: Int ->
                var ParentId: Int = i
                var ItemId: Int = i1

                var destinationRes = R.id.creatingOrderFragment

                if (viewModel.isCustomer.value == false) {
                    destinationRes = R.id.creatingAdvertisementFragment
                } else {
                    when (ParentId) {
                        12 -> destinationRes = R.id.creatingOrderFragment
                        13 -> destinationRes = R.id.creatingOrderFragment
                        4 -> {
                            if (ItemId == 5)
                                destinationRes = R.id.creatingOrderFragment
                            else if (ItemId == 6 || ItemId == 7 || ItemId == 37 || ItemId == 38 || ItemId == 39 || ItemId == 40 || ItemId == 41)
                                destinationRes = R.id.creatingOrderAtFragment
                            else if (ItemId == 45 || ItemId == 44)
                                destinationRes = R.id.creatingOrderPFragment
                        }
                        24 -> destinationRes = R.id.creatingOrderPFragment
                        8 -> destinationRes = R.id.creatingOrderPFragment
                        14 -> destinationRes = R.id.creatingOrderRisFragment
                    }
                }
                findNavController().navigate(destinationRes, bundleOf("id" to i1))
            }
        }
    }

    private fun applyFlowCollectors() {
        if (mode==0){
            viewModel.addAdvertScreenCategoriesFlow(categoryId).collectWithLifecycle(viewLifecycleOwner) {
                adapter.submitList(it)
            }
        }
        else
            viewModel.addAdvertScreenCategoriesFlowAll().collectWithLifecycle(viewLifecycleOwner) {
                adapter.submitList(it)
            }
    }

    private fun applyListeners() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        b.rv.adapter = null
        binding = null
        //popupWindow = null
    }

}