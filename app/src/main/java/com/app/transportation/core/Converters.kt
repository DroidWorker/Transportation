package com.app.transportation.core

import android.content.Context

fun Context.dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()
//fun Context.spToPx(sp: Int) = (sp * resources.displayMetrics.scaledDensity).toInt()

//fun Context.pxToDp(px: Int) = (px / resources.displayMetrics.density).toInt()
//fun Context.pxToSp(px: Int) = (px / resources.displayMetrics.scaledDensity).toInt()