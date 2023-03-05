package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class NewsResponse {
    @Serializable
    data class Success(
        val tarif: List<NewsDTO>
    ) : NewsResponse()

    @Serializable
    data class Failure(val message: String) : NewsResponse()
}