package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class UpdateProfileResponse {
    @Serializable
    data class Success(
        @SerialName("id") val id: String,
        @SerialName("first_name") val firstName: String,
        @SerialName("last_name") val lastName: String,
        val phone: String,
        val email: String,
        val card: String,
        val location: String,
        val avatar: String,
        val status: String,
        @SerialName("bussiness") val bussiness: String,
        @SerialName("bussiness_update") val bussiness_update: String?
    ) : UpdateProfileResponse()

    @Serializable
    data class Failure(val message: String) : UpdateProfileResponse()
}
