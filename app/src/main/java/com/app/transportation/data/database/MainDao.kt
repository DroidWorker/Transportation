package com.app.transportation.data.database

import androidx.room.*
import com.app.transportation.data.database.entities.AdvertisementCategory
import com.app.transportation.data.database.entities.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface MainDao {
    // Profile

    @Query("SELECT * FROM Profiles")
    fun profilesFlow(): Flow<List<Profile>>

    @Query("SELECT * FROM Profiles WHERE id = :id")
    fun profile(id: Long): Profile?

    @Query("DELETE FROM Profiles")
    suspend fun clearProfile()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile): Long

    @Update
    suspend fun update(profile: Profile)

    @Delete
    suspend fun delete(profile: Profile)

    //Advertisement categories

    @Query("SELECT * FROM AdvertisementCategories")
    fun advertCategoriesFlow(): Flow<List<AdvertisementCategory>>

    @Query("SELECT * FROM AdvertisementCategories WHERE id = :id")
    fun advertCategory(id: Long): AdvertisementCategory?

    @Query("DELETE FROM AdvertisementCategories")
    suspend fun clearAdvertCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: AdvertisementCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoryList: List<AdvertisementCategory>)

    @Update
    suspend fun update(category: AdvertisementCategory)

    @Delete
    suspend fun delete(category: AdvertisementCategory)
}