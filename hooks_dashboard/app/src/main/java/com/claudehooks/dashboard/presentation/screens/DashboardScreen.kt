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
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.repository.ConnectionStatus
import com.claudehooks.dashboard.presentation.components.FilterChips
import com.claudehooks.dashboard.presentation.components.HookEventCard
import com.claudehooks.dashboard.presentation.components.StatsCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var useTestData by remember { mutableStateOf(false) }
    val realRepository = remember { DataProvider.getRepository(context) }
    val testRepository = remember { DataProvider.createTestRepository(context) }
    
    var selectedFilters by remember { mutableStateOf(emptySet<HookType>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Choose repository based on mode
    val currentRepository = if (useTestData) null else realRepository
    
    // Observe data from appropriate repository
    val hookEvents by if (useTestData) {
        testRepository.getFilteredEvents(selectedFilters).collectAsState(initial = emptyList())
    } else {
        realRepository.getFilteredEvents(selectedFilters).collectAsState(initial = emptyList())
    }
    
    val stats by if (useTestData) {
        testRepository.getDashboardStats().collectAsState(initial = DashboardStats())
    } else {
        realRepository.getDashboardStats().collectAsState(initial = DashboardStats())
    }
    
    val connectionStatus by if (useTestData) {
        testRepository.connectionStatus.collectAsState()
    } else {
        realRepository.connectionStatus.collectAsState()
    }
    
    val isLoading by if (useTestData) {
        testRepository.isLoading.collectAsState()
    } else {
        realRepository.isLoading.collectAsState()
    }
    
    // Events are already filtered by the repository based on selectedFilters
    val filteredEvents = hookEvents
    
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Connection status indicator
                        when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Connected to Redis",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            ConnectionStatus.CONNECTING -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            ConnectionStatus.ERROR -> Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Connection Error",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            ConnectionStatus.DISCONNECTED -> Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Disconnected",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Claude Hooks Dashboard",
                            fontWeight = FontWeight.Bold
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
                                realRepository.reconnect()
                                snackbarHostState.showSnackbar(
                                    message = "Attempting to reconnect...",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reconnect")
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
                                realRepository.cleanOldEvents()
                                if (connectionStatus != ConnectionStatus.CONNECTED) {
                                    realRepository.reconnect()
                                }
                                snackbarHostState.showSnackbar(
                                    message = "Dashboard refreshed - showing latest events",
                                    duration = SnackbarDuration.Short
                                )
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
            
            // Events List
            if (filteredEvents.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(
                    items = filteredEvents,
                    key = { event -> event.id }
                ) { event ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        HookEventCard(
                            event = event,
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Event details: ${event.title}",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
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