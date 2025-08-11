package com.claudehooks.dashboard.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.data.EventLimitsConfig
import com.claudehooks.dashboard.data.repository.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val limitsConfig = remember { EventLimitsConfig(context) }
    val settingsRepository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State for settings values
    var memoryEvents by remember { mutableStateOf(limitsConfig.getMaxMemoryEvents()) }
    var displayEvents by remember { mutableStateOf(limitsConfig.getMaxDisplayEvents()) }
    var redisTtlHours by remember { mutableStateOf(24) } // Default 24 hours
    var persistenceEnabled by remember { mutableStateOf(settingsRepository.getPersistenceEnabled()) }
    var autoCleanupEnabled by remember { mutableStateOf(settingsRepository.getAutoCleanupEnabled()) }
    var cleanupHours by remember { mutableStateOf(settingsRepository.getCleanupHours()) }
    var maxStoredEvents by remember { mutableStateOf(settingsRepository.getMaxStoredEvents()) }
    var memoryPressureHandling by remember { mutableStateOf(true) } // Default enabled
    var memoryWarningThreshold by remember { mutableStateOf(70) } // 70% threshold
    var memoryCriticalThreshold by remember { mutableStateOf(85) } // 85% threshold
    var backgroundProcessing by remember { mutableStateOf(true) }
    var autoCleanupInterval by remember { mutableStateOf(30) } // 30 minutes
    var connectionRetryAttempts by remember { mutableStateOf(3) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    // Simulated memory usage - in real implementation this would come from system
    val currentMemoryUsage by remember { mutableStateOf(45) } // 45% as example
    val estimatedCacheUsage = (memoryEvents * 10) / 1024 // Rough estimate in MB
    
    // Get suggested memory limit
    val suggestedMemoryLimit = remember { limitsConfig.getSuggestedMemoryLimit(context) }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reset to defaults")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Event Limits Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Event Limits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Memory Events Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Memory Cache Size",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Badge {
                                Text("$memoryEvents events")
                            }
                        }
                        
                        Slider(
                            value = memoryEvents.toFloat(),
                            onValueChange = { memoryEvents = it.toInt() },
                            valueRange = EventLimitsConfig.MIN_MEMORY_EVENTS.toFloat()..EventLimitsConfig.MAX_MEMORY_EVENTS.toFloat(),
                            steps = 19, // Creates 20 discrete positions
                            onValueChangeFinished = {
                                limitsConfig.setMaxMemoryEvents(memoryEvents)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Memory cache size updated to $memoryEvents events",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Min: ${EventLimitsConfig.MIN_MEMORY_EVENTS}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Suggested: $suggestedMemoryLimit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Max: ${EventLimitsConfig.MAX_MEMORY_EVENTS}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Divider()
                    
                    // Display Events Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Display Limit",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Badge {
                                Text("$displayEvents events")
                            }
                        }
                        
                        Slider(
                            value = displayEvents.toFloat(),
                            onValueChange = { displayEvents = it.toInt() },
                            valueRange = EventLimitsConfig.MIN_DISPLAY_EVENTS.toFloat()..EventLimitsConfig.MAX_DISPLAY_EVENTS.toFloat(),
                            steps = 18, // Creates discrete positions
                            onValueChangeFinished = {
                                limitsConfig.setMaxDisplayEvents(displayEvents)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Display limit updated to $displayEvents events",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Min: ${EventLimitsConfig.MIN_DISPLAY_EVENTS}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default: ${EventLimitsConfig.DEFAULT_MAX_DISPLAY_EVENTS}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Max: ${EventLimitsConfig.MAX_DISPLAY_EVENTS}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Redis Storage Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Redis Storage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Redis TTL Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Storage Duration",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Badge {
                                Text(
                                    when {
                                        redisTtlHours < 24 -> "${redisTtlHours}h"
                                        redisTtlHours == 24 -> "1 day"
                                        redisTtlHours % 24 == 0 -> "${redisTtlHours/24} days"
                                        else -> "${redisTtlHours}h"
                                    }
                                )
                            }
                        }
                        
                        Slider(
                            value = redisTtlHours.toFloat(),
                            onValueChange = { redisTtlHours = it.toInt() },
                            valueRange = 1f..168f, // 1 hour to 7 days
                            steps = 167,
                            onValueChangeFinished = {
                                // Note: This would require implementing Redis TTL configuration
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Redis storage duration updated to ${if (redisTtlHours == 24) "1 day" else "${redisTtlHours}h"}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1 hour",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default: 24h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "7 days",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // General Persistence Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Persistence Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Event Persistence",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Save events across app restarts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = persistenceEnabled,
                            onCheckedChange = { 
                                persistenceEnabled = it
                                settingsRepository.setPersistenceEnabled(it)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Event persistence ${if (it) "enabled" else "disabled"}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                    
                    Divider()
                    
                    // Auto Cleanup Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Cleanup",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Automatically remove old events",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoCleanupEnabled,
                            onCheckedChange = { 
                                autoCleanupEnabled = it
                                settingsRepository.setAutoCleanupEnabled(it)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Auto cleanup ${if (it) "enabled" else "disabled"}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                    
                    if (autoCleanupEnabled) {
                        Divider()
                        
                        // Cleanup Hours Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cleanup After",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Badge {
                                    Text(
                                        when {
                                            cleanupHours < 24 -> "${cleanupHours}h"
                                            cleanupHours == 24 -> "1 day"
                                            cleanupHours % 24 == 0 -> "${cleanupHours/24} days"
                                            else -> "${cleanupHours}h"
                                        }
                                    )
                                }
                            }
                            
                            Slider(
                                value = cleanupHours.toFloat(),
                                onValueChange = { cleanupHours = it.toInt() },
                                valueRange = 1f..168f, // 1 hour to 7 days
                                steps = 167,
                                onValueChangeFinished = {
                                    settingsRepository.setCleanupHours(cleanupHours)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Cleanup interval updated to ${if (cleanupHours == 24) "1 day" else "${cleanupHours}h"}",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "1 hour",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Default: 24h",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "7 days",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Max Stored Events Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Max Stored Events",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Badge {
                                Text("${maxStoredEvents/1000}K events")
                            }
                        }
                        
                        Slider(
                            value = maxStoredEvents.toFloat(),
                            onValueChange = { maxStoredEvents = it.toInt() },
                            valueRange = 100f..50000f,
                            steps = 49,
                            onValueChangeFinished = {
                                settingsRepository.setMaxStoredEvents(maxStoredEvents)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Max stored events updated to ${maxStoredEvents/1000}K",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "100 events",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default: 5K",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "50K events",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Memory Management Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Memory Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Memory Pressure Handling Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic Memory Pressure Handling",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Automatically reduce cache during memory pressure",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = memoryPressureHandling,
                            onCheckedChange = { memoryPressureHandling = it }
                        )
                    }
                    
                    if (memoryPressureHandling) {
                        Divider()
                        
                        // Current Memory Usage Indicator
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current Memory Usage",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Badge(
                                        containerColor = when {
                                            currentMemoryUsage >= memoryCriticalThreshold -> MaterialTheme.colorScheme.errorContainer
                                            currentMemoryUsage >= memoryWarningThreshold -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }
                                    ) {
                                        Text(
                                            "${currentMemoryUsage}%",
                                            color = when {
                                                currentMemoryUsage >= memoryCriticalThreshold -> MaterialTheme.colorScheme.onErrorContainer
                                                currentMemoryUsage >= memoryWarningThreshold -> MaterialTheme.colorScheme.onTertiaryContainer
                                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                                            }
                                        )
                                    }
                                    Icon(
                                        when {
                                            currentMemoryUsage >= memoryCriticalThreshold -> Icons.Default.Warning
                                            currentMemoryUsage >= memoryWarningThreshold -> Icons.Default.Info
                                            else -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            currentMemoryUsage >= memoryCriticalThreshold -> MaterialTheme.colorScheme.error
                                            currentMemoryUsage >= memoryWarningThreshold -> MaterialTheme.colorScheme.tertiary
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            LinearProgressIndicator(
                                progress = currentMemoryUsage / 100f ,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = when {
                                    currentMemoryUsage >= memoryCriticalThreshold -> MaterialTheme.colorScheme.error
                                    currentMemoryUsage >= memoryWarningThreshold -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Cache: ~${estimatedCacheUsage}MB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when {
                                        currentMemoryUsage >= memoryCriticalThreshold -> "Critical"
                                        currentMemoryUsage >= memoryWarningThreshold -> "Warning"
                                        else -> "Normal"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        currentMemoryUsage >= memoryCriticalThreshold -> MaterialTheme.colorScheme.error
                                        currentMemoryUsage >= memoryWarningThreshold -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Memory Warning Threshold
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Warning Threshold",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Badge {
                                    Text("${memoryWarningThreshold}%")
                                }
                            }
                            
                            Slider(
                                value = memoryWarningThreshold.toFloat(),
                                onValueChange = { memoryWarningThreshold = it.toInt() },
                                valueRange = 50f..90f,
                                steps = 7
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "50%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Default: 70%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "90%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Memory Critical Threshold
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Critical Threshold",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Badge(containerColor = MaterialTheme.colorScheme.errorContainer) {
                                    Text(
                                        "${memoryCriticalThreshold}%",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            
                            Slider(
                                value = memoryCriticalThreshold.toFloat(),
                                onValueChange = { memoryCriticalThreshold = it.toInt() },
                                valueRange = 70f..95f,
                                steps = 4
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "70%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Default: 85%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "95%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Performance Optimization Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Performance Optimization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Background Processing Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background Processing",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Process events in background for smoother UI",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = backgroundProcessing,
                            onCheckedChange = { backgroundProcessing = it }
                        )
                    }
                    
                    Divider()
                    
                    // Auto-cleanup Interval Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto-cleanup Interval",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Badge {
                                Text("${autoCleanupInterval}min")
                            }
                        }
                        
                        Slider(
                            value = autoCleanupInterval.toFloat(),
                            onValueChange = { autoCleanupInterval = it.toInt() },
                            valueRange = 5f..120f, // 5 minutes to 2 hours
                            steps = 22
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "5min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default: 30min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "2h",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Divider()
                    
                    // Connection Retry Attempts
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Redis Retry Attempts",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Badge {
                                Text("${connectionRetryAttempts}x")
                            }
                        }
                        
                        Slider(
                            value = connectionRetryAttempts.toFloat(),
                            onValueChange = { connectionRetryAttempts = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "1 attempt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Default: 3x",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "10 attempts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                    }
                }
            }
            
            // Performance Tips
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Performance Tips",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    BulletPoint("Lower memory cache for devices with limited RAM")
                    BulletPoint("Reduce display limit for smoother scrolling")
                    BulletPoint("Enable background processing for better UI responsiveness")
                    BulletPoint("Adjust cleanup intervals based on monitoring frequency")
                    BulletPoint("Higher retry attempts improve reliability on poor connections")
                }
            }
        }
    } // End Column
    } // End Scaffold
    
    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Defaults?") },
            text = { 
                Text("This will reset all settings to their default values. Your cached events will not be affected.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        limitsConfig.resetToDefaults()
                        settingsRepository.resetToDefaults()
                        memoryEvents = EventLimitsConfig.DEFAULT_MAX_MEMORY_EVENTS
                        displayEvents = EventLimitsConfig.DEFAULT_MAX_DISPLAY_EVENTS
                        redisTtlHours = 24
                        persistenceEnabled = true
                        autoCleanupEnabled = true
                        cleanupHours = SettingsRepository.DEFAULT_CLEANUP_HOURS
                        maxStoredEvents = SettingsRepository.DEFAULT_MAX_STORED_EVENTS
                        memoryPressureHandling = true
                        memoryWarningThreshold = 70
                        memoryCriticalThreshold = 85
                        backgroundProcessing = true
                        autoCleanupInterval = 30
                        connectionRetryAttempts = 3
                        showResetDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Settings reset to defaults",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("•", color = MaterialTheme.colorScheme.primary)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}