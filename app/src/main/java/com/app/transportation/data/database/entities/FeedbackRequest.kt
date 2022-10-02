package com.app.transportation.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.transportation.core.currentDateTime
import java.util.*

@Entity(tableName = "FeedbacksAndRequests")
data class FeedbackRequest(
    @PrimaryKey val id: Long = 0,
    var status: String = "",
    val viewType: Int,
    var title: String = "",
    var subtitle: String = "",
    var dateTime: Date = currentDateTime,
    var price: Int = 0
)
