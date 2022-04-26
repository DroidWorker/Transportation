package com.app.transportation.data.database.entities

data class ProfileRvItem(
    val id: Long,
    val viewType: Int,
    var title: String = "",
    val realId: Int? = null,
    val categoryId: Int? = null
)
