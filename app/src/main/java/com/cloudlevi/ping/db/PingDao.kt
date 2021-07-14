package com.cloudlevi.ping.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cloudlevi.ping.data.ApartmentHomePost
import kotlinx.coroutines.flow.Flow

@Dao
interface PingDao {

    @Query("SELECT * FROM apartments_homepage_table LIMIT 30 OFFSET :offset")
    fun getAllHomePageApartments(offset: Int): Flow<List<ApartmentHomePost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListOfApartments(listOfApartments: List<ApartmentHomePost>)
}