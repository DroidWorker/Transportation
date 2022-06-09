package com.app.transportation.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.PaymentActivity
import com.app.transportation.R
import com.app.transportation.core.*
import com.app.transportation.databinding.FragmentProfileBinding
import com.app.transportation.ui.adapters.ProfileAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.redmadrobot.inputmask.MaskedTextChangedListener
import java.io.ByteArrayOutputStream
import java.io.File

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private val profileAdapter by lazy { ProfileAdapter() }

    private var telNumberListener: MaskedTextChangedListener? = null
    private var paymentCardListener: MaskedTextChangedListener? = null

    //private var popupWindow: PopupWindow? = null

    var bview : View? = null

    lateinit var ctx : Context

    private val id by lazy { arguments?.getLong("id") ?: 0L }

    var basePhone : String? =  ""

    private val obtainPhotoUriLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { uri -> viewModel.cafApplyPhotoByUri(uri) }
        }

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
        viewModel.updateMainFragmentData()
        viewModel.updateProfile()
        ctx = requireActivity()
        bview = view
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()

        applyObservers()

        applyListeners()
    }

    private fun setupAdapters() {
        b.profileRV.adapter = profileAdapter

        profileAdapter.apply {
            onEditClick = {title : String, realId : Int, categoryId : Int ->
                var destinationRes = R.id.creatingOrderFragment

                if (!title.contains("(заказ)")) {
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
                if (telNumber.contains("0000000")) {
                    val numb: String = telNumber.replace("0000000", "*******")
                    basePhone=numb
                    b.telNumberET.setText(numb)
                }
                else {
                    b.telNumberET.setText(telNumber)
                    basePhone = telNumber
                }
                b.emailET.setText(email)
                b.paymentCardET.setText(paymentCard)
                b.cityAreaET.setText(cityArea)
                if(avatar.length>1) {
                    avatar.let { photoitem ->
                        try {
                            val base64String = photoitem.replace("data:image/jpg;base64,", "")
                            val byteArray = Base64.decode(base64String, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                            b.avatar.scaleType = ImageView.ScaleType.CENTER_CROP
                            b.avatar.setImageBitmap(bitmap)
                            b.avatar.setBackgroundDrawable(BitmapDrawable(bitmap))
                            b.avatar.tag=0
                        } catch (ex: Exception) {
                            println("Error: " + ex.message.toString())
                        }
                    }
                }
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
        viewModel.cafTempPhotoUris.collectWithLifecycle(viewLifecycleOwner) {
            it.second.getOrNull(it.first)?.let { uri ->
                b.avatar.scaleType = ImageView.ScaleType.CENTER_CROP
                b.avatar.setImageURI(uri)
                b.avatar.tag = 1
            }
            checkIfProfileChanged()
        }

        hashSetOf(b.nameET, b.emailET, b.cityAreaET).forEach {
            it.doAfterTextChanged { checkIfProfileChanged() }
        }

        b.telNumberET.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (b.telNumberET.text.toString() != basePhone && s?.matches("^[+]?[0-9]{0,13}\$".toRegex()) == false){
                    b.telNumberET.setText(b.telNumberET.text?.substring(0, b.telNumberET.length()-1))
                }
                checkIfProfileChanged()
            }
        })
        //b.telNumberET.onFocusChangeListener = telNumberListener

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
            val photos = mutableListOf<String>()
            if (!b.cityAreaET.text.toString().matches("^[а-яА-Я]+[,]{1}[а-яА-Я ]+\$".toRegex())){
                Snackbar.make(bview!!, "данные не соответствуют формату 'город, область'", Snackbar.LENGTH_LONG).show()
            }
            else if(b.telNumberET.text.toString().length<9)
                Snackbar.make(bview!!, "номер телефона слишком короткий", Snackbar.LENGTH_LONG).show()
            else {
                it.isVisible = false
                viewModel.cafTempPhotoUris.value.second.firstOrNull()?.let {
                    val contentResolver = requireContext().applicationContext.contentResolver
                    contentResolver.openInputStream(it)?.use {
                        val base64String = Base64.encodeToString(it.readBytes(), Base64.DEFAULT)
                        File(requireContext().cacheDir.path + "/ttt.txt").writeText(base64String)
                    }
                }
                viewModel.cafTempPhotoUris.value.second.forEach { uri ->
                    val contentResolver = requireContext().applicationContext.contentResolver
                    contentResolver.openInputStream(uri)?.use {
                        var resultBitmap : Bitmap? = decodeSampledBitmapFromResource(it.readBytes(), 200, 200)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        resultBitmap?.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                        val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
                        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
                        photos.add("'data:image/jpg;base64,$base64String'")
                    }
                }
                viewModel.editProfile(
                    name = b.nameET.text.toString(),
                    telNumber = b.telNumberET.text.toString(),
                    email = b.emailET.text.toString(),
                    paymentCard = b.paymentCardET.text.toString(),
                    cityArea = b.cityAreaET.text.toString(),
                    avatar = photos
                )
            }
        }

        b.avatar.setOnClickListener{
            val position = viewModel.cafTempPhotoUris.value.first
            val currentValue = viewModel.cafTempPhotoUris.value.second.getOrNull(position)
            currentValue?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удалить фото?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.cafRemoveCurrentPhoto()
                    }
                    .setNegativeButton("Нет") { _, _ -> }
                    .show()
            } ?: importViaSystemFE()
        }

        b.payment.setOnClickListener{
            startActivity(Intent(activity, PaymentActivity::class.java))
        }
    }

    private fun checkIfProfileChanged() {
        val profile = viewModel.profileFlow.value ?: return

        val nameChanged = b.nameET.text.toString() != profile.name
        val telNumberChanged = b.telNumberET.text.toString() != basePhone
        val emailChanged = b.emailET.text.toString() != profile.email
        val paymentCardChanged = b.paymentCardET.text.toString() != profile.paymentCard
        val cityAreaChanged = b.cityAreaET.text.toString() != profile.cityArea
        val avatarChanged = b.avatar.tag == 1
        /* TODO val avatarChanged = b.avatar.text.toString() != profile.avatar*/
        /* TODOval specializationChanged = b.nameET.text.toString() != profile.specialization*/
        var fieldsChanged = nameChanged || telNumberChanged ||
                emailChanged || paymentCardChanged || cityAreaChanged || avatarChanged
        b.applyProfileChanges.isVisible = fieldsChanged
    }

    private fun importViaSystemFE() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
        runCatching { obtainPhotoUriLauncher.launch(intent) }
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