package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AdvertDataResponse {
    @Serializable
    data class Success(
        @SerialName("id") val categories: Map<String, AdvertData>
    ) : AdvertDataResponse()

    @Serializable
    data class Failure(val message: String) : AdvertDataResponse()
}
@Serializable
data class AdvertData(
    val name: String,
    @SerialName("parent_id") val parentId: String
)
