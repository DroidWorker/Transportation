package com.app.transportation.data.database.entities

data class SelectorCategory(
    val id: Int = 0,
    var realId: Int = 0,
    var level: Int = 0,
    var name: String = "",
    val parentId: Int
)
