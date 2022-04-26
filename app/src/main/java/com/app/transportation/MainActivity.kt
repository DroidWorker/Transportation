package com.app.transportation

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val navController
            by lazy { (supportFragmentManager.findFragmentById(R.id.navHostFragment)
                    as NavHostFragment).navController }

    val b by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val viewModel by viewModels<MainViewModel>()

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private val authToken: String?
        get() = prefs.getString("authToken", null).takeIf { it != "" }

    private val feedbacksRequestsActiveTab
        get() = prefs.getString("feedbacksRequestsActiveTab", null) ?: "requests"

    private var popupWindow: PopupWindow? = null


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
                "Мои заявки"
            else
                "Мои отклики"
    }

    private fun applyObservers() {
        viewModel.logoutSF.collectWithLifecycle(this) {
            navController.navigate(R.id.openLoginFragment)
        }

        viewModel.messageEvent.collectWithLifecycle(this) {
            Snackbar
                .make(findViewById(android.R.id.content), it, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun applyListeners() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment) {
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "feedbacksRequestsActiveTab")
            applyBottomNavTabTitle()
    }

}