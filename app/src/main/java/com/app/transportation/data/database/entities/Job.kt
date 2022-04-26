package com.app.transportation.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Jobs")
data class Job(
    @PrimaryKey var title: String,
    var price: String
)
