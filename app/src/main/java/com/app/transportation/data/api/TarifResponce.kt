package com.app.transportation.data.api

import kotlinx.serialization.Serializable

@Serializable
sealed class TarifResponce {
    @Serializable
    data class Success(
        val tarif: Map<String, TarifDTO>
    ) : TarifResponce()

    @Serializable
    data class Failure(val message: String) : TarifResponce()
}