package com.example.albuddy.stt.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.albuddy.stt.STTEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeSpeechEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : STTEngine {

    private val _transcriptionFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    override val transcriptionFlow: SharedFlow<String> = _transcriptionFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)
    override val errorFlow: SharedFlow<Throwable> = _errorFlow.asSharedFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    }

    override suspend fun startListening(audioFlow: Flow<ShortArray>) {
        withContext(Dispatchers.Main) {
            isListening = true
            startRecognizer()
        }
    }

    private fun startRecognizer() {
        if (!isListening) return

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                
                override fun onError(error: Int) {
                    if (!isListening) return
                    
                    scope.launch {
                        _errorFlow.emit(RuntimeException("Native SpeechRecognizer error: \$error"))
                        delay(500)
                        startRecognizer()
                    }
                }

                override fun onResults(results: Bundle?) {
                    if (!isListening) return
                    
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val bestMatch = matches[0]
                        scope.launch {
                            _transcriptionFlow.emit(bestMatch)
                        }
                    }
                    
                    // Continuous Loop
                    scope.launch {
                        startRecognizer()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            scope.launch {
                _errorFlow.emit(e)
                delay(500)
                startRecognizer()
            }
        }
    }

    override fun stopListening() {
        isListening = false
        // Needs to be on Main Thread
        scope.launch {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }
}
