package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class AdvertFavResponse {
    @Serializable
    data class Success(
        val advertMap: Map<String, FavAdvertDTO>,
    ) : AdvertFavResponse()

    @Serializable
    data class Failure(val message: String) : AdvertFavResponse()
}