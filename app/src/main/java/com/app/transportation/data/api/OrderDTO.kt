package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderDTO(
    @SerialName("id")val id: String,
    @SerialName("date")val date: String,
    @SerialName("time")val time: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("category")val category: String,
    @SerialName("from_city") val fromCity: String?,
    @SerialName("from_region") val fromRegion: String?,
    @SerialName("from_place") val fromPlace: String?,
    @SerialName("from_date") val fromDate: String?,
    @SerialName("from_time") val fromTime: String?,
    @SerialName("to_city") val toCity: String?,
    @SerialName("to_region") val toRegion: String?,
    @SerialName("to_place") val toPlace: String?,
    @SerialName("description")val description: String,
    @SerialName("name")val name: String,
    @SerialName("phone")val phone: String,
    @SerialName("payment")val payment: String,
    @SerialName("photo")val photo: Map<String, String?>,
    @SerialName("ping") val ping: Map<String, String?>
)
