package com.claudehooks.dashboard.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.claudehooks.dashboard.domain.model.HookData
import com.claudehooks.dashboard.domain.usecase.ExportHooksUseCase
import com.claudehooks.dashboard.presentation.components.*
import com.claudehooks.dashboard.presentation.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    var selectedHook by remember { mutableStateOf<HookData?>(null) }
    
    LaunchedEffect(uiState.filteredHooks) {
        // Auto-scroll to top when filters change
        if (listState.firstVisibleItemIndex > 5) {
            listState.animateScrollToItem(0)
        }
    }
    
    Scaffold(
        topBar = {
            DashboardTopBar(
                isConnected = uiState.isConnectedToRedis,
                connectionError = uiState.connectionError,
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = viewModel::updateSearchQuery,
                onRefresh = viewModel::refreshData,
                onRetryConnection = viewModel::retryConnection,
                onShowFilters = viewModel::toggleFilters,
                hasActiveFilters = !uiState.filters.isEmpty(),
                onExportJson = {
                    // Export would be handled here with proper context
                },
                onExportCsv = {
                    // Export would be handled here with proper context
                }
            )
        },
        floatingActionButton = {
            if (uiState.filteredHooks.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        // Scroll to top
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                !uiState.isConnectedToRedis -> {
                    ConnectionErrorState(
                        error = uiState.connectionError ?: "Not connected to Redis",
                        onRetry = viewModel::retryConnection,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                uiState.filteredHooks.isEmpty() -> {
                    EmptyState(
                        hasFilters = !uiState.filters.isEmpty(),
                        onClearFilters = viewModel::clearFilters,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stats overview
                        item {
                            StatsOverview(
                                stats = uiState.stats,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        // Hook type distribution
                        if (uiState.stats.hookTypeCounts.isNotEmpty()) {
                            item {
                                HookTypeDistributionCard(
                                    hookTypeCounts = uiState.stats.hookTypeCounts,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        
                        // Platform distribution
                        if (uiState.stats.platformCounts.isNotEmpty()) {
                            item {
                                PlatformDistributionCard(
                                    platformCounts = uiState.stats.platformCounts,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                        
                        // Hooks list header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Hooks (${uiState.filteredHooks.size})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                if (!uiState.filters.isEmpty()) {
                                    AssistChip(
                                        onClick = viewModel::clearFilters,
                                        label = { Text("Clear filters") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Hooks list
                        items(
                            items = uiState.filteredHooks,
                            key = { it.id }
                        ) { hook ->
                            HookCard(
                                hook = hook,
                                onClick = { 
                                    selectedHook = if (selectedHook?.id == hook.id) null else hook
                                },
                                isExpanded = selectedHook?.id == hook.id,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        
                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
            
            // Filter bottom sheet
            if (uiState.showFilters) {
                FilterBottomSheet(
                    currentFilters = uiState.filters,
                    availableToolNames = uiState.availableToolNames,
                    availableSessionIds = uiState.availableSessionIds,
                    onFiltersChanged = viewModel::updateFilters,
                    onDismiss = viewModel::toggleFilters
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    isConnected: Boolean,
    connectionError: String?,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetryConnection: () -> Unit,
    onShowFilters: () -> Unit,
    hasActiveFilters: Boolean,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search hooks...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column {
                    Text("Claude Hooks Dashboard")
                    if (connectionError != null) {
                        Text(
                            text = connectionError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = if (isConnected) "Connected" else "Disconnected",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        actions = {
            if (isSearchActive) {
                IconButton(onClick = { 
                    isSearchActive = false
                    onSearchQueryChanged("")
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close search")
                }
            } else {
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                
                IconButton(onClick = onShowFilters) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary
                               else LocalContentColor.current
                    )
                }
                
                Box {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export as JSON") },
                            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                            onClick = {
                                onExportJson()
                                showExportMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as CSV") },
                            leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) },
                            onClick = {
                                onExportCsv()
                                showExportMenu = false
                            }
                        )
                    }
                }
                
                if (isConnected) {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                } else {
                    IconButton(onClick = onRetryConnection) {
                        Icon(Icons.Default.Wifi, contentDescription = "Reconnect")
                    }
                }
            }
        }
    )
}

@Composable
private fun ConnectionErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Connection Failed",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry Connection")
        }
    }
}

@Composable
private fun EmptyState(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (hasFilters) Icons.Default.FilterList else Icons.Default.DataArray,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (hasFilters) "No matching hooks" else "No hooks yet",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (hasFilters) "Try adjusting your filters" 
                   else "Waiting for Claude Code activity...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (hasFilters) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onClearFilters) {
                Text("Clear Filters")
            }
        }
    }
}