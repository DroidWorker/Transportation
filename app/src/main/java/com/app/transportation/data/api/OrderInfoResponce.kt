package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class OrderInfoResponse {
    @Serializable
    data class Success(
        val order: OrderDTO
    ) : OrderInfoResponse()

    @Serializable
    data class Failure(val message: String) : OrderInfoResponse()
}
