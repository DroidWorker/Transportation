package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class businessLastResponce {
    @Serializable
    data class Success(
        val categoriesList: List<BusinessLastItemDTO>
    ) : businessLastResponce()

    @Serializable
    data class Failure(val message: String) : businessLastResponce()
}