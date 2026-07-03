package com.example.albuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.albuddy.data.model.Command
import com.example.albuddy.data.model.VoskWord

@Database(entities = [Command::class, VoskWord::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao
    abstract fun voskWordDao(): VoskWordDao
}
