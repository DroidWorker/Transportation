package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class OrderFavResponse {
    @Serializable
    data class Success(
        val orderMap: Map<String, FavOrderDTO>
    ) : OrderFavResponse()

    @Serializable
    data class Failure(val message: String) : OrderFavResponse()
}