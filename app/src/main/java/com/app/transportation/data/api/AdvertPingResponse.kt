package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class AdvertPingResponse {
    @Serializable
    data class Success(
        val advertMap: Map<String, PingAdvertDTO>,
    ) : AdvertPingResponse()

    @Serializable
    data class Failure(val message: String) : AdvertPingResponse()
}