package com.app.transportation

import android.app.Application
import com.app.transportation.di.ktorModule
import com.app.transportation.di.repositoriesModule
import com.app.transportation.di.roomModule
import com.app.transportation.di.settingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.NONE)
            androidContext(this@App)
            modules(roomModule, repositoriesModule, settingsModule, ktorModule)
        }
    }

}