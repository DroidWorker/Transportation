package com.app.transportation.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collect
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.repeatOnLifecycle
import com.app.transportation.databinding.FragmentProfileBinding
import com.app.transportation.databinding.FragmentTermsBinding
import com.app.transportation.ui.adapters.ProfileAdapter
import com.google.android.material.snackbar.Snackbar
import com.redmadrobot.inputmask.MaskedTextChangedListener

class TermsFragment : Fragment() {

    private var binding: FragmentTermsBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    var bview : View? = null

    private val title by lazy { arguments?.getString("title") ?: "" }

    var basePhone : String? =  ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTermsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Профиль"
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        when (title){
            "руковдство пользователя"-> viewModel.getStaticData("user_manual")
            "условия использования"-> viewModel.getStaticData("terms_of_use")
            "политика конфиденциальности"-> viewModel.getStaticData("privacy_policy")
        }

        bview = view
        super.onViewCreated(view, savedInstanceState)

        b.imageButton.setColorFilter(R.color.secondary_color)

        applyObservers()

        applyListeners()
    }

    private fun applyObservers() = viewLifecycleOwner.repeatOnLifecycle {
        viewModel.cachedStatic.collectWithLifecycle(viewLifecycleOwner){
            b.termsText.text = it
            b.WVText.loadData(it, "text/html", "en_US");
        }
    }

    private fun applyListeners(){
        b.imageButton.setOnClickListener{
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}