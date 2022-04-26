package com.app.transportation.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Advertisements")
data class Advertisement(
    @PrimaryKey val id: Long = 0,
    var title1: String = "",
    var title2: String = "",
    var title3: String = ""
)
