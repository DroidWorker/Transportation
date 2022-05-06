package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class VkInfoResponse {
    @Serializable
    data class Success(
        @SerialName("first_name") val first_name: String,
        @SerialName("last_name") val last_name: String,
        @SerialName("phone") val phone: String
    ) : VkInfoResponse()

    @Serializable
    data class Failure(val message: String) : VkInfoResponse()
}
