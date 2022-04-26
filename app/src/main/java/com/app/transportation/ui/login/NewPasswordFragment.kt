package com.app.transportation.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.data.login_screen_states.SendNewPasswordState
import com.app.transportation.databinding.FragmentNewPasswordBinding
import com.google.android.material.snackbar.Snackbar

class NewPasswordFragment : Fragment() {

    private var binding: FragmentNewPasswordBinding? = null
    private val b get() = binding!!

    private val viewModel: PasswordRestorationVM by navGraphViewModels(R.id.passwordRestorationFragment)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewPasswordBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.b?.upButton?.isVisible = true

        super.onViewCreated(view, savedInstanceState)

        applyObservers()

        applyListeners()
    }

    private fun applyObservers() {
        viewModel.sendNewPasswordStateSF.collectWithLifecycle(viewLifecycleOwner) {
            when (it) {
                is SendNewPasswordState.Success -> findNavController().navigate(R.id.openLoginFragment)
                else -> makeSnackbar(it.message)
            }
        }
    }

    private fun applyListeners() {
        b.save.setOnClickListener {
            val login = viewModel.tempLogin ?: return@setOnClickListener
            val newPassword = b.newPassword.text.toString()
            val newPasswordAgain = b.newPasswordAgain.text.toString()

            with(viewModel) {
                if (newPassword == newPasswordAgain && newPassword.passwordIsOk())
                    sendNewPassword(login, newPassword)
                else
                    makeSnackbar(getString(R.string.password_is_short))
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