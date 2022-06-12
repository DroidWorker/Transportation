package com.app.transportation.data.api
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileShortDTO(
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("phone") val phone: String,
    @SerialName("location") val location: String
)