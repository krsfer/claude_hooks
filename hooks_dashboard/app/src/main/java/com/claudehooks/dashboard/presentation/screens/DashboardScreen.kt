package com.claudehooks.dashboard.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.data.DataProvider
import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.repository.BackgroundServiceRepository
import com.claudehooks.dashboard.service.ConnectionStatus
import com.claudehooks.dashboard.data.repository.ConnectionStatus as LegacyConnectionStatus
import com.claudehooks.dashboard.presentation.components.FilterChips
import com.claudehooks.dashboard.presentation.components.HookEventCard
import com.claudehooks.dashboard.presentation.components.StatsCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: BackgroundServiceRepository? = null,
    onQuitRequested: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var useTestData by remember { mutableStateOf(false) }
    val fallbackRepository = remember { DataProvider.getRepository(context) }
    val testRepository = remember { DataProvider.createTestRepository(context) }
    
    var selectedFilters by remember { mutableStateOf(emptySet<HookType>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Use provided BackgroundServiceRepository or fall back to legacy repository
    val useBackgroundService = repository != null
    val currentRepository = if (useTestData) null else (repository ?: fallbackRepository)
    
    // Observe data from appropriate repository
    val hookEvents by if (useTestData) {
        testRepository.getFilteredEvents(selectedFilters).collectAsState(initial = emptyList())
    } else if (useBackgroundService) {
        repository!!.getFilteredEvents(selectedFilters).collectAsState(initial = emptyList())
    } else {
        fallbackRepository.getFilteredEvents(selectedFilters).collectAsState(initial = emptyList())
    }
    
    val stats by if (useTestData) {
        testRepository.getDashboardStats().collectAsState(initial = DashboardStats())
    } else if (useBackgroundService) {
        repository!!.dashboardStats.collectAsState(initial = DashboardStats())
    } else {
        fallbackRepository.getDashboardStats().collectAsState(initial = DashboardStats())
    }
    
    val connectionStatus by if (useTestData) {
        // Convert test repository ConnectionStatus to service ConnectionStatus
        testRepository.connectionStatus.map { legacyStatus ->
            when (legacyStatus) {
                LegacyConnectionStatus.DISCONNECTED -> ConnectionStatus.DISCONNECTED
                LegacyConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING 
                LegacyConnectionStatus.CONNECTED -> ConnectionStatus.CONNECTED
                LegacyConnectionStatus.ERROR -> ConnectionStatus.ERROR
            }
        }.collectAsState(initial = ConnectionStatus.DISCONNECTED)
    } else if (useBackgroundService) {
        repository!!.connectionStatus.collectAsState()
    } else {
        // Convert legacy ConnectionStatus to service ConnectionStatus
        fallbackRepository.connectionStatus.map { legacyStatus ->
            when (legacyStatus) {
                LegacyConnectionStatus.DISCONNECTED -> ConnectionStatus.DISCONNECTED
                LegacyConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING 
                LegacyConnectionStatus.CONNECTED -> ConnectionStatus.CONNECTED
                LegacyConnectionStatus.ERROR -> ConnectionStatus.ERROR
            }
        }.collectAsState(initial = ConnectionStatus.DISCONNECTED)
    }
    
    // For background service, we don't have isLoading - service is always running
    val isLoading by if (useTestData) {
        testRepository.isLoading.collectAsState()
    } else if (useBackgroundService) {
        remember { mutableStateOf(false) }
    } else {
        fallbackRepository.isLoading.collectAsState()
    }
    
    // Events are already filtered by the repository based on selectedFilters
    val filteredEvents = hookEvents
    
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Claude Hooks Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    // Connection status indicator moved to navigation icon position
                    when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Connected to Redis",
                            modifier = Modifier.size(20.dp).padding(start = 16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        ConnectionStatus.CONNECTING -> CircularProgressIndicator(
                            modifier = Modifier.size(16.dp).padding(start = 16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ConnectionStatus.ERROR -> Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Connection Error",
                            modifier = Modifier.size(20.dp).padding(start = 16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        ConnectionStatus.DISCONNECTED -> Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Disconnected",
                            modifier = Modifier.size(20.dp).padding(start = 16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Toggle test data button
                    IconButton(onClick = {
                        useTestData = !useTestData
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = if (useTestData) "Using simulated test data" else "Using live Redis data",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }) {
                        Icon(
                            if (useTestData) Icons.Default.BugReport else Icons.Default.CloudDone, 
                            contentDescription = if (useTestData) "Switch to Live Data" else "Switch to Test Data",
                            tint = if (useTestData) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Reconnect button for error states
                    if (!useTestData && connectionStatus == ConnectionStatus.ERROR) {
                        IconButton(onClick = {
                            scope.launch {
                                if (useBackgroundService) {
                                    repository!!.reconnect()
                                } else {
                                    fallbackRepository.reconnect()
                                }
                                snackbarHostState.showSnackbar(
                                    message = "Attempting to reconnect...",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reconnect")
                        }
                    }
                    
                    // Quit button for background service mode
                    if (useBackgroundService && onQuitRequested != null) {
                        IconButton(onClick = onQuitRequested) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Quit App",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    IconButton(onClick = {
                        scope.launch {
                            val statusMessage = if (useTestData) {
                                "Test mode: Using simulated data"
                            } else {
                                when (connectionStatus) {
                                    ConnectionStatus.CONNECTED -> "Connected to Redis successfully"
                                    ConnectionStatus.CONNECTING -> "Connecting to Redis..."
                                    ConnectionStatus.ERROR -> "Connection error - switch to test mode or reconnect"
                                    ConnectionStatus.DISCONNECTED -> "Disconnected from Redis"
                                }
                            }
                            snackbarHostState.showSnackbar(
                                message = statusMessage,
                                duration = SnackbarDuration.Long
                            )
                        }
                    }) {
                        Icon(Icons.Default.Info, contentDescription = "Connection Info")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        isRefreshing = true
                        try {
                            if (useTestData) {
                                // Clean old events from test repository
                                testRepository.cleanOldEvents()
                                snackbarHostState.showSnackbar(
                                    message = "Test data refreshed - cleaned old events",
                                    duration = SnackbarDuration.Short
                                )
                            } else {
                                // Clean old events and attempt reconnect if disconnected
                                if (useBackgroundService) {
                                    // For background service, just attempt reconnect if needed
                                    if (connectionStatus != ConnectionStatus.CONNECTED) {
                                        repository!!.reconnect()
                                    }
                                    snackbarHostState.showSnackbar(
                                        message = "Background service refreshed",
                                        duration = SnackbarDuration.Short
                                    )
                                } else {
                                    fallbackRepository.cleanOldEvents()
                                    if (connectionStatus != ConnectionStatus.CONNECTED) {
                                        fallbackRepository.reconnect()
                                    }
                                    snackbarHostState.showSnackbar(
                                        message = "Dashboard refreshed - showing latest events",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                            
                            delay(500) // Give some time for operations
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Refresh failed: ${e.message}",
                                duration = SnackbarDuration.Short
                            )
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                icon = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                text = { Text(if (isRefreshing) "Refreshing..." else "Refresh") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statistics Section
            item {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            item {
                StatsSection(stats = stats)
            }
            
            // Filter Chips
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Events",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = filteredEvents.size.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FilterChips(
                        selectedTypes = selectedFilters,
                        onTypeToggle = { type ->
                            selectedFilters = if (type in selectedFilters) {
                                selectedFilters - type
                            } else {
                                selectedFilters + type
                            }
                        },
                        onClearAll = { selectedFilters = emptySet() }
                    )
                }
            }
            
            // Events List with Time Grouping
            if (filteredEvents.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                val eventGroups = groupEventsByTime(filteredEvents)
                
                // Generate all items for LazyColumn
                val allItems = mutableListOf<Any>()
                eventGroups.forEach { group ->
                    allItems.add("header_${group.timeLabel}")
                    allItems.addAll(group.events)
                    allItems.add("spacer_${group.timeLabel}")
                }
                
                items(
                    items = allItems,
                    key = { item ->
                        when (item) {
                            is String -> item // Headers and spacers
                            is HookEvent -> item.id
                            else -> item.hashCode()
                        }
                    }
                ) { item ->
                    when (item) {
                        is String -> {
                            when {
                                item.startsWith("header_") -> {
                                    val groupLabel = item.removePrefix("header_")
                                    val group = eventGroups.find { it.timeLabel == groupLabel }
                                    if (group != null) {
                                        TimeGroupHeader(
                                            label = group.timeLabel,
                                            eventCount = group.events.size
                                        )
                                    }
                                }
                                item.startsWith("spacer_") -> {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                        is HookEvent -> {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                HookEventCard(
                                    event = item,
                                    onClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Event details: ${item.title}",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun StatsSection(stats: DashboardStats) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatsCard(
                title = "Total Events",
                value = stats.totalEvents.toString(),
                subtitle = "Last 24 hours",
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(150.dp)
            )
        }
        item {
            StatsCard(
                title = "Critical",
                value = stats.criticalCount.toString(),
                subtitle = "Requires attention",
                valueColor = MaterialTheme.colorScheme.error,
                cardColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                modifier = Modifier.width(150.dp)
            )
        }
        item {
            StatsCard(
                title = "Warnings",
                value = stats.warningCount.toString(),
                subtitle = "Monitor closely",
                valueColor = Color(0xFFFF9800),
                cardColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.width(150.dp)
            )
        }
        item {
            StatsCard(
                title = "Success Rate",
                value = String.format("%.1f%%", stats.successRate),
                subtitle = "System health",
                valueColor = Color(0xFF4CAF50),
                modifier = Modifier.width(150.dp)
            )
        }
        item {
            StatsCard(
                title = "Active Hooks",
                value = stats.activeHooks.toString(),
                subtitle = "Currently monitoring",
                valueColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.width(150.dp)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FilterAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No events found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Try adjusting your filters or refresh the dashboard",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Data class for grouped events with time period headers
 */
data class EventGroup(
    val timeLabel: String,
    val events: List<HookEvent>
)

/**
 * Group events by time periods for better organization
 */
private fun groupEventsByTime(events: List<HookEvent>): List<EventGroup> {
    if (events.isEmpty()) return emptyList()
    
    val now = Instant.now()
    val groups = mutableListOf<EventGroup>()
    
    // Group events by time periods
    val justNow = mutableListOf<HookEvent>()
    val lastHour = mutableListOf<HookEvent>()
    val today = mutableListOf<HookEvent>()
    val yesterday = mutableListOf<HookEvent>()
    val thisWeek = mutableListOf<HookEvent>()
    val older = mutableListOf<HookEvent>()
    
    for (event in events) {
        val secondsAgo = ChronoUnit.SECONDS.between(event.timestamp, now)
        val hoursAgo = ChronoUnit.HOURS.between(event.timestamp, now)
        val daysAgo = ChronoUnit.DAYS.between(event.timestamp, now)
        
        when {
            secondsAgo < 300 -> justNow.add(event) // Last 5 minutes
            secondsAgo < 3600 -> lastHour.add(event) // Last hour
            daysAgo == 0L -> today.add(event) // Today
            daysAgo == 1L -> yesterday.add(event) // Yesterday
            daysAgo <= 7 -> thisWeek.add(event) // This week
            else -> older.add(event) // Older
        }
    }
    
    // Add non-empty groups
    if (justNow.isNotEmpty()) groups.add(EventGroup("Just now", justNow))
    if (lastHour.isNotEmpty()) groups.add(EventGroup("Last hour", lastHour))
    if (today.isNotEmpty()) groups.add(EventGroup("Earlier today", today))
    if (yesterday.isNotEmpty()) groups.add(EventGroup("Yesterday", yesterday))
    if (thisWeek.isNotEmpty()) groups.add(EventGroup("This week", thisWeek))
    if (older.isNotEmpty()) groups.add(EventGroup("Older", older))
    
    return groups
}

/**
 * Time group header composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeGroupHeader(
    label: String,
    eventCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Badge(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Text(
                text = eventCount.toString(),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}