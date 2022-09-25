package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class StringResponce {
    @Serializable
    data class Success(
        val categoriesList: List<String>
        ) : StringResponce()

    @Serializable
    data class Failure(val message: String) : StringResponce()
}