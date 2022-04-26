package com.app.transportation.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.app.transportation.data.api.AuthResponse
import com.app.transportation.data.api.InfoMessageResponse
import com.app.transportation.data.api.RegisterResponse
import com.app.transportation.data.api.SmsCodeResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class LoginRepository : KoinComponent {

    private val client: HttpClient by inject()

    private val prefs: SharedPreferences by inject(named("MainSettings"))

    private var authToken: String?
        get() = prefs.getString("authToken", null)
        set(value) = prefs.edit(true) { putString("authToken", value) }

    suspend fun authorize(
        login: String,
        password: String
    ): AuthResponse = kotlin.runCatching {
        val response: HttpResponse = client.get("http://api-transport.mvp-pro.top/api/v1/sign_in") {
            parameter("login", login)
            parameter("password", password)
        }
        val responseBody: String = response.receive()
        val json = Json.Default
        val result = json.decodeFromString<AuthResponse.Success>(responseBody)
        (result as? AuthResponse.Success)?.let { authToken = result.accessToken }
        result
    }.getOrElse {
        val message = it.message
            ?.substringAfter("{\"message\":\"")
            ?.replace("\"}\"", "")
            ?: "Unexpected error"
        AuthResponse.Failure(message)
    }

    suspend fun register(
        login: String,
        password: String,
        name: String
    ): RegisterResponse = kotlin.runCatching {
        val response: HttpResponse = client.submitForm(
            url = "http://api-transport.mvp-pro.top/api/v1/sign_up",
            formParameters = Parameters.build {
                append("login", login)
                append("password", password)
                append("first_name", name)
                append("last_name", "null")
            }
        )
        Json.decodeFromString<RegisterResponse.Success>(response.receive())
    }.getOrElse {
        val message = it.message
            ?.substringAfter("{\"message\":\"")
            ?.replace("\"}\"", "")
            ?: "Unexpected error"
        RegisterResponse.Failure(message)
    }

    suspend fun requestSmsCode(login: String): SmsCodeResponse = kotlin.runCatching {
        val response: HttpResponse = client.submitForm(
            url = "http://api-transport.mvp-pro.top/api/v1/password_restore",
            formParameters = Parameters.build {
                append("login", login)
            }
        )
        Json.decodeFromString<SmsCodeResponse.Success>(response.receive())
    }.getOrElse {
        val message = it.message
            ?.substringAfter("{\"message\":\"")
            ?.replace("\"}\"", "")
            ?: "Unexpected error"
        SmsCodeResponse.Failure(message)
    }

    suspend fun sendSmsCode(login: String, code: String): String = kotlin.runCatching {
        val response: HttpResponse = client.submitForm(
            url = "http://api-transport.mvp-pro.top/api/v1/password_code",
            formParameters = Parameters.build {
                append("login", login)
                append("code", code)
            }
        )
        Json.decodeFromString<InfoMessageResponse>(response.receive()).message
    }.getOrElse {
        it.message
            ?.substringAfter("{\"message\":\"")
            ?.replace("\"}\"", "")
            ?: "Unexpected error"
    }

    suspend fun sendNewPassword(login: String, password: String): String = kotlin.runCatching {
        val response: HttpResponse = client.submitForm(
            url = "http://api-transport.mvp-pro.top/api/v1/password_save",
            formParameters = Parameters.build {
                append("login", login)
                append("value", password)
            }
        )
        Json.decodeFromString<InfoMessageResponse>(response.receive()).message
    }.getOrElse {
        it.message
            ?.substringAfter("{\"message\":\"")
            ?.replace("\"}\"", "")
            ?: "Unexpected error"
    }

}