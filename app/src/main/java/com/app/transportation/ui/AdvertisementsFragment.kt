package com.app.transportation.ui

import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.repeatOnLifecycle
import com.app.transportation.data.database.entities.Advert
import com.app.transportation.data.database.entities.SelectorCategory
import com.app.transportation.data.database.entities.ServiceType
import com.app.transportation.databinding.FragmentAdvertisementsBinding
import com.app.transportation.databinding.PopupMenuServicesFilterBinding
import com.app.transportation.ui.adapters.AdvertisementsAdapter
import com.app.transportation.ui.adapters.CreateOrderCategorySelectorAdapter
import com.app.transportation.ui.adapters.PopupMenuServicesFilterAdapter
import kotlin.math.roundToInt

class AdvertisementsFragment : Fragment() {

    private var binding: FragmentAdvertisementsBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private val adapter by lazy { AdvertisementsAdapter() }
    private val adapter1 by lazy { CreateOrderCategorySelectorAdapter() }

    private var popupWindow: PopupWindow? = null

    private val categoryId by lazy { arguments?.getInt("categoryId") ?: 0 }
    private val type by lazy { arguments?.getInt("type") ?: 0 }//0-customer 1-seller 2- 3-search result
    private val orderItemShouldOpenId by lazy { arguments?.getInt("clickedItemId") ?: -1 }
    private val searchText by lazy { arguments?.getString("searchText") ?: "" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdvertisementsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (type==0||type==3) {
            b.city.visibility = View.GONE
            b.filter.visibility = View.GONE
        }

        (activity as? MainActivity)?.apply {
            /*b.title.text =
                viewModel.advertCategoriesFlow.value.find { it.id == categoryId }?.name
                    ?: "Экран"*/
            if (viewModel.isCustomer.value==true)
                b.title.text = "я заказчик"
            else
                b.title.text = "я исполнитель"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)
        if (type==1){
            viewModel.cachedOrdersSF.tryEmit(emptyList())
            viewModel.getAllCategoryOrders(categoryId)
        }
        else if (type==2) {
            viewModel.cachedAdvertsSF.tryEmit(emptyList())
            viewModel.getAllCategoryAdverts(categoryId)
        }
        else if(type==0)
            viewModel.getCategoryAdverts(categoryId)
        else
            viewModel.getSearchResult(searchText)

        applyRVAdapter()

        applyObservers()

        applyListeners()
    }

    private fun applyRVAdapter() {
        if (type == 1||type==2) {//if seller
            b.servicesRV.adapter = adapter

            adapter.onClick = {
                if (viewModel.isCustomer.value == true) {
                    viewModel.cachedAdvert.tryEmit(this)
                    findNavController().navigate(R.id.advertDetailsFragment)//not use here
                } else {
                    viewModel.cachedOrder.tryEmit(this)
                    findNavController().navigate(R.id.orderDetailsFragment)
                }
            }
            if (adapter.itemCount == 0&&type==1)
                    b.beSeller.visibility = View.VISIBLE
            else if(type == 2)
                b.createOrder.visibility = View.VISIBLE

        } else if(type==3) {
            b.servicesRV.adapter = adapter

            adapter.onClick = {
                /*if (viewModel.isCustomer.value == true) {
                    viewModel.cachedAdvert.tryEmit(this)
                    findNavController().navigate(R.id.advertDetailsFragment)//not use here
                } else {
                    viewModel.cachedOrder.tryEmit(this)
                    findNavController().navigate(R.id.orderDetailsFragment)
                }*/
            }
        }
        else {//if customer
            b.servicesRV.adapter = adapter1
            adapter1.mode=4

            adapter1.onClick = { i: Int, i1: Int ->
                var ParentId: Int = i
                var ItemId: Int = i1

                if (adapter1.parentIds.contains(i1)) {
                    findNavController().navigate(R.id.createOrderCategorySelectorFragment, bundleOf("id" to ItemId, "mode" to 2))
                }
                else{
                    findNavController().navigate(R.id.advertisementsFragment, bundleOf("categoryId" to ItemId, "type" to 2))
                }
            }
            b.createOrder.visibility = View.VISIBLE
        }

    }

