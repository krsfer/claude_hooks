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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.data.MockDataProvider
import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
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
    var hookEvents by remember { mutableStateOf(MockDataProvider.generateMockHookEvents()) }
    var stats by remember { mutableStateOf(MockDataProvider.generateMockStats()) }
    var selectedFilters by remember { mutableStateOf(emptySet<HookType>()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Filter events based on selected types
    val filteredEvents = remember(selectedFilters, hookEvents) {
        if (selectedFilters.isEmpty()) {
            hookEvents
        } else {
            hookEvents.filter { it.type in selectedFilters }
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
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
                    IconButton(onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Settings coming soon!",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                        delay(1000) // Simulate refresh
                        hookEvents = MockDataProvider.generateMockHookEvents()
                        stats = MockDataProvider.generateMockStats()
                        isRefreshing = false
                        snackbarHostState.showSnackbar(
                            message = "Dashboard refreshed",
                            duration = SnackbarDuration.Short
                        )
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
                    key = { it.id }
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