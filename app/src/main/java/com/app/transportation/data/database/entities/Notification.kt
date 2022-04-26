package com.app.transportation.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Notifications")
data class Notification(
    @PrimaryKey val id: Long,
    var title: String,
    var description: String,
    var isRead: Boolean
)