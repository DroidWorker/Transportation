package com.app.transportation

import android.Manifest
import android.app.*
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Html
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.app.transportation.core.AlarmReceiver
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.isLightTheme
import com.app.transportation.data.upButtonSF
import com.app.transportation.databinding.ActivityMainBinding
import com.app.transportation.databinding.PopupMenuMainBinding
import com.app.transportation.service.NotificationService
import com.app.transportation.ui.MainViewModel
import com.app.transportation.ui.login.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.vk.api.sdk.VK
import com.vk.api.sdk.VKApiCallback
import com.vk.api.sdk.auth.VKAccessToken
import com.vk.api.sdk.auth.VKAuthCallback
import com.vk.api.sdk.exceptions.VKAuthException
import com.vk.api.sdk.requests.VKRequest
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val navController
            by lazy { (supportFragmentManager.findFragmentById(R.id.navHostFragment)
                    as NavHostFragment).navController }

    val b by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val viewModel by viewModels<MainViewModel>()
    private val vm by viewModels<LoginViewModel>()

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private val authToken: String?
        get() = prefs.getString("authToken", null).takeIf { it != "" }

    private val feedbacksRequestsActiveTab
        get() = prefs.getString("feedbacksRequestsActiveTab", null) ?: "requests"

    private var popupWindow: PopupWindow? = null

    val RC_SIGN_IN : Int = 103

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(this,
                arrayOf(Manifest.permission.CALL_PHONE),
                1)
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1)
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                1)
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY)
            != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY),
                1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName: String = this.packageName
            val pm: PowerManager = this.getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)
            ) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:" + "YOUR_PACKAGE_NAME")
                this.startActivity(intent)
            }
        }

        MapKitFactory.setApiKey("af3124eb-b373-4c99-9249-d501e4c4a539")

        applyTheme()

        setContentView(b.root)

        b.bottomNav.setupWithNavController(navController)

        checkAuth()

        applyBottomNavTabTitle()

        applyObservers()

        applyListeners()

        createNotificationChannel()

        /*cancelAlarm()
        setAlarm()*/
        if (!isServiceRunning(NotificationService::class.java)) {
            startService(Intent(this, NotificationService::class.java))
        }
    }



    private fun cancelAlarm(){
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)

        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        alarmManager.cancel(pendingIntent)
    }


    private fun setAlarm(){
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)

        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 3000, pendingIntent
        )
    }

    fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager : ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "trnspChannel"
            val description = "chaneel for repeater"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("trnsp", name, importance)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.description = description
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes);
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun applyTheme() {
        val mode = when (prefs.getString("appTheme", "system")) {
            "system" -> MODE_NIGHT_FOLLOW_SYSTEM
            "light" -> MODE_NIGHT_NO
            "dark" -> MODE_NIGHT_YES
            else -> MODE_NIGHT_FOLLOW_SYSTEM
        }
        setDefaultNightMode(mode)
    }

    private fun checkAuth() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (authToken == null) {
                withContext(Dispatchers.Main) {
                    navController.navigate(R.id.openLoginFragment)
                }
            }
        }
    }

    private fun applyBottomNavTabTitle() {
        b.bottomNav.menu.findItem(R.id.feedbacksRequestsFragment)?.title =
            if (feedbacksRequestsActiveTab == "requests")
                "Мои заявки"
            else
                "Мои отклики"
    }

    private fun applyObservers() {
        viewModel.logoutSF.collectWithLifecycle(this) {
            navController.navigate(R.id.openLoginFragment)
        }

        viewModel.messageEvent.collectWithLifecycle(this) {
            if (it=="401"){
                val snack = Snackbar.make(findViewById(android.R.id.content), Html.fromHtml("Упс...<br><b>Вы не зарегистрированы</b><br>Пожалуйста зарегистрируйтесь, чтобы видеть активные заявки от заказчика и выполнять их и пользоваться всеми функциями приложения"), Snackbar.LENGTH_LONG)
                val tv = snack.view.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
                tv.maxLines = 7
                snack.show()
            }else {
                val snackbar =
                    Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_LONG)
                        .setDuration(Snackbar.LENGTH_LONG)
                val snackbarView = snackbar.view
                val tv = snackbarView.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
                tv.maxLines = 10
                snackbar.show()
            }
        }
    }

    private fun applyListeners() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment||destination.id==R.id.termsFragment) {
                b.toolbars.isVisible = false
                window.navigationBarColor = getColor(R.color.nav_bar_color)
            }
            //b.title.text = destination.label
        }

        b.upButton.setOnClickListener {
            if (popupWindow != null) {
                popupWindow?.dismiss()
                popupWindow = null
            } else {
                /*val destination = navController.currentDestination ?: return@setOnClickListener
                if (destination.id != R.id.passwordRestorationFragment)
                    navController.navigateUp()
                else*/
                //upButtonSF.tryEmit(listOf())
                if (!navController.navigateUp())
                    finish()
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(this)

        b.menuButton.setOnClickListener {
            popupWindow = PopupWindow(this).apply {
                val menuB = PopupMenuMainBinding.inflate(layoutInflater)
                contentView = menuB.root
                menuB.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height =
                    View.MeasureSpec.makeMeasureSpec(menuB.root.measuredHeight,
                        View.MeasureSpec.UNSPECIFIED)

                setBackgroundDrawable(
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.menu_background))

                menuB.guidelineLayout.setOnClickListener{
                    navController.navigate(R.id.termsFragment,
                        bundleOf("title" to "руковдство пользователя")
                    )
                }

                menuB.feedbackLayout.setOnClickListener{
                    val email = Intent(Intent.ACTION_SENDTO)
                    email.data = Uri.parse("mailto:sabbatum@inbox.ru")
                    email.putExtra(Intent.EXTRA_SUBJECT, "Обращение пользователя")
                    email.putExtra(Intent.EXTRA_TEXT, "")
                    startActivity(email)

                    try {
                        startActivity(email)
                    } catch (ex: ActivityNotFoundException) {
                        viewModel.messageEvent.tryEmit("No email clients installed.")
                    }
                }

                menuB.shareLayout.setOnClickListener {
                    val sharingIntent = Intent(Intent.ACTION_SEND)
                    sharingIntent.type = "text/plain"
                    val shareBody = "https://apps.rustore.ru/app/com.app.transportation"
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject")
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
                    startActivity(Intent.createChooser(sharingIntent, "поделиться ..."))
                }

                menuB.logoutLayout.setOnClickListener {
                    viewModel.logout()
                    popupWindow?.dismiss()
                    popupWindow = null
                }

                menuB.themeSwitcher.apply {
                    isChecked = when (prefs.getString("appTheme", "system")) {
                        "system" -> isLightTheme().also {
                            menuB.currentTheme.text = if (it) "Светлая тема" else "Темная тема"
                        }
                        "light" -> true.also { menuB.currentTheme.text = "Светлая тема" }
                        "dark" -> false.also { menuB.currentTheme.text = "Темная тема" }
                        else -> true.also { menuB.currentTheme.text = "Светлая тема" }
                    }

                    setOnCheckedChangeListener { _, isChecked ->
                        setDefaultNightMode(
                            if (isChecked) {
                                prefs.edit { putString("appTheme", "light") }
                                menuB.currentTheme.text = "Светлая тема"
                                MODE_NIGHT_NO
                            }
                            else {
                                prefs.edit { putString("appTheme", "dark") }
                                menuB.currentTheme.text = "Темная тема"
                                MODE_NIGHT_YES
                            }
                        )
                    }
                }

                isOutsideTouchable = true
                setTouchInterceptor { v, event ->
                    v.performClick()
                    return@setTouchInterceptor if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        dismiss()
                        true
                    } else
                        false
                }

                showAsDropDown(b.menuButton,
                    1, (0.1 * b.menuButton.height).roundToInt(), Gravity.CENTER)
            }
        }
    }

    override fun onBackPressed() {
        if (popupWindow != null) {
            popupWindow?.dismiss()
            popupWindow = null
        } else {
            val destination = navController.currentDestination ?: return
            if (destination.id != R.id.passwordRestorationFragment)
                super.onBackPressed()
            else
                upButtonSF.tryEmit(listOf())
        }
    }

    private fun handleSignInResult(task : Task<GoogleSignInAccount>, isReg : Boolean = false){
        try {
            val account : GoogleSignInAccount = task.getResult(ApiException::class.java)
            if (account.email!=null&&account.displayName!=null&&account.givenName!=null) {
                if (isReg||vm.GmailLogin==null||vm.GmailPassword==null) {
                    vm.register(
                        account.email!!,
                        "GmailAccount" + account.givenName,
                        account.displayName!!
                    )
                    vm.GmailLogin = account.email
                    vm.GmailPassword = "GmailAccount" + account.givenName
                    makeSnackbar(account.displayName + ", вы зарегистрированы")
                }
                vm.authorize(vm.GmailLogin!!, vm.GmailPassword!!)
                navController.navigate(R.id.mainFragment)
            }
        }
        catch (ex : Exception){
            makeSnackbar("Ошибка Google аторизации")
            Toast.makeText(this, ex.localizedMessage, Toast.LENGTH_SHORT).show()
            println("errereereer"+ex.stackTraceToString())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] === PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.messageEvent.tryEmit("разрешение получено")
                } else {
                    viewModel.messageEvent.tryEmit("разрешение не получено. Некоторые функции приложения могут быть недоступны")
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==RC_SIGN_IN){ //if gmailRegistration
            val task : Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task, true)
        }
        else if(requestCode==123){
            val task : Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
        else {
            val callback = object : VKAuthCallback {
                override fun onLogin(token: VKAccessToken) {
                    makeSnackbar("авторизация успешна")
                    vm.VkAuthToken = token.toString()

                    val vkRequest: VKRequest<JSONObject> =
                        VKRequest<JSONObject>("account.getProfileInfo")
                            .addParam("access_key", token.toString())
                            .addParam("v", "5.130")
                    VK.execute(vkRequest, object : VKApiCallback<JSONObject> {
                        override fun success(result: JSONObject) {
                            val jobj = JSONObject(result.getJSONObject("response").toString())
                            val name = jobj.getString("first_name")
                            val id = jobj.getString("id")
                            val phone = jobj.getString("phone").replace("\\s".toRegex(), "")
                                .replace("*", "0")
                            vm.register(phone, "VK_$id$phone", name)
                            vm.VKLogin = phone
                            vm.VKPassword = "VK_$id$phone"
                            vm.authorize(phone, "VK_$id$phone")
                            navController.navigate(R.id.mainFragment)
                        }

                        override fun fail(error: Exception) {
                            makeSnackbar("laod profile error")
                        }
                    })
                }


                override fun onLoginFailed(authException: VKAuthException) {
                    makeSnackbar("ошибка авторизации")
                }
            }
            if (data == null || !VK.onActivityResult(requestCode, resultCode, data, callback)) {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    private fun makeSnackbar(message: String) {
        Snackbar.make(findViewById(R.id.title), message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "feedbacksRequestsActiveTab")
            applyBottomNavTabTitle()
    }

}