package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class RegisterResponse {
    @Serializable
    class Success(val message: String) : RegisterResponse()
    @Serializable
    data class Failure(val message: String) : RegisterResponse()
}