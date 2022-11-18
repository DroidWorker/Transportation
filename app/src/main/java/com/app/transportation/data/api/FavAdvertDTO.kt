package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
//used for favourites & ping
@Serializable
data class FavAdvertDTO(
    val id: String,
    @SerialName("advert_id")val advert_id: String,
    val date: String,
    val time: String,
    @SerialName("category_id") val categoryId: String,
    val category: String,
    @SerialName("title")val title: String,
    @SerialName("price")val price: String,
    @SerialName("city")val city: String,
    @SerialName("description")val description: String,
    val photo: Map<String, String?>
)