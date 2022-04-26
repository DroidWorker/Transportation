package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class OrderListResponse {
    @Serializable
    data class Success(
        val orderMap: Map<String, OrderDTO>
    ) : OrderListResponse()

    @Serializable
    data class Failure(val message: String) : OrderListResponse()
}
