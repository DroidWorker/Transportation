package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusinessLastItemDTO (
    @SerialName("category") val category: String,
    @SerialName("price") val price: String,
    @SerialName("photo") val photo: String
)