package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class optionDTO(
    @SerialName("option_id") val option_id: String,
    @SerialName("name") val name: String,
    @SerialName("amount") val amount: String,
    @SerialName("status") val status: String
)