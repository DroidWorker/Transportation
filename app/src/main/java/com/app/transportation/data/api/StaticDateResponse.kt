package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class StaticDateResponse {
    @Serializable
    data class Success(
        val text: String
    ) : StaticDateResponse()

    @Serializable
    data class Failure(val message: String) : StaticDateResponse()
}