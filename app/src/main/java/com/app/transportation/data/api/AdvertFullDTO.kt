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
    @SerialName("city") val city: String,
    val description: String,
    val photo: Map<String, String>,
    val options: List<optionDTO>,
    @SerialName("bussiness") val bussiness: String?,
    @SerialName("bussiness_update") val bussiness_update: String?
)