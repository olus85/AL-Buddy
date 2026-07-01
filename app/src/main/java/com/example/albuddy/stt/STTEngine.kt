package com.example.albuddy.stt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface STTEngine {
    suspend fun startListening(audioFlow: Flow<ShortArray>)
    fun stopListening()
    
    val transcriptionFlow: SharedFlow<String>
    val errorFlow: SharedFlow<Throwable>
}
