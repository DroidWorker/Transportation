package com.app.transportation.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.hideKeyboard
import com.app.transportation.data.login_screen_states.SendSmsCodeState
import com.app.transportation.databinding.FragmentSmsCodeInputBinding
import com.google.android.material.snackbar.Snackbar

class SmsCodeInputFragment : Fragment() {

    private var binding: FragmentSmsCodeInputBinding? = null
    private val b get() = binding!!

    private val viewModel: PasswordRestorationVM by navGraphViewModels(R.id.passwordRestorationFragment)

    private val code by lazy { arguments?.getString("code")?.split(", ") }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSmsCodeInputBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.b?.upButton?.isVisible = true

        super.onViewCreated(view, savedInstanceState)

        obtainCode()

        applyObservers()

        applyListeners()
    }

    private fun obtainCode() {
        val code = code ?: return

        b.digit1.setText(code[0])
        b.digit2.setText(code[1])
        b.digit3.setText(code[2])
        b.digit4.setText(code[3])
    }

    private fun applyObservers() {
        viewModel.sendSmsCodeStateSF.collectWithLifecycle(viewLifecycleOwner) {
            when (it) {
                is SendSmsCodeState.Success -> findNavController().navigate(R.id.newPasswordFragment)
                else -> makeSnackbar(it.message)
            }
        }
    }

    private fun applyListeners() {
        b.save.setOnClickListener {
            val list = listOf(
                b.digit1.text.toString(), b.digit2.text.toString(),
                b.digit3.text.toString(), b.digit4.text.toString()
            )
            viewModel.sendSmsCode(list)
        }

        b.digit1.doAfterTextChanged {
            if (it?.isNotBlank() == true)
                b.digit2.requestFocus()
        }
        b.digit2.doAfterTextChanged {
            if (it?.isNotBlank() == true)
                b.digit3.requestFocus()
        }
        b.digit3.doAfterTextChanged {
            if (it?.isNotBlank() == true)
                b.digit4.requestFocus()
        }
        b.digit4.doAfterTextChanged {
            if (it?.isNotBlank() == true) {
                b.digit4.clearFocus()
                requireContext().hideKeyboard(b.digit4)
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