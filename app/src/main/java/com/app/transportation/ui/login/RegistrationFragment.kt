package com.app.transportation.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.data.login_screen_states.RegistrationState
import com.app.transportation.databinding.FragmentRegistrationBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistrationFragment : Fragment() {

    private var binding: FragmentRegistrationBinding? = null
    private val b get() = binding!!

    private val viewModel: LoginViewModel by navGraphViewModels(R.id.loginFragment)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyObservers()

        applyListeners()
    }

    private fun applyObservers() {
        viewModel.registrationState.collectWithLifecycle(viewLifecycleOwner) {
            if (it !is RegistrationState.Registering){
                makeSnackbar(it.message)
                if (it is RegistrationState.Success) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        delay(2000)
                        withContext(Dispatchers.Main) {
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }

    private fun applyListeners() {
        b.alreadyRegisteredSuggestion.setOnClickListener {
            findNavController().navigateUp()
        }

        b.register.setOnClickListener {
            val login = b.login.text.toString()
            val password = b.password.text.toString()
            val name = b.name.text.toString()
            val fields = hashSetOf(login, password, name)
            if (fields.none { it.isBlank() })
                viewModel.register(login, password, name)
            else
                makeSnackbar(getString(R.string.not_all_fields_filled))
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