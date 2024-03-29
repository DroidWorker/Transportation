package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdvertDTO(
    val id: String,
    val date: String,
    val time: String,
    @SerialName("category_id") val categoryId: String,
    val category: String,
    val title: String,
    val price: String,
    @SerialName("city") val city: String,
    val description: String,
    val photo: Map<String, String>,
    @SerialName("ping") val ping: Map<String, String?>
)