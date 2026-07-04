package com.example.albuddy.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.albuddy.data.model.STTEngineType
import com.example.albuddy.ui.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: MainViewModel, onNavigateToDictionary: () -> Unit, modifier: Modifier = Modifier) {
    val haUrl by viewModel.haUrl.collectAsState()
    val haToken by viewModel.haToken.collectAsState()
    val playMatchSound by viewModel.playMatchSound.collectAsState()
    val vibrateOnMatch by viewModel.vibrateOnMatch.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val activeEngine by viewModel.activeEngine.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val nativeSttLanguage by viewModel.nativeSttLanguage.collectAsState()
    val nativeSttWebSearch by viewModel.nativeSttWebSearch.collectAsState()
    val nativeSttSilenceLength by viewModel.nativeSttSilenceLength.collectAsState()
    val nativeSttPartialResults by viewModel.nativeSttPartialResults.collectAsState()
    val nativeSttMaxResults by viewModel.nativeSttMaxResults.collectAsState()

    var urlInput by remember(haUrl) { mutableStateOf(haUrl ?: "") }
    var tokenInput by remember(haToken) { mutableStateOf(haToken ?: "") }

    var isHaConfigExpanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    // Auto-test connection on typing delay
    LaunchedEffect(urlInput, tokenInput) {
        if (urlInput.isNotBlank() && tokenInput.isNotBlank()) {
            delay(1000)
            viewModel.updateHaUrl(urlInput.trim())
            viewModel.updateHaToken(tokenInput.trim())
            viewModel.testConnection()
        }
    }

    // Auto-download Vosk
    LaunchedEffect(activeEngine) {
        if (activeEngine == STTEngineType.VOSK) {
            viewModel.checkModelExists() // Refresh progress state
            delay(100) // allow state to update
            if (viewModel.downloadProgress.value == -1) {
                viewModel.downloadModel()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HA Configuration ---
        Card(
            modifier = Modifier.fillMaxWidth().clickable { isHaConfigExpanded = !isHaConfigExpanded },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Home, contentDescription = "Home Assistant", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Home Assistant API", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (connectionStatus != null) {
                            Icon(
                                imageVector = if (connectionStatus == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Connection Status",
                                tint = if (connectionStatus == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            imageVector = if (isHaConfigExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle HA Config"
                        )
                    }
                }
                
                AnimatedVisibility(visible = isHaConfigExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            label = { Text("HA URL (http://ip:8123)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("Long-Lived Access Token") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = "Toggle Password Visibility")
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- STT Engine Settings ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Speech-to-Text Engine", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setActiveEngine(STTEngineType.VOSK) }.padding(vertical = 4.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = activeEngine == STTEngineType.VOSK,
                        onClick = null // handled by row
                    )
                    Text("Vosk (Offline, Grammar Mode)")
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.setActiveEngine(STTEngineType.NATIVE_OFFLINE) }.padding(vertical = 4.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = activeEngine == STTEngineType.NATIVE_OFFLINE,
                        onClick = null // handled by row
                    )
                    Text("Native Android (Offline)")
                }

                if (activeEngine == STTEngineType.VOSK) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (downloadProgress >= 0) {
                        if (downloadProgress == 100) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Vosk Model ist installiert", color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            Text("Downloading Vosk Model...", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateToDictionary, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Dictionary")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vosk Dictionary verwalten")
                    }
                } else if (activeEngine == STTEngineType.NATIVE_OFFLINE) {
                    Spacer(modifier = Modifier.height(16.dp))
                    var showNativeSettingsDialog by remember { mutableStateOf(false) }
                    
                    Button(onClick = { showNativeSettingsDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Settings, contentDescription = "Native Settings")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Erweiterte STT Einstellungen")
                    }

                    if (showNativeSettingsDialog) {
                        AlertDialog(
                            onDismissRequest = { showNativeSettingsDialog = false },
                            title = { Text("Native STT Feineinstellungen") },
                            text = {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    var langExpanded by remember { mutableStateOf(false) }
                                    val languages = listOf("" to "System (Standard)", "de-DE" to "Deutsch", "en-US" to "Englisch")
                                    val currentLangName = languages.find { it.first == nativeSttLanguage }?.second ?: nativeSttLanguage
                                    
                                    Box {
                                        OutlinedTextField(
                                            value = currentLangName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Erkennungssprache") },
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = {
                                                Icon(Icons.Default.ArrowDropDown, "Select")
                                            }
                                        )
                                        androidx.compose.material3.Surface(
                                            modifier = Modifier.matchParentSize().clickable { langExpanded = true }, 
                                            color = androidx.compose.ui.graphics.Color.Transparent
                                        ) {}
                                        
                                        DropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                                            languages.forEach { (code, name) ->
                                                DropdownMenuItem(
                                                    text = { Text(name) },
                                                    onClick = { 
                                                        viewModel.updateNativeSttLanguage(code)
                                                        langExpanded = false 
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Switch(checked = nativeSttWebSearch, onCheckedChange = { viewModel.updateNativeSttWebSearch(it) })
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Web-Search Modell verwenden (statt Diktat)")
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Switch(checked = nativeSttPartialResults, onCheckedChange = { viewModel.updateNativeSttPartialResults(it) })
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Zwischenergebnisse aktivieren")
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Ruhe-Länge nach dem Sprechen: ${nativeSttSilenceLength}ms", style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = nativeSttSilenceLength.toFloat(),
                                        onValueChange = { viewModel.updateNativeSttSilenceLength(it.toLong()) },
                                        valueRange = 300f..3000f,
                                        steps = 26
                                    )
                                    
                                    var maxResultsText by remember(nativeSttMaxResults) { mutableStateOf(nativeSttMaxResults.toString()) }
                                    OutlinedTextField(
                                        value = maxResultsText,
                                        onValueChange = { 
                                            maxResultsText = it
                                            it.toIntOrNull()?.let { num ->
                                                if (num in 1..5) viewModel.updateNativeSttMaxResults(num)
                                            }
                                        },
                                        label = { Text("Maximale Ergebnisse (1-5)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Hinweis: Änderungen erfordern einen Neustart des Service (Stoppen & Starten). Einige Parameter (wie die Ruhe-Länge) werden von der Google-App oft ignoriert.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showNativeSettingsDialog = false }) {
                                    Text("Schließen")
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Feedback Settings ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Feedback", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Switch(checked = playMatchSound, onCheckedChange = { viewModel.updatePlaySound(it) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ton abspielen bei Erkennung")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Switch(checked = vibrateOnMatch, onCheckedChange = { viewModel.updateVibrate(it) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vibration bei Erkennung")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // --- Backup & Restore ---
        Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                    exportLauncher.launch("albuddy_backup_$dateStr.json")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Export")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Import")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
