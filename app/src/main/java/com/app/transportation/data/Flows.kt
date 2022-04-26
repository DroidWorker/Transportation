package com.app.transportation.data

import kotlinx.coroutines.flow.MutableSharedFlow

val upButtonSF = MutableSharedFlow<List<Any>>(extraBufferCapacity = 1)