package com.app.transportation.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.data.login_screen_states.SendEmailState
import com.app.transportation.databinding.FragmentPasswordRestorationBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PasswordRestorationFragment : Fragment() {

    private var binding: FragmentPasswordRestorationBinding? = null
    private val b get() = binding!!

    private val viewModel: PasswordRestorationVM by navGraphViewModels(R.id.passwordRestorationFragment)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPasswordRestorationBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.b?.upButton?.isVisible = true

        super.onViewCreated(view, savedInstanceState)

        applyObservers()

        applyListeners()
    }

    private fun applyObservers() {
        viewModel.sendEmailStateSF.collectWithLifecycle(viewLifecycleOwner) {
            when (it) {
                is SendEmailState.Success ->
                    findNavController().navigate(R.id.smsCodeInputFragment,
                        bundleOf("code" to it.code.joinToString()))
                else -> makeSnackbar(it.message)
            }
        }
    }

    private fun applyListeners() {
        b.save.setOnClickListener {
            if (b.email.text?.isNotBlank() == true) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.requestSmsCode(b.email.text.toString())
                }
            }
        }
    }

    private fun makeSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}