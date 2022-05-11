package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class AddPingResponse {
    @Serializable
    data class Success(val message: String) : AddPingResponse()

    @Serializable
    data class Failure(val message: String) : AddPingResponse()
}