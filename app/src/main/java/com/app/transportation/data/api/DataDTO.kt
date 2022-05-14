package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class DataDTO(
    @SerialName("text") val text: String,
)