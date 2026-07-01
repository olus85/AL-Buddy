package com.example.albuddy.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechStateRepository @Inject constructor() {
    private val _liveTranscription = MutableStateFlow("")
    val liveTranscription: StateFlow<String> = _liveTranscription.asStateFlow()

    private val _lastMatchedCommand = MutableStateFlow<String?>(null)
    val lastMatchedCommand: StateFlow<String?> = _lastMatchedCommand.asStateFlow()

    fun updateTranscription(text: String) {
        _liveTranscription.value = text
    }

    fun updateMatchedCommand(commandPhrase: String) {
        _lastMatchedCommand.value = commandPhrase
    }

    fun clear() {
        _liveTranscription.value = ""
        _lastMatchedCommand.value = null
    }
}
