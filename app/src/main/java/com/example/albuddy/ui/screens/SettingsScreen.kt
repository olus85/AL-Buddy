package com.example.albuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.albuddy.data.model.STTEngineType
import com.example.albuddy.ui.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val haUrl by viewModel.haUrl.collectAsState()
    val haToken by viewModel.haToken.collectAsState()
    var urlInput by remember(haUrl) { mutableStateOf(haUrl ?: "") }
    var tokenInput by remember(haToken) { mutableStateOf(haToken ?: "") }
    val playMatchSound by viewModel.playMatchSound.collectAsState()
    val vibrateOnMatch by viewModel.vibrateOnMatch.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportBackup(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    var playSoundInput by remember(playMatchSound) { mutableStateOf(playMatchSound) }
    var vibrateInput by remember(vibrateOnMatch) { mutableStateOf(vibrateOnMatch) }

    var testResult by remember { mutableStateOf<Boolean?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Home Assistant API", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("HA URL (http://ip:8123)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = { Text("Long-Lived Access Token") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { 
            viewModel.saveSettings(urlInput, tokenInput, playSoundInput, vibrateInput)
            viewModel.testConnection { success -> testResult = success }
        }) {
            Text("Save & Test Connection")
        }

        if (testResult != null) {
            Text(
                text = if (testResult == true) "Connection Successful!" else "Connection Failed.",
                color = if (testResult == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val downloadProgress by viewModel.downloadProgress.collectAsState()
        
        Button(onClick = { viewModel.downloadModel() }, modifier = Modifier.fillMaxWidth()) {
            Text("Download STT Model (Vosk)")
        }
        
        if (downloadProgress >= 0) {
            Spacer(modifier = Modifier.height(8.dp))
            if (downloadProgress == 100) {
                Text("Model downloaded successfully!", color = MaterialTheme.colorScheme.primary)
            } else {
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("$downloadProgress% downloaded", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Feedback", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Switch(checked = playSoundInput, onCheckedChange = { 
                playSoundInput = it
                viewModel.saveSettings(urlInput, tokenInput, it, vibrateInput)
            })
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play Sound on Match")
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Switch(checked = vibrateInput, onCheckedChange = { 
                vibrateInput = it
                viewModel.saveSettings(urlInput, tokenInput, playSoundInput, it)
            })
            Spacer(modifier = Modifier.width(8.dp))
            Text("Vibrate on Match")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.saveSettings(urlInput, tokenInput, playSoundInput, vibrateInput) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save Settings")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Button(
                onClick = {
                    val dateStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                    exportLauncher.launch("backup$dateStr.json")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Backup (Export)")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Restore (Import)")
            }
        }
    }
}
