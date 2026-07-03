package com.example.albuddy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vosk_words")
data class VoskWord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String
)
