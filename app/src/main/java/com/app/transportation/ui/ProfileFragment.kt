package com.app.transportation.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.content.edit
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.provider.Settings
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
import com.app.transportation.core.collect
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.decodeSampledBitmapFromResource
import com.app.transportation.core.repeatOnLifecycle
import com.app.transportation.databinding.FragmentProfileBinding
import com.app.transportation.ui.adapters.ProfileAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.redmadrobot.inputmask.MaskedTextChangedListener
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null
    private val b get() = binding!!

    private val viewModel by activityViewModels<MainViewModel>()

    private val profileAdapter by lazy { ProfileAdapter() }

    private var telNumberListener: MaskedTextChangedListener? = null
    private var paymentCardListener: MaskedTextChangedListener? = null

    private var advertCount = 999
    private var advertLimit = 0//лимит на публикацию объявлений

    //private var popupWindow: PopupWindow? = null

    var bview : View? = null

    lateinit var ctx : Context
    var photoFlag = false

    var businessPrice = -1

    private val id by lazy { arguments?.getLong("id") ?: 0L }

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private val userId : String? = prefs.getString("myId", null).takeIf { it != "" }

    private var userEmail = "";

    var basePhone : String? =  ""

    private val obtainPhotoUriLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.data?.let { uri -> viewModel.applyProfilePhotoByUri(uri) }
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
        viewModel.getTarifs()
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
                    val slID = viewModel.getSecondLevelParentID(categoryId)
                    viewModel.getParentID(categoryId).collectWithLifecycle(viewLifecycleOwner) {
                        val tlID = if(slID==it) categoryId else it
                        when (slID) {
                            12 -> destinationRes = R.id.creatingOrderFragment
                            13 -> destinationRes = R.id.creatingOrderFragment
                            4 -> {
                                if (tlID == 5)
                                    destinationRes = R.id.creatingOrderFragment
                                else if (tlID == 6 || tlID == 7 || tlID == 37 || tlID == 38 || tlID == 39 || tlID == 40 || tlID == 41)
                                    destinationRes = R.id.creatingOrderAtFragment
                                else if (tlID == 45 || tlID == 44)
                                    destinationRes = R.id.creatingOrderPFragment
                            }
                            24 -> destinationRes = R.id.creatingOrderPFragment
                            8 -> destinationRes = R.id.creatingOrderPFragment
                            14 -> destinationRes = R.id.creatingOrderRisFragment
                        }
                    }
                }
                findNavController().navigate(destinationRes, bundleOf("id" to realId, "isEdit" to 1))
            }
            onDeleteClick = { isOrder: Boolean, id: Int, position: Int ->
                viewModel.deleteAdvert(isOrder, id, position)
                if(!isOrder){advertCount--}
            }
            onAddItemClick = {
                if (advertCount==999){
                    viewModel.messageEvent.tryEmit("данные не загружены! Пожалуйста, подождите")
                }
                else if (advertCount>advertLimit&& viewModel.isCustomer.value == false){
                    viewModel.messageEvent.tryEmit("превышен лимит объявлений для вашего тарифа!")
                }
                else {
                    findNavController().navigate(
                        R.id.createOrderCategorySelectorFragment,
                        bundleOf("id" to this)
                    )
                }
            }
        }
    }

    private fun applyObservers() = viewLifecycleOwner.repeatOnLifecycle {
        viewModel.profileFlow.collect(this) { profile ->
            profile?.apply {
                println("prrroooof"+this)
                userEmail = email
                b.name.text = name
                b.login.text = login
                if (profile.bussiness.contains("ACTIVE")&&!profile.bussiness.contains("NO")){
                    b.payment.isEnabled = false
                    b.payment2.isEnabled = false
                    if (profile.bussiness.contains("|")&&!profile.bussiness.contains("null")) {
                        val dateTo: String = profile.bussiness.split("|")[1]
                        val cal = Calendar.getInstance()
                        val formatter = SimpleDateFormat("dd.MM.yyyy hh:mm", Locale.ENGLISH)
                        formatter.parse(dateTo)?.let { cal.time = it }
                        cal.add(Calendar.DATE, 30)
                        val resstr = if (profile.bussiness.contains("expert", true)) {
                            b.switch1.isEnabled = true
                            "Тариф ЭКСПЕРТ. Активен до ${cal.get(Calendar.DAY_OF_MONTH)} ${cal.getDisplayName(Calendar.MONTH, Calendar.LONG_FORMAT, Locale("ru"))} ${cal.get(Calendar.YEAR)}"
                        }
                        else "Тариф УНИВЕРСАЛ. Активен до ${cal.get(Calendar.DAY_OF_MONTH)} ${cal.getDisplayName(Calendar.MONTH, Calendar.LONG_FORMAT, Locale("ru"))} ${cal.get(Calendar.YEAR)}"
                        b.expTV.text = resstr
                    }
                    else{
                        b.expTV.visibility = View.GONE
                    }
                }
                else{
                    b.payment.text = "Aктивировать бизнес аккаунт"
                }

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
        viewModel.ctx = context
        viewModel.profileAdvertsFlow.collect(this) {
            advertCount = 0
            it.forEach{item ->
                if(item.viewType == 2&&!item.title.contains("(заказ)")){
                    advertCount++
                    }
            }
            profileAdapter.submitList(it)
        }
        viewModel.deletedAdvertPosition.collect(this) { position ->
            profileAdapter.currentList.toMutableList().let {
                it.removeAt(position)
                profileAdapter.submitList(it)
            }
        }
        viewModel.cachedTarif.collect(this){
            try {
                if (it.isNotEmpty()&&!b.payment.text.contains("тариф")) {
                    businessPrice = it.first().amount.toInt()
                    prefs.edit{putString("businessPrice", businessPrice.toString())}
                    b.payment.text = "активировать бизнес аккаунт - ${businessPrice/100} p"
                }
            }
            catch (ex : java.lang.Exception){
                viewModel.messageEvent.tryEmit("Error: Invalid PriceList")
            }
        }
    }

    private fun applyListeners() {
        viewModel.profilePhotoUri.collectWithLifecycle(viewLifecycleOwner) {
            it.second.getOrNull(it.first)?.let { uri ->
                viewModel.cafApplyPhotoByUri(uri)
                b.avatar.scaleType = ImageView.ScaleType.CENTER_CROP
                b.avatar.setImageURI(uri)
                b.avatar.tag = if(photoFlag) 2 else 0
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
                if(b.avatar.tag==2) {
                    viewModel.editProfile(
                        name = b.nameET.text.toString(),
                        telNumber = b.telNumberET.text.toString(),
                        email = b.emailET.text.toString(),
                        paymentCard = "",
                        cityArea = b.cityAreaET.text.toString(),
                        avatar = photos
                    )
                }
                else{
                    viewModel.editProfile(
                        name = b.nameET.text.toString(),
                        telNumber = b.telNumberET.text.toString(),
                        email = b.emailET.text.toString(),
                        paymentCard = "",
                        cityArea = b.cityAreaET.text.toString()
                    )
                }
                b.avatar.tag = 0
            }
        }

        b.avatar.setOnClickListener{
            //val position = viewModel.cafTempPhotoUris.value.first
            //val currentValue = viewModel.cafTempPhotoUris.value.second.getOrNull(position)
            val position = viewModel.cafTempPhotoUris.value.first
            val currentValue = viewModel.cafTempPhotoUris.value.second.getOrNull(position)
            currentValue?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удалить фото?")
                    .setPositiveButton("Да") { _, _ ->
                        viewModel.cafRemoveCurrentPhoto()
                        importViaSystemFE()
                    }
                    .setNegativeButton("Нет") { _, _ -> }
                    .show()
            } ?: importViaSystemFE()
        }

        b.payment.setOnClickListener{
            onPayClick(0)
        }
        b.payment2.setOnClickListener{
            onPayClick(1)
        }
    }

    fun onPayClick(businesstype: Int){
        if (businessPrice==-1){
            viewModel.messageEvent.tryEmit("Пожалуйста, дождитесь загрузки цен!")
            return
        }
        if (userEmail.isEmpty() || userId?.toInt()==0) {
            viewModel.messageEvent.tryEmit("не указан email, либо профиль еще не загружен")
            return
        }
        val intent = Intent(activity, PaymentActivity::class.java)
        intent.putExtra("summ", if (businesstype==0) 30000 else if (businesstype==1) 60000 else 30000)
        intent.putExtra("mode", if (businesstype==0) 1 else if (businesstype==1) 3 else 1)
        intent.putExtra("id", userId?.toInt())
        intent.putExtra("email", userEmail)
        startActivity(intent)
    }

    private fun checkIfProfileChanged() {
        val profile = viewModel.profileFlow.value ?: return

        val nameChanged = b.nameET.text.toString() != profile.name
        val telNumberChanged = b.telNumberET.text.toString() != basePhone
        val emailChanged = b.emailET.text.toString() != profile.email
        val paymentCardChanged = false
        val cityAreaChanged = b.cityAreaET.text.toString() != profile.cityArea
        val avatarChanged = b.avatar.tag == 2
        /* TODO val avatarChanged = b.avatar.text.toString() != profile.avatar*/
        /* TODOval specializationChanged = b.nameET.text.toString() != profile.specialization*/
        var fieldsChanged = nameChanged || telNumberChanged ||
                emailChanged || paymentCardChanged || cityAreaChanged || avatarChanged
        b.applyProfileChanges.isVisible = fieldsChanged
    }

    private fun importViaSystemFE() {
        photoFlag = true
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
        paymentCardListener = null
        binding = null
        //popupWindow = null
    }

}