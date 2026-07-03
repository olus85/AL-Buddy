package com.example.albuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.albuddy.ui.MainViewModel
import com.example.albuddy.ui.screens.CommandCreatorScreen
import com.example.albuddy.ui.screens.DashboardScreen
import com.example.albuddy.ui.screens.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission granted or denied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            ALBuddyApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ALBuddyApp() {
    var currentScreen by remember { mutableStateOf("dashboard") }
    val viewModel: MainViewModel = hiltViewModel()

    BackHandler(enabled = currentScreen != "dashboard") {
        currentScreen = if (currentScreen == "vosk_dictionary") "settings" else "dashboard"
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AL Buddy") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentScreen == "dashboard",
                    onClick = { currentScreen = "dashboard" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Command") },
                    label = { Text("Add Command") },
                    selected = currentScreen == "add_command",
                    onClick = { 
                        viewModel.editingCommand = null
                        currentScreen = "add_command" 
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentScreen == "settings",
                    onClick = { currentScreen = "settings" }
                )
            }
        }
    ) { innerPadding ->
        Modifier.padding(innerPadding).let {
            when (currentScreen) {
                "dashboard" -> DashboardScreen(viewModel = viewModel, modifier = it, onNavigateToEdit = { currentScreen = "add_command" })
                "add_command" -> CommandCreatorScreen(viewModel = viewModel, modifier = it, onBack = { currentScreen = "dashboard" })
                "settings" -> SettingsScreen(viewModel = viewModel, onNavigateToDictionary = { currentScreen = "vosk_dictionary" }, modifier = it)
                "vosk_dictionary" -> com.example.albuddy.ui.screens.VoskDictionaryScreen(viewModel = viewModel, modifier = it)
            }
        }
    }
}
