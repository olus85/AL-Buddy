package com.example.albuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.albuddy.data.model.Command

@Database(entities = [Command::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao
}