    private fun applyObservers() = viewLifecycleOwner.repeatOnLifecycle {
        if(type==1){
            viewModel.cachedOrdersSF.collectWithLifecycle(viewLifecycleOwner) {
                adapter.submitList(it)
                if (it.isEmpty()) {
                    var filler: ArrayList<Advert> = ArrayList()
                    filler.add(
                        Advert(
                            0,
                            3,
                            0,
                            "",
                            0,
                            0,
                            "Заказов не найдено!",
                            "",
                            "",
                            "",
                            null,
                            "",
                            emptyList(),
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            ""
                        )
                    )
                    adapter.submitList(filler.toList())
                }
            }
        }
        else if (type==2) {
            viewModel.cachedAdvertsSF.collectWithLifecycle(viewLifecycleOwner) {
                adapter.submitList(it)
                if (it.isEmpty()) {
                    var filler: ArrayList<Advert> = ArrayList()
                    filler.add(
                        Advert(
                            0,
                            3,
                            0,
                            "",
                            0,
                            0,
                            "Объявлений не найдено!",
                            "Оформите заказ чтобы исполнитель нашел вас первым!",
                            "",
                            "",
                            null,
                            "",
                            emptyList(),
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            ""
                        )
                    )
                    adapter.submitList(filler.toList())
                }
                if (adapter.itemCount!=0&&type==2)
                    b.createOrder.visibility = View.GONE
                /*if (adapter1.getItemCount() == 0) {
                    b.beSeller.visibility = View.VISIBLE
                } else {
                    b.beSeller.visibility = View.INVISIBLE
                }*/
            }
        }else if(type==3){
            viewModel.cachedSearchResult.collectWithLifecycle(viewLifecycleOwner) {
                adapter.submitList(it)
                if (adapter.itemCount==0) {
                    var filler: ArrayList<Advert> = ArrayList()
                    filler.add(
                        Advert(
                            0,
                            3,
                            0,
                            "",
                            0,
                            0,
                            "Ничего не найдено!",
                            "",
                            "",
                            "",
                            null,
                            "",
                            emptyList(),
                            "",
                            "",
                            "",
                            "",
                            "",
                            "",
                            ""
                        )
                    )
                    adapter.submitList(filler.toList())
                }
            }
        } else {
            viewModel.addAdvertScreenCategoriesFlowAll().collectWithLifecycle(viewLifecycleOwner) {
                var list : ArrayList<SelectorCategory> = ArrayList()
                if (orderItemShouldOpenId!=0){
                    it.forEach{ item ->
                        if (item.realId==orderItemShouldOpenId||item.parentId==orderItemShouldOpenId){
                            list.add(item)
                        }
                    }
                    adapter1.submitList(list.toList())
                }
                else
                    adapter1.submitList(it)
            }
            b.createOrder.visibility = View.VISIBLE
        }
    }


    private fun applyListeners() {
        b.beSeller.setOnClickListener{
            findNavController().navigate(R.id.createOrderCategorySelectorFragment, bundleOf("id" to 0, "mode" to 3))
        }
        b.createOrder.setOnClickListener{
            findNavController().navigate(R.id.createOrderCategorySelectorFragment, bundleOf("id" to 0, "mode" to 3))
        }
        b.city.setOnClickListener { findNavController().navigate(R.id.addCityDF) }
        b.filter.setOnClickListener {
            popupWindow = PopupWindow(requireContext()).apply {
                val menuB = PopupMenuServicesFilterBinding.inflate(layoutInflater)
                contentView = menuB.root
                menuB.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height = View.MeasureSpec.makeMeasureSpec(
                    menuB.root.measuredHeight,
                    View.MeasureSpec.UNSPECIFIED
                )

                setBackgroundDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.menu_background)
                )

                val list = viewModel.advertCategoriesFlow.value
                    .filter { it.parentId == categoryId }
                    .map { ServiceType(it.id, it.name) }
                menuB.filterRV.adapter = PopupMenuServicesFilterAdapter().apply {
                    submitList(list)
                    onClick = { id ->
                        adapter.submitList(
                            viewModel.cachedAdvertsSF.value.filter { it.subcategoryId == id }.toMutableList()
                        )
                    }
                }

                menuB.specifyWeight.setOnClickListener {
                    //TODO
                }

                menuB.from.setOnClickListener {
                    //TODO
                }

                menuB.to.setOnClickListener {
                    //TODO
                }

                isOutsideTouchable = true
                setTouchInterceptor { v, event ->
                    v.performClick()
                    return@setTouchInterceptor if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        dismiss()
                        true
                    } else
                        false
                }

                showAsDropDown(b.city, 1, (0.1 * b.city.height).roundToInt(), Gravity.CENTER)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b.servicesRV.adapter = null
        binding = null
        popupWindow = null
    }

}