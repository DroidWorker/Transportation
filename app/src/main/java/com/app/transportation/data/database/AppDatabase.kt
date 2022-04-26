package com.app.transportation.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.transportation.data.database.entities.*

@Database(
    entities = [
        Profile::class,
        Job::class,
        Notification::class,
        FeedbackRequest::class,
        AdvertisementCategory::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(TypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val dao: MainDao
}