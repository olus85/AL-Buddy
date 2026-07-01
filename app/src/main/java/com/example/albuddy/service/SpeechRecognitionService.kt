package com.example.albuddy.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.albuddy.data.local.CommandDao
import com.example.albuddy.data.model.STTEngineType
import com.example.albuddy.data.repository.SettingsRepository
import com.example.albuddy.data.repository.SpeechStateRepository
import com.example.albuddy.network.HomeAssistantApi
import com.example.albuddy.stt.AudioRecorder
import com.example.albuddy.stt.STTEngine
import com.example.albuddy.stt.vosk.VoskEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class SpeechRecognitionService : Service() {

    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var speechStateRepository: SpeechStateRepository
    @Inject lateinit var commandDao: CommandDao
    @Inject lateinit var homeAssistantApi: HomeAssistantApi
    @Inject lateinit var voskEngine: VoskEngine

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private var engineJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var activeEngine: STTEngine? = null
    
    // Track last trigger times to prevent rapid repeated executions for partial results
    private val lastTriggerTimes = mutableMapOf<Long, Long>()

    companion object {
        private const val CHANNEL_ID = "ALBuddyServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        private const val TAG = "SpeechRecognitionService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("WakelockTimeout")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                startForeground(NOTIFICATION_ID, createNotification())
                acquireWakeLock()
                startListeningPipeline()
            }
            ACTION_STOP_SERVICE -> {
                speechStateRepository.clear()
                stopListeningPipeline()
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ALBuddy::STTWakeLock"
            )
            wakeLock?.acquire()
            Log.d(TAG, "Partial WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Partial WakeLock released")
            }
        }
        wakeLock = null
    }

    private fun startListeningPipeline() {
        engineJob?.cancel()
        engineJob = serviceScope.launch(CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Pipeline error: ${throwable.message}", throwable)
            restartPipeline()
        }) {
            val voskDir = File(filesDir, "vosk-model")
            var modelPath = voskDir.absolutePath
            val children = voskDir.listFiles()
            if (children != null && children.size == 1 && children[0].isDirectory) {
                modelPath = children[0].absolutePath
            }
            voskEngine.initializeModel(modelPath)
            activeEngine = voskEngine

            // Launch command matcher
            launch {
                activeEngine?.transcriptionFlow?.collect { transcription ->
                    Log.d(TAG, "Transcription: $transcription")
                    speechStateRepository.updateTranscription(transcription)
                    matchCommand(transcription)
                }
            }

            // Launch error watcher
            launch {
                activeEngine?.errorFlow?.collect { error ->
                    Log.e(TAG, "Engine error", error)
                    restartPipeline()
                }
            }

            // Start audio recording and feed to engine
            val audioFlow = audioRecorder.startRecording()
            activeEngine?.startListening(audioFlow)
        }
    }

    private suspend fun matchCommand(transcription: String) {
        val commands = commandDao.getAllCommands().firstOrNull() ?: return
        val currentTime = System.currentTimeMillis()
        
        for (cmd in commands) {
            // Check for debouncing (3 seconds cooldown per command)
            val lastTrigger = lastTriggerTimes[cmd.id] ?: 0L
            if (currentTime - lastTrigger < 3000) continue
            
            // Match exact phrase using word boundaries, supporting multiple phrases separated by commas
            val phrases = cmd.triggerPhrase.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            var matchedPhrase: String? = null
            
            for (phrase in phrases) {
                val regex = Regex("\\b${Regex.escape(phrase)}\\b", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(transcription)) {
                    matchedPhrase = phrase
                    break
                }
            }
            
            if (matchedPhrase != null) {
                Log.d(TAG, "Command matched: $matchedPhrase (from command id ${cmd.id})")
                speechStateRepository.updateMatchedCommand(matchedPhrase)
                lastTriggerTimes[cmd.id] = currentTime
                
                // Play sound and vibrate if enabled
                serviceScope.launch {
                    val playSound = settingsRepository.playMatchSound.firstOrNull() ?: true
                    val vibrate = settingsRepository.vibrateOnMatch.firstOrNull() ?: true
                    
                    Log.d(TAG, "Feedback toggles - Sound: \$playSound, Vibrate: \$vibrate")
                    
                    if (playSound) {
                        try {
                            val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                            val mediaPlayer = android.media.MediaPlayer().apply {
                                setDataSource(applicationContext, uri)
                                setAudioAttributes(
                                    android.media.AudioAttributes.Builder()
                                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build()
                                )
                                prepare()
                                start()
                            }
                            mediaPlayer.setOnCompletionListener {
                                it.release()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error playing sound", e)
                        }
                    }
                    
                    if (vibrate) {
                        try {
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                vibratorManager.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            }
                            
                            if (vibrator.hasVibrator()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    vibrator.vibrate(
                                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK),
                                        android.os.VibrationAttributes.Builder()
                                            .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                                            .build()
                                    )
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    vibrator.vibrate(
                                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK),
                                        android.media.AudioAttributes.Builder()
                                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build()
                                    )
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(500, 255),
                                        android.media.AudioAttributes.Builder()
                                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build()
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(500)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error vibrating", e)
                        }
                    }
                }
                
                // Launch API call in a new coroutine so we don't block the STT collection loop
                serviceScope.launch {
                    try {
                        homeAssistantApi.callService(
                            domain = cmd.domain,
                            service = cmd.service,
                            body = mapOf("entity_id" to cmd.entityId)
                        )
                        Log.d(TAG, "Successfully triggered HA service: ${cmd.domain}.${cmd.service}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to call HA API", e)
                    }
                }
                break // Only trigger first match to prevent multiple actions on overlapping phrases
            }
        }
    }

    private fun restartPipeline() {
        serviceScope.launch {
            Log.d(TAG, "Restarting pipeline in 3 seconds...")
            activeEngine?.stopListening()
            engineJob?.cancel()
            delay(3000)
            startListeningPipeline()
        }
    }

    private fun stopListeningPipeline() {
        activeEngine?.stopListening()
        engineJob?.cancel()
    }

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AL Buddy Listening Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AL Buddy")
            .setContentText("Listening for commands...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechStateRepository.clear()
        stopListeningPipeline()
        releaseWakeLock()
        serviceScope.cancel()
    }
}
