package com.app.transportation.core

import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.view.inputmethod.InputMethodManager

fun Context.isLightTheme() =
    resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) !=
            Configuration.UI_MODE_NIGHT_YES

fun Context.showKeyboard(view: View) {
    val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

fun Context.hideKeyboard(view: View) {
    val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.hideSoftInputFromWindow(view.windowToken, 0)
}