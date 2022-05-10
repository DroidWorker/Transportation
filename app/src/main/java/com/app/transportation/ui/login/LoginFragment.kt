package com.app.transportation.ui.login

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.data.login_screen_states.AuthState
import com.app.transportation.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import com.vk.api.sdk.VK
import com.vk.api.sdk.auth.VKScope

class LoginFragment : Fragment() {

    private var binding: FragmentLoginBinding? = null
    private val b get() = binding!!

    private val viewModel: LoginViewModel by navGraphViewModels(R.id.loginFragment)

    var mGoogleSignInClient : GoogleSignInClient? = null

    var RC_SIGN_IN : Int = 123

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var gso : GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyObservers()

        applyListeners()
    }

    private fun applyObservers() {
        viewModel.authState.collectWithLifecycle(viewLifecycleOwner) {
            when (it) {
                is AuthState.Authorizing -> { /* TODO */ }
                is AuthState.Success -> findNavController().navigate(
                    R.id.openMainFragment,
                    bundleOf("needToUpdate" to true)
                )
                is AuthState.IncorrectPassword -> {
                    setPasswordFieldBoxStrokeColor(true)
                    val text = getString(R.string.incorrect_password)
                    makeSnackbar(text)
                    b.signInSecondTitle.text = text
                }
                is AuthState.UserNotFound -> {
                    val text = getString(R.string.user_not_found)
                    makeSnackbar(text)
                    b.signInSecondTitle.text = text
                }
                is AuthState.UnexpectedError -> {
                    val text = getString(R.string.unexpected_error)
                    makeSnackbar(text)
                    b.signInSecondTitle.text = text
                }
            }
        }
    }

    private fun applyListeners() {
        b.notRegisteredSuggestion.setOnClickListener {
            b.signInTitle.text = getString(R.string.sign_in)
            setPasswordFieldBoxStrokeColor(false)
            findNavController().navigate(R.id.registrationFragment)
        }

        b.signIn.setOnClickListener {
            b.signInTitle.text = getString(R.string.sign_in)
            setPasswordFieldBoxStrokeColor(false)
            viewModel.authorize(b.login.text.toString(), b.password.text.toString())
        }

        b.forgotPassword.setOnClickListener {
            b.signInTitle.text = getString(R.string.sign_in)
            setPasswordFieldBoxStrokeColor(false)
            findNavController().navigate(R.id.passwordRestorationFragment)
        }

        b.continueWithoutRegistration.setOnClickListener{
            b.signInTitle.text = getString(R.string.sign_in)
            setPasswordFieldBoxStrokeColor(false)
            findNavController().navigate(R.id.openMainFragment);
        }

        b.signViaVK.setOnClickListener{
            b.signInTitle.text = getString(R.string.sign_in)
            setPasswordFieldBoxStrokeColor(false)
            if (viewModel.VKLogin==null||viewModel.VKPassword==null)
            {
                VK.login(requireActivity(), arrayListOf(VKScope.PHOTOS, VKScope.PHONE, VKScope.EMAIL))

            }
            else
            {
                viewModel.authorize(viewModel.VKLogin!!, viewModel.VKPassword!!)
            }
        }

        b.signViaGmail.setOnClickListener{
            b.signInTitle.text = getString(R.string.sign_in)
            setPasswordFieldBoxStrokeColor(false)
            if (viewModel.GmailLogin==null||viewModel.GmailPassword==null)
            {
                val signInIntent : Intent = mGoogleSignInClient!!.signInIntent
                activity?.startActivityForResult(signInIntent, RC_SIGN_IN)
            }
            else
            {
                viewModel.authorize(viewModel.GmailLogin!!, viewModel.GmailPassword!!)
            }
        }
    }

    private fun makeSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun setPasswordFieldBoxStrokeColor(isError: Boolean) {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_focused),
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf()
        )
        val color = ContextCompat.getColor(requireContext(),
            if (isError) R.color.red else R.color.edittext_field_color)
        val colors = intArrayOf(color, color, color)
        b.passwordLayout.setBoxStrokeColorStateList(ColorStateList(states, colors))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}