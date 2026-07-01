package com.example.albuddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.albuddy.data.model.Command
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandDao {
    @Query("SELECT * FROM commands")
    fun getAllCommands(): Flow<List<Command>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCommand(command: Command): Long

    @Update
    fun updateCommand(command: Command): Int

    @Delete
    fun deleteCommand(command: Command): Int
}
