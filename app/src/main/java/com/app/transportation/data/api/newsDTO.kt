package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsDTO (
    @SerialName("title")val title: String,
    @SerialName("subtitle")var subtitle: String,
    @SerialName("backImage")var backImage: String
)