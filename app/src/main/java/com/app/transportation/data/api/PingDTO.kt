package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PingDTO (
    @SerialName("user_id")val userId: String,
    @SerialName("first_name")var firstName: String,
    @SerialName("last_name")var lastName: String,
    @SerialName("phone")var phone: String,
    @SerialName("status")val status: String?
)