package com.example.albuddy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.albuddy.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandCreatorScreen(viewModel: MainViewModel, modifier: Modifier = Modifier, onBack: () -> Unit) {
    val entities by viewModel.entities.collectAsState()
    val services by viewModel.services.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchEntities()
    }

    val editingCommand = viewModel.editingCommand

    var phrase by remember(editingCommand) { mutableStateOf(editingCommand?.triggerPhrase ?: "") }
    var entityId by remember(editingCommand) { mutableStateOf(editingCommand?.entityId ?: "") }
    var domain by remember(editingCommand) { mutableStateOf(editingCommand?.domain ?: "") }
    var service by remember(editingCommand) { mutableStateOf(editingCommand?.service ?: "") }

    var expandedEntity by remember { mutableStateOf(false) }
    var expandedService by remember { mutableStateOf(false) }

    var entitySearchQuery by remember(editingCommand) { mutableStateOf(editingCommand?.entityId ?: "") }
    var debouncedQuery by remember(editingCommand) { mutableStateOf(editingCommand?.entityId ?: "") }

    LaunchedEffect(entitySearchQuery) {
        kotlinx.coroutines.delay(300)
        debouncedQuery = entitySearchQuery
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(if (editingCommand != null) "Edit Command" else "Create New Command", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phrase,
            onValueChange = { phrase = it },
            label = { Text("Trigger Phrases (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expandedEntity,
            onExpandedChange = { expandedEntity = !expandedEntity },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = entitySearchQuery,
                onValueChange = { 
                    entitySearchQuery = it
                    entityId = it
                    expandedEntity = true
                },
                label = { Text("Entity ID") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEntity) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expandedEntity,
                onDismissRequest = { expandedEntity = false }
            ) {
                // Perform quick list filtering using the debounced query
                val filteredEntities = remember(debouncedQuery, entities) {
                    if (debouncedQuery.isBlank()) emptyList()
                    else entities.asSequence().filter {
                        it.entity_id.contains(debouncedQuery, ignoreCase = true) ||
                        it.attributes?.friendly_name?.contains(debouncedQuery, ignoreCase = true) == true
                    }.take(50).toList()
                }

                filteredEntities.forEach { entity ->
                    val displayName = if (entity.attributes?.friendly_name != null) "${entity.attributes.friendly_name} (${entity.entity_id})" else entity.entity_id
                    DropdownMenuItem(
                        text = { Text(displayName) },
                        onClick = {
                            entityId = entity.entity_id
                            entitySearchQuery = entity.entity_id
                            debouncedQuery = entity.entity_id
                            domain = entity.entity_id.substringBefore(".")
                            expandedEntity = false
                            if (domain == "script" || domain == "scene") {
                                service = "turn_on"
                            } else {
                                service = ""
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Read-only domain display
        OutlinedTextField(
            value = domain,
            onValueChange = { },
            label = { Text("Domain") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (domain != "script" && domain != "scene") {
            ExposedDropdownMenuBox(
                expanded = expandedService,
                onExpandedChange = { expandedService = !expandedService },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = service,
                    onValueChange = { 
                        service = it 
                        expandedService = true
                    },
                    label = { Text("Service") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedService) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedService,
                    onDismissRequest = { expandedService = false }
                ) {
                    val availableServices = services.find { it.domain == domain }?.services?.keys?.toList() ?: emptyList()
                    val filteredServices = availableServices.filter { it.contains(service, ignoreCase = true) }
                    filteredServices.forEach { srv ->
                        DropdownMenuItem(
                            text = { Text(srv) },
                            onClick = {
                                service = srv
                                expandedService = false
                            }
                        )
                    }
                }
            }
        } else {
            Text("Service is automatically set to 'turn_on' for $domain.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (phrase.isNotBlank() && entityId.isNotBlank() && domain.isNotBlank() && service.isNotBlank()) {
                    if (editingCommand != null) {
                        viewModel.updateCommand(editingCommand.copy(triggerPhrase = phrase, entityId = entityId, domain = domain, service = service))
                        viewModel.editingCommand = null
                    } else {
                        viewModel.addCommand(phrase, entityId, domain, service)
                    }
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (editingCommand != null) "Update Command" else "Save Command")
        }
    }
}
