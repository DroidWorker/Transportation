package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TarifDTO (
    @SerialName("name")val name: String,
    @SerialName("amount")var amount: String
)