package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class AdvertCreateResponse {
    @Serializable
    data class Success(
        val message: String,
        val id: Int? = null
    ) : AdvertCreateResponse()

    @Serializable
    data class Failure(val message: String) : AdvertCreateResponse()
}
