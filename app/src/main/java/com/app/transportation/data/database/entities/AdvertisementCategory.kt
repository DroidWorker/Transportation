package com.app.transportation.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "AdvertisementCategories")
data class AdvertisementCategory(
    @PrimaryKey val id: Int = 0,
    var level: Int = 0,
    var name: String = "",
    val parentId: Int,
    var childId: Int = 0
)
