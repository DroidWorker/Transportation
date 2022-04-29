package com.app.transportation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collect
import com.app.transportation.core.repeatOnLifecycle
import com.app.transportation.databinding.FragmentProfileBinding
import com.app.transportation.ui.adapters.ProfileAdapter
import com.redmadrobot.inputmask.MaskedTextChangedListener

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private val profileAdapter by lazy { ProfileAdapter() }

    private var telNumberListener: MaskedTextChangedListener? = null
    private var paymentCardListener: MaskedTextChangedListener? = null

    //private var popupWindow: PopupWindow? = null

    private val id by lazy { arguments?.getLong("id") ?: 0L }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Профиль"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)


        viewModel.updateProfile()

        setupAdapters()

        applyObservers()

        applyListeners()
    }

    private fun setupAdapters() {
        b.profileRV.adapter = profileAdapter

        profileAdapter.apply {
            onEditClick = {realId : Int, categoryId : Int ->
                var destinationRes = R.id.creatingOrderFragment

                if (viewModel.isCustomer.value == false) {
                    destinationRes = R.id.creatingAdvertisementFragment
                } else {
                    when (categoryId) {
                        15,16,17,159,160 -> destinationRes = R.id.creatingOrderFragment
                        18,19,20,157,158 -> destinationRes = R.id.creatingOrderFragment
                        5,6,7,35,36,37,38,39,40,41,42,43,44,45,46 -> {
                            if (categoryId == 5)
                                destinationRes = R.id.creatingOrderFragment
                            else if (categoryId == 6 || categoryId == 7 || categoryId == 37 || categoryId == 38 || categoryId == 39 || categoryId == 40 || categoryId == 41)
                                destinationRes = R.id.creatingOrderAtFragment
                            else if (categoryId == 45 || categoryId == 44)
                                destinationRes = R.id.creatingOrderPFragment
                        }
                        47,48 -> destinationRes = R.id.creatingOrderPFragment
                        9,10,11,126,127,128,129,130,131,132,133,134,135,136,137 -> destinationRes = R.id.creatingOrderPFragment
                        21,22,23 -> destinationRes = R.id.creatingOrderRisFragment
                    }
                }
                findNavController().navigate(destinationRes, bundleOf("id" to realId, "isEdit" to 1))
            }
            onDeleteClick = { isOrder: Boolean, id: Int, position: Int ->
                viewModel.deleteAdvert(isOrder, id, position)
            }
            onAddItemClick = {
                findNavController().navigate(
                    R.id.createOrderCategorySelectorFragment,
                    bundleOf("id" to this)
                )
            }
        }
    }

    private fun applyObservers() = viewLifecycleOwner.repeatOnLifecycle {
        viewModel.profileFlow.collect(this) { profile ->
            profile?.apply {
                b.name.text = name
                b.login.text = login

                b.nameET.setText(name)
                b.telNumberET.setText(telNumber)
                b.emailET.setText(email)
                b.paymentCardET.setText(paymentCard)
                b.cityAreaET.setText(cityArea)
            }
        }
        viewModel.profileAdvertsFlow.collect(this) {
            profileAdapter.submitList(it)
        }
        viewModel.deletedAdvertPosition.collect(this) { position ->
            profileAdapter.currentList.toMutableList().let {
                it.removeAt(position)
                profileAdapter.submitList(it)
            }
        }
    }

    private fun applyListeners() {
        hashSetOf(b.nameET, b.emailET, b.cityAreaET).forEach {
            it.doAfterTextChanged { checkIfProfileChanged() }
        }

        telNumberListener = MaskedTextChangedListener(
            "[0] [000] [000] [00] [00]",
            true, b.telNumberET, null,
            object : MaskedTextChangedListener.ValueListener {
                override fun onTextChanged(
                    maskFilled: Boolean,
                    extractedValue: String,
                    formattedValue: String
                ) {
                    checkIfProfileChanged()
                }
            }
        )
        b.telNumberET.addTextChangedListener(telNumberListener)
        b.telNumberET.onFocusChangeListener = telNumberListener

        paymentCardListener = MaskedTextChangedListener(
            "[0000] [0000] [0000] [0000]",
            true, b.paymentCardET, null,
            object : MaskedTextChangedListener.ValueListener {
                override fun onTextChanged(
                    maskFilled: Boolean,
                    extractedValue: String,
                    formattedValue: String
                ) {
                    checkIfProfileChanged()
                }
            }
        )
        b.paymentCardET.addTextChangedListener(paymentCardListener)
        b.paymentCardET.onFocusChangeListener = paymentCardListener

        b.applyProfileChanges.setOnClickListener {
            it.isVisible = false
            viewModel.editProfile(
                name = b.nameET.text.toString(),
                telNumber = b.telNumberET.text.toString(),
                email = b.emailET.text.toString(),
                paymentCard = b.paymentCardET.text.toString(),
                cityArea = b.cityAreaET.text.toString()
            )
        }
    }

    private fun checkIfProfileChanged() {
        val profile = viewModel.profileFlow.value ?: return

        val nameChanged = b.nameET.text.toString() != profile.name
        val telNumberChanged = b.telNumberET.text.toString() != profile.telNumber
        val emailChanged = b.emailET.text.toString() != profile.email
        val paymentCardChanged = b.paymentCardET.text.toString() != profile.paymentCard
        val cityAreaChanged = b.cityAreaET.text.toString() != profile.cityArea
        /* TODO val avatarChanged = b.avatar.text.toString() != profile.avatar*/
        /* TODOval specializationChanged = b.nameET.text.toString() != profile.specialization*/

        val fieldsChanged = nameChanged || telNumberChanged ||
                emailChanged || paymentCardChanged || cityAreaChanged

        b.applyProfileChanges.isVisible = fieldsChanged
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b.profileRV.adapter = null
        b.telNumberET.removeTextChangedListener(telNumberListener)
        telNumberListener = null
        b.paymentCardET.removeTextChangedListener(paymentCardListener)
        paymentCardListener = null
        binding = null
        //popupWindow = null
    }

}