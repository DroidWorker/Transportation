package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class AddFavoriteResponsee {
    @Serializable
    data class Success(val message: String) : AddFavoriteResponsee()

    @Serializable
    data class Failure(val message: String) : AddFavoriteResponsee()
}