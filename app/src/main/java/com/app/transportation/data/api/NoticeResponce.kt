package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class NoticeResponce {
    @Serializable
    data class Success(
        val notice: Map<String, NoticeDTO>
    ) : NoticeResponce()

    @Serializable
    data class Failure(val message: String) : NoticeResponce()
}