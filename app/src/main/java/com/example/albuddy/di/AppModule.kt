package com.example.albuddy.di

import android.content.Context
import androidx.room.Room
import com.example.albuddy.data.local.AppDatabase
import com.example.albuddy.data.local.CommandDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "albuddy.db"
        ).build()
    }

    @Provides
    fun provideCommandDao(appDatabase: AppDatabase): CommandDao {
        return appDatabase.commandDao()
    }
}
