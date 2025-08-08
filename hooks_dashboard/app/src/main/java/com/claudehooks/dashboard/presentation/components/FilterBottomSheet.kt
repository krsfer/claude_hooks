package com.claudehooks.dashboard.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilters: FilterCriteria,
    availableToolNames: List<String>,
    availableSessionIds: List<String>,
    onFiltersChanged: (FilterCriteria) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filters by remember { mutableStateOf(currentFilters) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Row {
                    TextButton(onClick = {
                        filters = FilterCriteria()
                    }) {
                        Text("Clear All")
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Session ID filter
                item {
                    SessionIdFilter(
                        selectedSessionId = filters.sessionId,
                        availableSessionIds = availableSessionIds,
                        onSessionIdChanged = { sessionId ->
                            filters = filters.copy(sessionId = sessionId)
                        }
                    )
                }
                
                // Hook Type filter
                item {
                    HookTypeFilter(
                        selectedTypes = filters.hookTypes,
                        onTypesChanged = { types ->
                            filters = filters.copy(hookTypes = types)
                        }
                    )
                }
                
                // Status filter
                item {
                    StatusFilter(
                        selectedStatuses = filters.statuses,
                        onStatusesChanged = { statuses ->
                            filters = filters.copy(statuses = statuses)
                        }
                    )
                }
                
                // Platform filter
                item {
                    PlatformFilter(
                        selectedPlatforms = filters.platforms,
                        onPlatformsChanged = { platforms ->
                            filters = filters.copy(platforms = platforms)
                        }
                    )
                }
                
                // Tool Name filter
                item {
                    ToolNameFilter(
                        selectedToolNames = filters.toolNames,
                        availableToolNames = availableToolNames,
                        onToolNamesChanged = { toolNames ->
                            filters = filters.copy(toolNames = toolNames)
                        }
                    )
                }
                
                // Time Range filter
                item {
                    TimeRangeFilter(
                        startTime = filters.startTime,
                        endTime = filters.endTime,
                        onTimeRangeChanged = { start, end ->
                            filters = filters.copy(startTime = start, endTime = end)
                        }
                    )
                }
                
                // Apply button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            onFiltersChanged(filters)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Filters")
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp)) // Bottom padding
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionIdFilter(
    selectedSessionId: String?,
    availableSessionIds: List<String>,
    onSessionIdChanged: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "Session ID",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedSessionId?.takeLast(8) ?: "All sessions",
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All sessions") },
                    onClick = {
                        onSessionIdChanged(null)
                        expanded = false
                    }
                )
                
                availableSessionIds.take(10).forEach { sessionId ->
                    DropdownMenuItem(
                        text = { Text(sessionId.takeLast(8)) },
                        onClick = {
                            onSessionIdChanged(sessionId)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HookTypeFilter(
    selectedTypes: Set<HookType>,
    onTypesChanged: (Set<HookType>) -> Unit
) {
    Column {
        Text(
            text = "Hook Types",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HookType.values().forEach { hookType ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedTypes.contains(hookType),
                        onClick = {
                            val newTypes = if (selectedTypes.contains(hookType)) {
                                selectedTypes - hookType
                            } else {
                                selectedTypes + hookType
                            }
                            onTypesChanged(newTypes)
                        },
                        role = Role.Checkbox
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedTypes.contains(hookType),
                    onCheckedChange = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = hookType.name.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatusFilter(
    selectedStatuses: Set<HookStatus>,
    onStatusesChanged: (Set<HookStatus>) -> Unit
) {
    Column {
        Text(
            text = "Status",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HookStatus.values().forEach { status ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedStatuses.contains(status),
                        onClick = {
                            val newStatuses = if (selectedStatuses.contains(status)) {
                                selectedStatuses - status
                            } else {
                                selectedStatuses + status
                            }
                            onStatusesChanged(newStatuses)
                        },
                        role = Role.Checkbox
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedStatuses.contains(status),
                    onCheckedChange = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PlatformFilter(
    selectedPlatforms: Set<Platform>,
    onPlatformsChanged: (Set<Platform>) -> Unit
) {
    Column {
        Text(
            text = "Platform",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Platform.values().forEach { platform ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedPlatforms.contains(platform),
                        onClick = {
                            val newPlatforms = if (selectedPlatforms.contains(platform)) {
                                selectedPlatforms - platform
                            } else {
                                selectedPlatforms + platform
                            }
                            onPlatformsChanged(newPlatforms)
                        },
                        role = Role.Checkbox
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedPlatforms.contains(platform),
                    onCheckedChange = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = platform.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolNameFilter(
    selectedToolNames: Set<String>,
    availableToolNames: List<String>,
    onToolNamesChanged: (Set<String>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(
            text = "Tool Names",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = if (selectedToolNames.isEmpty()) "All tools" 
                       else "${selectedToolNames.size} selected",
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableToolNames.forEach { toolName ->
                    DropdownMenuItem(
                        text = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedToolNames.contains(toolName),
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(toolName)
                            }
                        },
                        onClick = {
                            val newToolNames = if (selectedToolNames.contains(toolName)) {
                                selectedToolNames - toolName
                            } else {
                                selectedToolNames + toolName
                            }
                            onToolNamesChanged(newToolNames)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRangeFilter(
    startTime: Instant?,
    endTime: Instant?,
    onTimeRangeChanged: (Instant?, Instant?) -> Unit
) {
    Column {
        Text(
            text = "Time Range",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick time range buttons
            FilterChip(
                selected = isLastHour(startTime, endTime),
                onClick = {
                    val now = Clock.System.now()
                    onTimeRangeChanged(now.minus(1.hours), now)
                },
                label = { Text("Last Hour") },
                modifier = Modifier.weight(1f)
            )
            
            FilterChip(
                selected = isLast24Hours(startTime, endTime),
                onClick = {
                    val now = Clock.System.now()
                    onTimeRangeChanged(now.minus(1.days), now)
                },
                label = { Text("Last 24h") },
                modifier = Modifier.weight(1f)
            )
            
            FilterChip(
                selected = startTime == null && endTime == null,
                onClick = {
                    onTimeRangeChanged(null, null)
                },
                label = { Text("All Time") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun isLastHour(startTime: Instant?, endTime: Instant?): Boolean {
    if (startTime == null || endTime == null) return false
    val now = Clock.System.now()
    val oneHourAgo = now.minus(1.hours)
    return startTime >= oneHourAgo && endTime <= now.plus(1.hours) // Small tolerance
}

private fun isLast24Hours(startTime: Instant?, endTime: Instant?): Boolean {
    if (startTime == null || endTime == null) return false
    val now = Clock.System.now()
    val oneDayAgo = now.minus(1.days)
    return startTime >= oneDayAgo && endTime <= now.plus(1.hours) // Small tolerance
}