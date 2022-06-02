package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class AdvertFullListResponse {
    @Serializable
    data class Success(
        val advertMap: Map<String, AdvertFullDTO>,
    ) : AdvertFullListResponse()

    @Serializable
    data class Failure(val message: String) : AdvertFullListResponse()
}
