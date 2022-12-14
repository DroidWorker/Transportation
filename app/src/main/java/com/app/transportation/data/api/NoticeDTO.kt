package com.app.transportation.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoticeDTO (
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("data_id") val dataId: String,
    @SerialName("type") val type: String,
    @SerialName("text") val text: String,
    @SerialName("date") val date: String
)