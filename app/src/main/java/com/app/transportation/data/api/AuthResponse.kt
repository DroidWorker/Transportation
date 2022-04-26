package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AuthResponse {
    @Serializable
    data class Success(
        @SerialName("access_token") val accessToken: String,
        @SerialName("access_level") val accessLevel: Int
    ) : AuthResponse()

    @Serializable
    data class Failure(val message: String) : AuthResponse()
}
