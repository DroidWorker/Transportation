package com.app.transportation.di

import androidx.room.Room
import com.app.transportation.core.getSharedPrefs
import com.app.transportation.data.LoginRepository
import com.app.transportation.data.Repository
import com.app.transportation.data.database.AppDatabase
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val roomModule = module {
    // Database instance
    single {
        Room.databaseBuilder(androidApplication(), AppDatabase::class.java, "TransportationDb")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }
    // Dao
    single { get<AppDatabase>().dao }
}

val repositoriesModule = module {
    single { Repository(get()) }
    single { LoginRepository() }
}

val settingsModule = module {
    /*single {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile(MAIN_SETTINGS) }
    }*/
    single(named("MainSettings")) { androidContext().getSharedPrefs("MainSettings") }
}

val ktorModule = module {
    single {
        HttpClient(Android) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(kotlinx.serialization.json.Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }
}


