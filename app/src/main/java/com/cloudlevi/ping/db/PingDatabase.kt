package com.cloudlevi.ping.db

import android.database.DatabaseErrorHandler
import androidx.room.Database
import androidx.room.RoomDatabase
import com.cloudlevi.ping.data.ApartmentHomePost
import com.cloudlevi.ping.di.AppModule
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [ApartmentHomePost::class], version = 1)
abstract class PingDatabase: RoomDatabase() {

    abstract fun pingDao(): PingDao

    class Callback @Inject constructor(
        private val database: Provider<PingDatabase>,
        @AppModule.ApplicationScope private val applicationScope: CoroutineScope
    ) : RoomDatabase.Callback(){

    }
}