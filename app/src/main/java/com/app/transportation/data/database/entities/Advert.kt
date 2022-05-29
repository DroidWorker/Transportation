package com.app.transportation.data.database.entities

data class Advert(
    var id: Int,
    val viewType: Int,
    val categoryId: Int,
    val category: String,
    val subcategoryId: Int,
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
    var ping: Map<String, String> = emptyMap()
)
