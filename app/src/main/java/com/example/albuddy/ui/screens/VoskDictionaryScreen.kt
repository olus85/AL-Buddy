package com.example.albuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.albuddy.ui.MainViewModel

@Composable
fun VoskDictionaryScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val dictionaryItems by viewModel.dictionaryDisplayItems.collectAsState()
    var newWordInput by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Vosk Dictionary verwalten", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hinweis: Alle Trigger-Phrasen deiner Kommandos werden automatisch zum Dictionary hinzugefügt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newWordInput,
                onValueChange = { newWordInput = it },
                label = { Text("Neues Wort / Phrase (Kommagetrennt möglich)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newWordInput.isNotBlank()) {
                    viewModel.addVoskWord(newWordInput)
                    newWordInput = ""
                }
            }) {
                Text("Hinzufügen")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(dictionaryItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = if (item.isCommand) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) else CardDefaults.cardColors()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(item.word, style = MaterialTheme.typography.bodyLarge)
                            if (item.isCommand) {
                                Text("Kommando (Automatisch)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (!item.isCommand && item.voskWord != null) {
                            IconButton(onClick = { viewModel.removeVoskWord(item.voskWord) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
