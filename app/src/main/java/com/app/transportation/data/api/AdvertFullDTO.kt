package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdvertFullDTO(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val time: String,
    @SerialName("category_id") val categoryId: String,
    val category: String,
    val title: String,
    val price: String,
    val description: String,
    val photo: Map<String, String>,
)