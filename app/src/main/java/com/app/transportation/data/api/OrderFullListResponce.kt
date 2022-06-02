package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class OrderFullListResponse {
    @Serializable
    data class Success(
        val orderMap: Map<String, OrderFullDTO>
    ) : OrderFullListResponse()

    @Serializable
    data class Failure(val message: String) : OrderFullListResponse()
}
