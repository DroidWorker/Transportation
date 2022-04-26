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
    var price: String? = null,
    var description: String = "",
    var photo: List<String> = emptyList(),
    var fromLocation: String = "",
    var toLocation: String = "",
    var payment: String = ""
)
