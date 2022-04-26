package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class SmsCodeResponse {
    @Serializable
    class Success(val code: Int) : SmsCodeResponse()
    @Serializable
    data class Failure(val message: String) : SmsCodeResponse()
}