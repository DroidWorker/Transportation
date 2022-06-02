package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderFullDTO(
    val id: String,
    val date: String,
    val time: String,
    @SerialName("category_id") val categoryId: String,
    val category: String,
    @SerialName("from_city") val fromCity: String?,
    @SerialName("from_region") val fromRegion: String?,
    @SerialName("from_place") val fromPlace: String?,
    @SerialName("from_date") val fromDate: String?,
    @SerialName("from_time") val fromTime: String?,
    @SerialName("to_city") val toCity: String?,
    @SerialName("to_region") val toRegion: String?,
    @SerialName("to_place") val toPlace: String?,
    val description: String,
    val name: String,
    val phone: String,
    val payment: String,
    val photo: Map<String, String>
)
