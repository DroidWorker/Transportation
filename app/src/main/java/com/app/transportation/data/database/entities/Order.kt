package com.app.transportation.data.database.entities

data class Order(
    var id: Int,
    val viewType: Int,
    val categoryId: Int,
    val category: String,
    var title: String,
    var subtitle: String = "",
    var date: String,
    var time: String,
    var price: String
)
