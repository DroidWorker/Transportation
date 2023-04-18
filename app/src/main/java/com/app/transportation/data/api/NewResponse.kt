package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class NewsResponse {
    @Serializable
    data class Success(
        val tarif: Map<String, NewsDTO>
    ) : NewsResponse()

    @Serializable
    data class Failure(val message: String) : NewsResponse()
}