package com.app.transportation.data.database.entities

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Advertisements")
data class Advertisement(
    @PrimaryKey val id: Long = 0,
    var title1: String = "",
    var title2: String = "",
    var iamge: Bitmap? = null
)
