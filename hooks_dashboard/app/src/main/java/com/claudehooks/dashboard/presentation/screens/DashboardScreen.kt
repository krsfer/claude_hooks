package com.claudehooks.dashboard.presentation.screens

import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.claudehooks.dashboard.data.DataProvider
import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity
import com.claudehooks.dashboard.data.repository.BackgroundServiceRepository
import com.claudehooks.dashboard.service.ConnectionStatus
import com.claudehooks.dashboard.data.repository.ConnectionStatus as LegacyConnectionStatus
import com.claudehooks.dashboard.presentation.components.AdvancedFilterBar
import com.claudehooks.dashboard.presentation.components.FilterChips
import com.claudehooks.dashboard.presentation.components.FilterPreset
import com.claudehooks.dashboard.presentation.components.HookEventCard
import com.claudehooks.dashboard.presentation.components.StatsCard
import com.claudehooks.dashboard.data.model.FilterState
import com.claudehooks.dashboard.presentation.util.applyFilters
import com.claudehooks.dashboard.presentation.util.extractAvailableSessions
import com.claudehooks.dashboard.presentation.util.getFilterStats
import com.claudehooks.dashboard.presentation.components.DataFreshnessIndicator
import com.claudehooks.dashboard.presentation.components.DataFreshnessBanner
import com.claudehooks.dashboard.presentation.components.AutoReconnectionIndicator
import com.claudehooks.dashboard.presentation.components.ReconnectionState
import com.claudehooks.dashboard.presentation.components.rememberAutoReconnectionState
import com.claudehooks.dashboard.presentation.components.CutoutAwareToolbar
import com.claudehooks.dashboard.presentation.util.performReconnection
import com.claudehooks.dashboard.presentation.components.PerformanceMetricsCard
import com.claudehooks.dashboard.presentation.components.PerformanceTrendChart
import com.claudehooks.dashboard.presentation.components.TrendChartType
import com.claudehooks.dashboard.data.model.PerformanceMetrics
import com.claudehooks.dashboard.presentation.components.ExportDialog
import com.claudehooks.dashboard.presentation.components.ShareBottomSheet
import com.claudehooks.dashboard.service.ExportService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    repository: BackgroundServiceRepository? = null,
    onQuitRequested: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var useTestData by remember { mutableStateOf(false) }
    val fallbackRepository = remember { DataProvider.getRepository(context) }
    val testRepository = remember { DataProvider.createTestRepository(context) }
    
    var filterState by remember { mutableStateOf(FilterState()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Auto-reconnection state management
    val autoReconnectionState = rememberAutoReconnectionState()
    
    // Performance visualization state
    var showPerformanceMetrics by remember { mutableStateOf(false) }
    var performanceMetricsExpanded by remember { mutableStateOf(false) }
    var selectedTrendChart by remember { mutableStateOf(TrendChartType.HEALTH_SCORE) }
    var trendChartExpanded by remember { mutableStateOf(false) }
    
    // Track if app is in foreground
    var isAppInForeground by remember { mutableStateOf(true) }
    
    // Observe lifecycle to pause/resume monitoring
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isAppInForeground = true
                    android.util.Log.d("DashboardScreen", "ON_RESUME - App came to foreground, showPerformanceMetrics=$showPerformanceMetrics, useTestData=$useTestData")
                    // Resume monitoring if it should be running
                    if (showPerformanceMetrics && !useTestData) {
                        if (repository != null) {
                            repository.startPerformanceMonitoring()
                            android.util.Log.d("DashboardScreen", "Started performance monitoring (repository)")
                        } else {
                            fallbackRepository.startPerformanceMonitoring()
                            android.util.Log.d("DashboardScreen", "Started performance monitoring (fallback)")
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isAppInForeground = false
                    android.util.Log.d("DashboardScreen", "ON_PAUSE - App went to background, stopping performance monitoring")
                    // Always pause monitoring when backgrounded
                    if (repository != null) {
                        repository.stopPerformanceMonitoring()
                        android.util.Log.d("DashboardScreen", "Stopped performance monitoring (repository)")
                    } else {
                        fallbackRepository.stopPerformanceMonitoring()
                        android.util.Log.d("DashboardScreen", "Stopped performance monitoring (fallback)")
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Control performance monitoring based on toggle AND foreground state
    LaunchedEffect(showPerformanceMetrics, isAppInForeground) {
        android.util.Log.d("DashboardScreen", "LaunchedEffect triggered: showPerformanceMetrics=$showPerformanceMetrics, isAppInForeground=$isAppInForeground, useTestData=$useTestData")
        if (!useTestData) {
            if (showPerformanceMetrics && isAppInForeground) {
                // Start performance monitoring when metrics are shown AND app is in foreground
                android.util.Log.d("DashboardScreen", "LaunchedEffect: Starting monitoring")
                if (repository != null) {
                    repository.startPerformanceMonitoring()
                } else {
                    fallbackRepository.startPerformanceMonitoring()
                }
            } else {
                // Stop performance monitoring when metrics are hidden OR app is backgrounded
                android.util.Log.d("DashboardScreen", "LaunchedEffect: Stopping monitoring")
                if (repository != null) {
                    repository.stopPerformanceMonitoring()
                } else {
                    fallbackRepository.stopPerformanceMonitoring()
                }
            }
        }
    }
    
    // Export functionality state
    var showExportDialog by remember { mutableStateOf(false) }
    var showShareBottomSheet by remember { mutableStateOf(false) }
    var selectedEventForShare by remember { mutableStateOf<HookEvent?>(null) }
    
    // Use provided BackgroundServiceRepository or fall back to legacy repository
    val useBackgroundService = repository != null
    
    // Observe data from appropriate repository
    val allHookEvents by if (useTestData) {
        testRepository.getFilteredEvents(emptySet()).collectAsState(initial = emptyList())
    } else if (useBackgroundService) {
        repository!!.getFilteredEvents(emptySet()).collectAsState(initial = emptyList())
    } else {
        fallbackRepository.getFilteredEvents(emptySet()).collectAsState(initial = emptyList())
    }
    
    // Apply advanced filtering
    val hookEvents = remember(allHookEvents, filterState) {
        allHookEvents.applyFilters(filterState)
    }
    
    // Extract available sessions for filtering
    val availableSessions = remember(allHookEvents) {
        allHookEvents.extractAvailableSessions()
    }
    
    // Get filter statistics
    val filterStats = remember(allHookEvents, filterState) {
        allHookEvents.getFilterStats(filterState)
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
    
    // Get last update time for data freshness tracking
    val lastUpdateTime by if (useTestData) {
        testRepository.lastUpdateTime.collectAsState()
    } else if (useBackgroundService) {
        repository!!.lastUpdateTime.collectAsState()
    } else {
        fallbackRepository.lastUpdateTime.collectAsState()
    }
    
    // Get performance monitoring data
    val performanceMonitor = remember(useTestData, useBackgroundService) {
        if (useTestData) {
            testRepository.getPerformanceMonitor()
        } else if (useBackgroundService) {
            repository?.getPerformanceMonitor()
        } else {
            fallbackRepository.getPerformanceMonitor()
        }
    }
    
    // Observe performance metrics
    val currentPerformanceMetrics by performanceMonitor?.currentMetrics?.collectAsState() 
        ?: remember { mutableStateOf(PerformanceMetrics()) }
    
    val performanceHistory by performanceMonitor?.performanceHistory?.collectAsState() 
        ?: remember { mutableStateOf(com.claudehooks.dashboard.data.model.PerformanceHistory()) }
    
    // Loading state is managed by background service or repositories internally
    
    // Events are filtered by advanced filter system
    val filteredEvents = hookEvents
    
    // Cutout information is handled inside CutoutAwareToolbar
    
    Scaffold(
        modifier = modifier
            .fillMaxSize(),
        // Remove windowInsetsPadding to allow CutoutAwareToolbar to position icons above cutout
        topBar = {
            CutoutAwareToolbar(
                connectionStatus = connectionStatus,
                lastUpdateTime = lastUpdateTime,
                useTestData = useTestData,
                onTestDataToggle = {
                    useTestData = !useTestData
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = if (useTestData) "Using simulated test data" else "Using live Redis data",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onReconnect = {
                    performReconnection(
                        autoReconnectionState = autoReconnectionState,
                        useBackgroundService = useBackgroundService,
                        repository = repository,
                        fallbackRepository = fallbackRepository,
                        scope = scope,
                        snackbarHostState = snackbarHostState
                    )
                },
                onPerformanceToggle = {
                    showPerformanceMetrics = !showPerformanceMetrics
                },
                showPerformanceMetrics = showPerformanceMetrics,
                onExport = {
                    showExportDialog = true
                },
                onSettings = {
                    onNavigateToSettings?.invoke()
                },
                onInfo = {
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
                },
                onQuitRequested = onQuitRequested,
                scope = scope,
                snackbarHostState = snackbarHostState
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
        val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp + navigationBarsPadding.calculateBottomPadding()
            ),
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
            
            // Auto-reconnection indicator
            item {
                AutoReconnectionIndicator(
                    reconnectionState = autoReconnectionState.reconnectionState.value,
                    attemptCount = autoReconnectionState.attemptCount.value,
                    maxAttempts = autoReconnectionState.maxAttempts.value,
                    onDismiss = { autoReconnectionState.reset() }
                )
            }
            
            // Data freshness banner for stale data warnings
            item {
                DataFreshnessBanner(
                    lastUpdate = lastUpdateTime,
                    connectionStatus = connectionStatus,
                    onReconnectClick = {
                        performReconnection(
                            autoReconnectionState = autoReconnectionState,
                            useBackgroundService = useBackgroundService,
                            repository = repository,
                            fallbackRepository = fallbackRepository,
                            scope = scope,
                            snackbarHostState = snackbarHostState
                        )
                    }
                )
            }
            
            // Performance Visualization Section
            if (showPerformanceMetrics) {
                item {
                    PerformanceMetricsCard(
                        metrics = currentPerformanceMetrics,
                        expanded = performanceMetricsExpanded,
                        onToggleExpanded = { performanceMetricsExpanded = !performanceMetricsExpanded }
                    )
                }
                
                item {
                    PerformanceTrendChart(
                        history = performanceHistory,
                        chartType = selectedTrendChart,
                        expanded = trendChartExpanded,
                        onToggleExpanded = { trendChartExpanded = !trendChartExpanded }
                    )
                }
                
                // Chart type selector when expanded
                if (trendChartExpanded) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TrendChartType.values().forEach { chartType ->
                                FilterChip(
                                    onClick = { selectedTrendChart = chartType },
                                    label = {
                                        Text(
                                            text = when (chartType) {
                                                TrendChartType.MEMORY_USAGE -> "Memory"
                                                TrendChartType.EVENT_RATE -> "Events"
                                                TrendChartType.CONNECTION_LATENCY -> "Latency"
                                                TrendChartType.HEALTH_SCORE -> "Health"
                                            }
                                        )
                                    },
                                    selected = selectedTrendChart == chartType
                                )
                            }
                        }
                    }
                }
            }
            
            // Advanced Filter Section
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Filter statistics badge
                            if (filterState.hasActiveFilters()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "${filterStats.filteredEvents}/${filterStats.totalEvents}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = filteredEvents.size.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AdvancedFilterBar(
                        searchQuery = filterState.searchQuery,
                        onSearchQueryChange = { query ->
                            filterState = filterState.updateSearchQuery(query)
                        },
                        selectedTypes = filterState.selectedTypes,
                        onTypeToggle = { type ->
                            filterState = filterState.toggleType(type)
                        },
                        selectedSeverities = filterState.selectedSeverities,
                        onSeverityToggle = { severity ->
                            filterState = filterState.toggleSeverity(severity)
                        },
                        selectedSessions = filterState.selectedSessions,
                        onSessionToggle = { sessionId ->
                            filterState = filterState.toggleSession(sessionId)
                        },
                        availableSessions = availableSessions,
                        activePreset = filterState.activePreset,
                        onPresetApply = { preset ->
                            filterState = filterState.applyPreset(preset)
                        },
                        onClearAll = {
                            filterState = filterState.clearAll()
                        }
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
                                Column {
                                    // Add severity divider for critical events
                                    if (item.severity == Severity.CRITICAL) {
                                        SeverityDivider(
                                            severity = item.severity,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    
                                    HookEventCard(
                                        event = item,
                                        searchQuery = filterState.searchQuery,
                                        onClick = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Event details: ${item.title}",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            selectedEventForShare = item
                                            showShareBottomSheet = true
                                        }
                                    )
                                    
                                    // Add separator after critical events
                                    if (item.severity == Severity.CRITICAL) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
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
    
    // Export Service
    val exportService = remember { ExportService(context) }
    
    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format, _ ->
                scope.launch {
                    try {
                        val result = exportService.exportEvents(
                            events = filteredEvents,
                            format = format
                        )
                        if (result.isSuccess) {
                            snackbarHostState.showSnackbar(
                                message = "Export completed successfully!",
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                message = "Export failed: ${result.exceptionOrNull()?.message}",
                                duration = SnackbarDuration.Long
                            )
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar(
                            message = "Export error: ${e.message}",
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            },
            eventCount = filteredEvents.size
        )
    }
    
    // Share Bottom Sheet
    if (showShareBottomSheet && selectedEventForShare != null) {
        ShareBottomSheet(
            event = selectedEventForShare!!,
            onDismiss = {
                showShareBottomSheet = false
                selectedEventForShare = null
            },
            onShareText = {
                val shareContent = "Hook Event: ${selectedEventForShare!!.title}\n${selectedEventForShare!!.message}\nSource: ${selectedEventForShare!!.source}\nTime: ${selectedEventForShare!!.timestamp}"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareContent)
                    putExtra(Intent.EXTRA_SUBJECT, "Claude Hook Event")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Event"))
            },
            onCopyToClipboard = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    "Hook Event", 
                    "${selectedEventForShare!!.title}: ${selectedEventForShare!!.message}"
                )
                clipboard.setPrimaryClip(clip)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Event copied to clipboard",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
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
                isImportant = stats.criticalCount > 0,
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
                isImportant = stats.warningCount > 0,
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

/**
 * Severity divider for visual separation of critical events
 */
@Composable
private fun SeverityDivider(
    severity: Severity,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (severity) {
        Severity.CRITICAL -> MaterialTheme.colorScheme.error to "🚨 CRITICAL ALERTS"
        Severity.ERROR -> MaterialTheme.colorScheme.error to "⚠️ ERRORS"
        Severity.WARNING -> Color(0xFFFF9800) to "⚠️ WARNINGS"
        Severity.INFO -> MaterialTheme.colorScheme.primary to "ℹ️ INFORMATION"
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = color,
            thickness = 2.dp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = color,
            thickness = 2.dp
        )
    }
}


