package com.example.albuddy.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.albuddy.data.local.CommandDao
import com.example.albuddy.data.model.Command
import com.example.albuddy.data.model.STTEngineType
import com.example.albuddy.data.repository.SettingsRepository
import com.example.albuddy.network.HomeAssistantApi
import com.example.albuddy.network.model.HAEntity
import com.example.albuddy.network.model.HAServiceDomain
import com.example.albuddy.service.SpeechRecognitionService
import com.example.albuddy.data.repository.SpeechStateRepository
import com.example.albuddy.utils.ModelDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import com.example.albuddy.data.repository.VoskDictionaryRepository
import com.example.albuddy.data.model.VoskWord
import com.google.gson.Gson
import com.example.albuddy.data.model.BackupData
import android.net.Uri
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val commandDao: CommandDao,
    private val settingsRepository: SettingsRepository,
    private val homeAssistantApi: HomeAssistantApi,
    private val modelDownloader: ModelDownloader,
    private val speechStateRepository: SpeechStateRepository,
    private val voskDictionaryRepository: VoskDictionaryRepository
) : ViewModel() {

    val commands: StateFlow<List<Command>> = commandDao.getAllCommands()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val haUrl = settingsRepository.haUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val haToken = settingsRepository.haToken.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val activeEngine = settingsRepository.activeSttEngine.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), STTEngineType.VOSK)
    val playMatchSound = settingsRepository.playMatchSound.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val vibrateOnMatch = settingsRepository.vibrateOnMatch.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _entities = MutableStateFlow<List<HAEntity>>(emptyList())
    val entities: StateFlow<List<HAEntity>> = _entities.asStateFlow()

    private val _services = MutableStateFlow<List<HAServiceDomain>>(emptyList())
    val services: StateFlow<List<HAServiceDomain>> = _services.asStateFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val _downloadProgress = MutableStateFlow(-1)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _serviceError = MutableStateFlow<String?>(null)
    val serviceError: StateFlow<String?> = _serviceError.asStateFlow()
    
    val liveTranscription: StateFlow<String> = speechStateRepository.liveTranscription
    val lastMatchedCommand: StateFlow<String?> = speechStateRepository.lastMatchedCommand

    private val _connectionStatus = MutableStateFlow<Boolean?>(null)
    val connectionStatus: StateFlow<Boolean?> = _connectionStatus.asStateFlow()

    fun clearServiceError() {
        _serviceError.value = null
    }

    init {
        checkModelExists()
    }
    
    data class DictionaryDisplayItem(val word: String, val isCommand: Boolean, val voskWord: VoskWord? = null)

    val dictionaryDisplayItems: StateFlow<List<DictionaryDisplayItem>> = kotlinx.coroutines.flow.combine(
        commands,
        voskDictionaryRepository.getAllCustomWords()
    ) { cmds, customWords ->
        val items = mutableListOf<DictionaryDisplayItem>()
        cmds.forEach { items.add(DictionaryDisplayItem(it.triggerPhrase, true)) }
        customWords.forEach { items.add(DictionaryDisplayItem(it.word, false, it)) }
        items.sortedBy { it.word.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addVoskWord(input: String) {
        viewModelScope.launch {
            input.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { word ->
                voskDictionaryRepository.addWord(word)
            }
        }
    }

    fun removeVoskWord(voskWord: VoskWord) {
        viewModelScope.launch {
            voskDictionaryRepository.removeWord(voskWord)
        }
    }
    
    fun setActiveEngine(engine: STTEngineType) {
        viewModelScope.launch {
            settingsRepository.setActiveSttEngine(engine)
        }
    }

    fun fetchEntities() {
        viewModelScope.launch {
            try {
                _entities.value = homeAssistantApi.getStates()
                _services.value = homeAssistantApi.getServices()
            } catch (e: Exception) {
                _entities.value = emptyList()
                _services.value = emptyList()
            }
        }
    }

    var editingCommand: Command? = null

    fun addCommand(phrase: String, entityId: String, domain: String, service: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            commandDao.insertCommand(Command(triggerPhrase = phrase, entityId = entityId, domain = domain, service = service))
        }
    }

    fun updateCommand(command: Command) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            commandDao.updateCommand(command)
        }
    }

    fun deleteCommand(command: Command) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            commandDao.deleteCommand(command)
        }
    }

    fun updateHaUrl(url: String) {
        viewModelScope.launch { settingsRepository.setHaUrl(url) }
    }

    fun updateHaToken(token: String) {
        viewModelScope.launch { settingsRepository.setHaToken(token) }
    }

    fun updatePlaySound(play: Boolean) {
        viewModelScope.launch { settingsRepository.setPlayMatchSound(play) }
    }

    fun updateVibrate(vibrate: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrateOnMatch(vibrate) }
    }

    fun testConnection() {
        _connectionStatus.value = null // reset while testing
        viewModelScope.launch {
            try {
                homeAssistantApi.getStates()
                _connectionStatus.value = true
            } catch (e: Exception) {
                _connectionStatus.value = false
            }
        }
    }

    fun toggleService() {
        try {
            val intent = Intent(context, SpeechRecognitionService::class.java)
            if (_serviceRunning.value) {
                intent.action = SpeechRecognitionService.ACTION_STOP_SERVICE
                context.startService(intent)
                _serviceRunning.value = false
            } else {
                intent.action = SpeechRecognitionService.ACTION_START_SERVICE
                context.startForegroundService(intent)
                _serviceRunning.value = true
            }
        } catch (e: Exception) {
            _serviceError.value = "Failed to start service: ${e.message}"
            _serviceRunning.value = false
        }
    }

    fun checkModelExists() {
        val dirName = "vosk-model"
        val targetFile = java.io.File(context.filesDir, dirName)
        if (targetFile.exists() && (targetFile.listFiles()?.isNotEmpty() == true || targetFile.isFile)) {
            _downloadProgress.value = 100
        } else {
            _downloadProgress.value = -1
        }
    }

    fun downloadModel() {
        viewModelScope.launch {
            val url = "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip" // Example URL
            val dirName = "vosk-model"

            _downloadProgress.value = 0
            try {
                modelDownloader.downloadAndUnpackModel(url, dirName).collect { progress ->
                    _downloadProgress.value = progress
                }
            } catch (e: Exception) {
                _downloadProgress.value = -1 // Error
                _serviceError.value = "Download failed: ${e.message}"
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val currentUrl = haUrl.firstOrNull() ?: ""
                val currentToken = haToken.firstOrNull() ?: ""
                val currentPlaySound = playMatchSound.firstOrNull() ?: true
                val currentVibrate = vibrateOnMatch.firstOrNull() ?: true
                val currentCommands = commandDao.getAllCommands().firstOrNull() ?: emptyList()

                val backupData = BackupData(
                    haUrl = currentUrl,
                    haToken = currentToken,
                    playSound = currentPlaySound,
                    vibrate = currentVibrate,
                    commands = currentCommands
                )
                
                val jsonStr = Gson().toJson(backupData)
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonStr.toByteArray())
                }
            } catch (e: Exception) {
                _serviceError.value = "Backup failed: ${e.message}"
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                if (jsonStr != null) {
                    val backupData = Gson().fromJson(jsonStr, BackupData::class.java)
                    
                    settingsRepository.setHaUrl(backupData.haUrl ?: "")
                    settingsRepository.setHaToken(backupData.haToken ?: "")
                    settingsRepository.setPlayMatchSound(backupData.playSound)
                    settingsRepository.setVibrateOnMatch(backupData.vibrate)
                    
                    // Restore commands
                    val existingCommands = commandDao.getAllCommands().firstOrNull() ?: emptyList()
                    existingCommands.forEach { commandDao.deleteCommand(it) }
                    
                    backupData.commands.forEach { cmd ->
                        commandDao.insertCommand(
                            Command(triggerPhrase = cmd.triggerPhrase, entityId = cmd.entityId, domain = cmd.domain, service = cmd.service)
                        )
                    }
                }
            } catch (e: Exception) {
                _serviceError.value = "Restore failed: ${e.message}"
            }
        }
    }
}
