package com.app.transportation.data.database.entities

import com.app.transportation.data.api.PingDTO
import com.app.transportation.data.api.optionDTO

data class Advert(
    var id: Int,
    var userId: String = "",
    val viewType: Int,
    val categoryId: Int,
    val category: String,
    val subcategoryId: Int,
    val fourthLevelCategory: Int = 0,
    var title: String,
    var subtitle: String = "",
    var date: String,
    var time: String,
    var price: String? = "0",
    var description: String = "",
    var photo: List<String> = emptyList(),
    var fromCity: String = "",
    var fromRegion: String = "",
    var fromPlace: String = "",
    var toCity: String = "",
    var toRegion: String = "",
    var toPlace: String = "",
    var payment: String = "",
    var ping: Map<String, String?> = emptyMap(),
    var profile: List<PingDTO> = emptyList(),
    var options: List<optionDTO> = emptyList()
)
