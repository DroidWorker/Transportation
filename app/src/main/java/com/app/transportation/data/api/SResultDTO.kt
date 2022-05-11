package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SResultDTO(
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("price") val price: String,
    @SerialName("description") val description: String,
)