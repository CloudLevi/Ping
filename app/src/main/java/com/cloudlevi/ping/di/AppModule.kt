package com.cloudlevi.ping.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.cloudlevi.ping.EXCHANGE_URL
import com.cloudlevi.ping.PingApplication
import com.cloudlevi.ping.api.ExchangeApiService
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.db.PingDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.prefs.Preferences
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext appContext: Context): PreferencesManager
            = PreferencesManager(appContext)

    @Provides
    @Singleton
    fun provideDatabase(
        app: Application,
        callback: PingDatabase.Callback
        ) = Room.databaseBuilder(app, PingDatabase::class.java, "ping_database")
        .addCallback(callback)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun providePingDao(db: PingDatabase) = db.pingDao()

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideExchangeAPI(): ExchangeApiService {
        val retrofit = Retrofit
            .Builder()
            .baseUrl(EXCHANGE_URL)
            .addConverterFactory(GsonConverterFactory.create()).build()
        return retrofit.create(ExchangeApiService::class.java)
    }

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope
}