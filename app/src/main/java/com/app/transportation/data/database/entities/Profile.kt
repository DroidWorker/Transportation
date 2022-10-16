package com.app.transportation.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Profiles")
data class Profile(
    @PrimaryKey val id: Long = 0L,
    var login: String = "",
    var name: String = "",
    var telNumber: String = "",
    var email: String = "",
    var paymentCard: String = "",
    var cityArea: String = "",
    var specialization: String = "",
    var avatar: String = "",
    var bussiness: String = ""
)
