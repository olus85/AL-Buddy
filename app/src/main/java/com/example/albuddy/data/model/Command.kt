package com.example.albuddy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commands")
data class Command(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val triggerPhrase: String,
    val entityId: String,
    val domain: String,
    val service: String
)
