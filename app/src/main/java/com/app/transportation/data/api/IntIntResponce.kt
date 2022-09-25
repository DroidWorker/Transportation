package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class IntIntResponce {
    @Serializable
    data class Success(
        val categoriesList: Map<Int, Int>
    ) : IntIntResponce()

    @Serializable
    data class Failure(val message: String) : IntIntResponce()
}