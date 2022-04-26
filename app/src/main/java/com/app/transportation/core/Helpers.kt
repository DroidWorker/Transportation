package com.app.transportation.core

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

fun LifecycleOwner.repeatOnLifecycle(block: CoroutineScope.() -> Unit) = lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        block()
    }
}

fun <T> Flow<T>.collect(scope: CoroutineScope, onEach: (T) -> Unit) =
    onEach { onEach(it) }
        .launchIn(scope)

fun <T> Flow<T>.collectWithLifecycle(lifecycleOwner: LifecycleOwner, onEachFlow: (T) -> Unit) =
    flowWithLifecycle(lifecycleOwner.lifecycle)
        .onEach { onEachFlow(it) }
        .launchIn(lifecycleOwner.lifecycleScope)

fun Context.getSharedPrefs(name: String): SharedPreferences =
    getSharedPreferences(name, Context.MODE_PRIVATE)