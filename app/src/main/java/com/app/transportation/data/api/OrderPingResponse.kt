package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class OrderPingResponse {
    @Serializable
    data class Success(
        val orderMap: Map<String, PingOrderDTO>
    ) : OrderPingResponse()

    @Serializable
    data class Failure(val message: String) : OrderPingResponse()
}