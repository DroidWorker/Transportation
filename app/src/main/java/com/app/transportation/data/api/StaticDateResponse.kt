package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class StaticDateResponse {
    @Serializable
    data class Success(
        @SerialName("id")val id: DataDTO
    ) : StaticDateResponse()

    @Serializable
    data class Failure(val message: String) : StaticDateResponse()
}