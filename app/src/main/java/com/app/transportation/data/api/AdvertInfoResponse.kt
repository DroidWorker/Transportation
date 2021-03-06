package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class AdvertInfoResponse {
    @Serializable
    data class Success(
        val id: String,
        val date: String,
        val time: String,
        @SerialName("category_id") val categoryId: String,
        val category: String,
        val title: String,
        val price: String,
        val description: String,
        val photo: Map<String, String>
    ) : AdvertInfoResponse()

    @Serializable
    data class Failure(val message: String) : AdvertInfoResponse()
}
