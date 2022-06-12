package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class ProfileShortResponse {
    @Serializable
    data class Success(
        val profile: ProfileShortDTO
    ) : ProfileShortResponse()

    @Serializable
    data class Failure(val message: String) : ProfileShortResponse()
}