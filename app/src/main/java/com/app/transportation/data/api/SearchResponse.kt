package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class SearchResponse {
    @Serializable
    data class Success(
        val resMap: Map<String, SResultDTO>
    ) : SearchResponse()

    @Serializable
    data class Failure(val message: String) : SearchResponse()
}
