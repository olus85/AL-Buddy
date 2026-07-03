package com.example.albuddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.albuddy.data.model.VoskWord
import kotlinx.coroutines.flow.Flow

@Dao
interface VoskWordDao {
    @Query("SELECT * FROM vosk_words")
    fun getAllWords(): Flow<List<VoskWord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWord(word: VoskWord): Long

    @Delete
    fun deleteWord(word: VoskWord): Int
}
