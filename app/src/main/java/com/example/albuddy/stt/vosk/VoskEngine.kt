package com.example.albuddy.stt.vosk

import com.example.albuddy.stt.STTEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskEngine @Inject constructor() : STTEngine {

    private val _transcriptionFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    override val transcriptionFlow: SharedFlow<String> = _transcriptionFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)
    override val errorFlow: SharedFlow<Throwable> = _errorFlow.asSharedFlow()

    private var recognizer: Recognizer? = null
    private var isListening = false

    suspend fun initializeModel(modelPath: String) {
        withContext(Dispatchers.IO) {
            try {
                val model = Model(modelPath)
                recognizer = Recognizer(model, 16000.0f)
            } catch (e: Exception) {
                _errorFlow.emit(e)
            }
        }
    }

    override suspend fun startListening(audioFlow: Flow<ShortArray>) {
        if (recognizer == null) {
            _errorFlow.emit(IllegalStateException("Vosk Recognizer not initialized"))
            return
        }
        isListening = true

        audioFlow.catch { e ->
            _errorFlow.emit(e)
        }.collect { buffer ->
            if (!isListening) return@collect

            try {
                if (recognizer!!.acceptWaveForm(buffer, buffer.size)) {
                    val result = recognizer!!.result
                    parseResult(result)
                } else {
                    val partialResult = recognizer!!.partialResult
                    parsePartialResult(partialResult)
                }
            } catch (e: Exception) {
                _errorFlow.emit(e)
            }
        }
    }

    override fun stopListening() {
        isListening = false
    }

    private suspend fun parseResult(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val text = json.optString("text", "")
            if (text.isNotBlank()) {
                _transcriptionFlow.emit(text)
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
    }

    private suspend fun parsePartialResult(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val partial = json.optString("partial", "")
            if (partial.isNotBlank()) {
                _transcriptionFlow.emit(partial) 
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
    }
}
