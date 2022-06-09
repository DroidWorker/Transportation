package com.app.transportation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.repeatOnLifecycle
import com.app.transportation.data.database.entities.Advert
import com.app.transportation.databinding.FragmentFavoritesBinding
import com.app.transportation.ui.adapters.FavouritesAdapter

class favouritesFragment : Fragment() {

    private var binding: FragmentFavoritesBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private val adapter by lazy { FavouritesAdapter() }

    var bview : View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Избранное"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }
        viewModel.cachedAdvertFavorite.tryEmit(emptyList())
        viewModel.cachedOrderFavorite.tryEmit(emptyList())
        viewModel.getAdvertsFavorite()
        viewModel.getOrdersFavorite()

        bview = view
        b.progressBar.visibility=View.VISIBLE
        super.onViewCreated(view, savedInstanceState)


        viewModel.updateProfile()

        setupAdapters()

        applyObservers()

        applyListeners()
    }

    private fun setupAdapters() {
        b.favList.adapter = adapter
    }

    private fun applyObservers() = viewLifecycleOwner.repeatOnLifecycle {
        var list : ArrayList<Advert> = ArrayList()
        viewModel.cachedAdvertFavorite.collectWithLifecycle(viewLifecycleOwner){
            if (it.isEmpty())
            else if (it[0].viewType==2&&adapter.itemCount>0)
                b.progressBar.visibility=View.GONE
            else {
                list.addAll(it)
                b.progressBar.visibility = View.GONE
                adapter.submitList(list.toList())
            }
        }
        viewModel.cachedOrderFavorite.collectWithLifecycle(viewLifecycleOwner){
            if (it.isEmpty())
            else if (it[0].viewType==2&&adapter.itemCount>0){
                b.progressBar.visibility=View.GONE
            }
            else {
                list.addAll(it)
                b.progressBar.visibility = View.GONE
                adapter.submitList(list.toList())
            }
        }
    }

    private fun applyListeners() {
        adapter.onClick = {
            if (this.description.isNotBlank())
                viewModel.deleteAdvertFavorite(this.id)
            else
                viewModel.deleteOrderFavorite(this.id)
            viewModel.cachedAdvertFavorite.tryEmit(emptyList())
            viewModel.cachedOrderFavorite.tryEmit(emptyList())
            viewModel.getAdvertsFavorite()
            viewModel.getOrdersFavorite()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}