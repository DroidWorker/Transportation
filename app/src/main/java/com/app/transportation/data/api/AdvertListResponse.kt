package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class AdvertListResponse {
    @Serializable
    data class Success(
        val advertMap: Map<String, AdvertDTO>,
    ) : AdvertListResponse()

    @Serializable
    data class Failure(val message: String) : AdvertListResponse()
}
