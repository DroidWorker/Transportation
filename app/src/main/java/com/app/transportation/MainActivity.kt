package com.app.transportation

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.core.isLightTheme
import com.app.transportation.data.upButtonSF
import com.app.transportation.databinding.ActivityMainBinding
import com.app.transportation.databinding.PopupMenuMainBinding
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyTheme()

        setContentView(b.root)

        b.bottomNav.setupWithNavController(navController)

        checkAuth()

        applyBottomNavTabTitle()

        applyObservers()

        applyListeners()
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
                "?????? ????????????"
            else
                "?????? ??????????????"
    }

    private fun applyObservers() {
        viewModel.logoutSF.collectWithLifecycle(this) {
            navController.navigate(R.id.openLoginFragment)
        }

        viewModel.messageEvent.collectWithLifecycle(this) {
            val snackbar =
                Snackbar.make(findViewById(android.R.id.content), it, Snackbar.LENGTH_LONG).setDuration(Snackbar.LENGTH_LONG)
            val snackbarView = snackbar.view
            val tv = snackbarView.findViewById<View>( com.google.android.material.R.id.snackbar_text) as TextView
            tv.maxLines = 10
            snackbar.show()
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

                menuB.shareLayout.setOnClickListener {
                    //TODO
                }

                menuB.logoutLayout.setOnClickListener {
                    viewModel.logout()
                    popupWindow?.dismiss()
                    popupWindow = null
                }

                menuB.themeSwitcher.apply {
                    isChecked = when (prefs.getString("appTheme", "system")) {
                        "system" -> isLightTheme().also {
                            menuB.currentTheme.text = if (it) "?????????????? ????????" else "???????????? ????????"
                        }
                        "light" -> true.also { menuB.currentTheme.text = "?????????????? ????????" }
                        "dark" -> false.also { menuB.currentTheme.text = "???????????? ????????" }
                        else -> true.also { menuB.currentTheme.text = "?????????????? ????????" }
                    }

                    setOnCheckedChangeListener { _, isChecked ->
                        setDefaultNightMode(
                            if (isChecked) {
                                prefs.edit { putString("appTheme", "light") }
                                menuB.currentTheme.text = "?????????????? ????????"
                                MODE_NIGHT_NO
                            }
                            else {
                                prefs.edit { putString("appTheme", "dark") }
                                menuB.currentTheme.text = "???????????? ????????"
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

    private fun handleSignInResult(task : Task<GoogleSignInAccount>){
        try {
            val account : GoogleSignInAccount = task.getResult(ApiException::class.java)
            if (account.email!=null&&account.displayName!=null&&account.givenName!=null) {
                vm.register(
                    account.email!!,
                    "GmailAccount" + account.givenName,
                    account.displayName!!
                )
                vm.GmailLogin = account.email
                vm.GmailPassword = "GmailAccount" + account.givenName
                makeSnackbar(account.displayName+", ???? ????????????????????????????????")
                vm.authorize(account.email!!, "GmailAccount" + account.givenName)
                navController.navigate(R.id.mainFragment)
            }
        }
        catch (ex : Exception){
            makeSnackbar("???????????? Google ????????????????????")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==RC_SIGN_IN){ //if gmailRegistration
            val task : Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
        else {
            val callback = object : VKAuthCallback {
                override fun onLogin(token: VKAccessToken) {
                    makeSnackbar("?????????????????????? ??????????????")
                    println("success")
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
                    makeSnackbar("???????????? ??????????????????????")
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