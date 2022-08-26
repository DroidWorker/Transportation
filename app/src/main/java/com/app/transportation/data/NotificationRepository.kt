package com.app.transportation.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.app.transportation.core.getSharedPrefs
import com.app.transportation.data.api.NoticeDTO
import com.app.transportation.data.api.NoticeResponce
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject
import kotlin.coroutines.coroutineContext

class NotificationRepository (ctx : Context){
    val ctx : Context? = ctx

    private val client: HttpClient = HttpClient()
    private val prefs: SharedPreferences = ctx!!.getSharedPrefs("MainSettings")
    private var authToken: String? = prefs.getString("authToken", null).takeIf { it != "" }

    suspend fun getNotice(): NoticeResponce = kotlin.runCatching {
        val token = authToken ?: return@runCatching NoticeResponce.Failure("token is null")
        val response: HttpResponse =
            client.get("http://api-transport.mvp-pro.top/api/v1/notice") {
                headers { append("X-Access-Token", token) }
            }
        var responseBody: String = response.receive()
        val json = Json.Default

        kotlin.runCatching {
            val map = json.decodeFromString<List<NoticeDTO>>(responseBody)
            NoticeResponce.Success(map)
        }.getOrElse {
            println("notice = $it")
            json.decodeFromString<NoticeResponce.Failure>(responseBody)
        }
    }.getOrElse {
        NoticeResponce.Failure(it.stackTraceToString())
    }
}