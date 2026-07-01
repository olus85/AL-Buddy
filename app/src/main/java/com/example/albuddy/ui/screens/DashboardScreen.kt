package com.example.albuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.albuddy.ui.MainViewModel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.LaunchedEffect

@Composable
fun DashboardScreen(viewModel: MainViewModel, modifier: Modifier = Modifier, onNavigateToEdit: () -> Unit = {}) {
    val isRunning by viewModel.serviceRunning.collectAsState()
    val commands by viewModel.commands.collectAsState()
    val serviceError by viewModel.serviceError.collectAsState()
    val liveTranscription by viewModel.liveTranscription.collectAsState()
    val lastMatchedCommand by viewModel.lastMatchedCommand.collectAsState()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    LaunchedEffect(serviceError) {
        serviceError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearServiceError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
        Button(
            onClick = { viewModel.toggleService() },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                if (isRunning) "Stop Listening" else "Start Listening",
                style = MaterialTheme.typography.titleLarge
            )
        }
        
        if (isRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Live Transcription", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (liveTranscription.isBlank()) "Listening..." else liveTranscription,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    
                    lastMatchedCommand?.let {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Last Match:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Configured Commands", style = MaterialTheme.typography.titleMedium)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(commands) { cmd ->
                ListItem(
                    headlineContent = { Text("Say: \"${cmd.triggerPhrase}\"") },
                    supportingContent = { Text("Action: ${cmd.domain}.${cmd.service} -> ${cmd.entityId}") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { 
                                viewModel.editingCommand = cmd
                                onNavigateToEdit()
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteCommand(cmd) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                )
            }
            if (commands.isEmpty()) {
                item {
                    Text("No commands created yet.", modifier = Modifier.padding(16.dp))
                }
            }
        }
        }
    }
}
