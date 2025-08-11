package com.claudehooks.dashboard.presentation.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.platform.LocalDensity
import com.claudehooks.dashboard.presentation.util.getCutoutInfo
import com.claudehooks.dashboard.service.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CutoutAwareToolbar(
    connectionStatus: ConnectionStatus,
    lastUpdateTime: Instant?,
    useTestData: Boolean,
    onTestDataToggle: () -> Unit,
    onReconnect: () -> Unit,
    onPerformanceToggle: () -> Unit,
    showPerformanceMetrics: Boolean,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    onInfo: () -> Unit,
    onQuitRequested: (() -> Unit)?,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val cutoutInfo = getCutoutInfo()
    var showQuitMenu by remember { mutableStateOf(false) }
    
    // Calculate layout strategy based on cutout presence
    if (cutoutInfo.hasCutout && cutoutInfo.topInset > 0.dp) {
        // Cutout-aware layout: distribute icons around cutout with title below
        CutoutOptimizedLayout(
            cutoutInfo = cutoutInfo,
            connectionStatus = connectionStatus,
            lastUpdateTime = lastUpdateTime,
            useTestData = useTestData,
            onTestDataToggle = onTestDataToggle,
            onReconnect = onReconnect,
            onPerformanceToggle = onPerformanceToggle,
            showPerformanceMetrics = showPerformanceMetrics,
            onExport = onExport,
            onSettings = onSettings,
            onInfo = onInfo,
            onQuitRequested = onQuitRequested,
            scope = scope,
            snackbarHostState = snackbarHostState,
            showQuitMenu = showQuitMenu,
            onShowQuitMenuChange = { showQuitMenu = it },
            modifier = modifier
        )
    } else {
        // Standard layout for devices without cutouts
        StandardToolbarLayout(
            connectionStatus = connectionStatus,
            lastUpdateTime = lastUpdateTime,
            useTestData = useTestData,
            onTestDataToggle = onTestDataToggle,
            onReconnect = onReconnect,
            onPerformanceToggle = onPerformanceToggle,
            showPerformanceMetrics = showPerformanceMetrics,
            onExport = onExport,
            onSettings = onSettings,
            onInfo = onInfo,
            onQuitRequested = onQuitRequested,
            scope = scope,
            snackbarHostState = snackbarHostState,
            showQuitMenu = showQuitMenu,
            onShowQuitMenuChange = { showQuitMenu = it },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CutoutOptimizedLayout(
    cutoutInfo: com.claudehooks.dashboard.presentation.util.CutoutInfo,
    connectionStatus: ConnectionStatus,
    lastUpdateTime: Instant?,
    useTestData: Boolean,
    onTestDataToggle: () -> Unit,
    onReconnect: () -> Unit,
    onPerformanceToggle: () -> Unit,
    showPerformanceMetrics: Boolean,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    onInfo: () -> Unit,
    onQuitRequested: (() -> Unit)?,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    showQuitMenu: Boolean,
    onShowQuitMenuChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Get proper system status bar padding to avoid system UI overlap
    val systemStatusBarPadding = WindowInsets.statusBars.asPaddingValues()
    
    // Calculate the positioning strategy:
    // - Adequate top padding to clear system status bar (time, battery, etc.)
    // - Icons positioned in safe area between system UI and cutout
    // - Title positioned closer to cutout area with minimal gap
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = systemStatusBarPadding.calculateTopPadding() + 8.dp, // Proper clearance for system UI
                start = cutoutInfo.leftInset,
                end = cutoutInfo.rightInset
            )
    ) {
        // Top row: Icons distributed around cutout area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side icons (connection indicators)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedConnectionStatus(connectionStatus = connectionStatus)
                DataFreshnessIndicator(
                    lastUpdate = lastUpdateTime,
                    connectionStatus = connectionStatus
                )
                
                // Test data toggle
                IconButton(onClick = onTestDataToggle) {
                    Icon(
                        if (useTestData) Icons.Default.BugReport else Icons.Default.CloudDone,
                        contentDescription = if (useTestData) "Switch to Live Data" else "Switch to Test Data",
                        tint = if (useTestData) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Right side icons (actions)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Reconnect button (conditional)
                if (!useTestData && connectionStatus == ConnectionStatus.ERROR) {
                    IconButton(onClick = onReconnect) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reconnect")
                    }
                }
                
                // Performance metrics toggle
                IconButton(onClick = onPerformanceToggle) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Performance Metrics",
                        tint = if (showPerformanceMetrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Export button
                IconButton(onClick = onExport) {
                    Icon(
                        imageVector = Icons.Default.GetApp,
                        contentDescription = "Export Data",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Settings button
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
                
                // Info button
                IconButton(onClick = onInfo) {
                    Icon(Icons.Default.Info, contentDescription = "Connection Info")
                }
            }
        }
        
        // Minimal spacer to position title closer to cutout (optimize space usage)
        Spacer(
            modifier = Modifier.height(
                maxOf(cutoutInfo.topInset - 80.dp, 2.dp) // Ultra-aggressive gap reduction to bring title closer
            )
        )
        
        // Bottom row: Centered title with quit menu (positioned below cutout)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .combinedClickable(
                        onClick = { /* Regular click - do nothing */ },
                        onLongClick = { onShowQuitMenuChange(true) }
                    )
            ) {
                Text(
                    text = "Dashboard",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "• Ultra-optimized spacing •",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            // Quit menu
            QuitDropdownMenu(
                expanded = showQuitMenu,
                onDismiss = { onShowQuitMenuChange(false) },
                onQuitRequested = onQuitRequested,
                scope = scope,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun StandardToolbarLayout(
    connectionStatus: ConnectionStatus,
    lastUpdateTime: Instant?,
    useTestData: Boolean,
    onTestDataToggle: () -> Unit,
    onReconnect: () -> Unit,
    onPerformanceToggle: () -> Unit,
    showPerformanceMetrics: Boolean,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    onInfo: () -> Unit,
    onQuitRequested: (() -> Unit)?,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    showQuitMenu: Boolean,
    onShowQuitMenuChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Standard single-row layout for non-cutout devices
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .combinedClickable(
                            onClick = { /* Regular click - do nothing */ },
                            onLongClick = { onShowQuitMenuChange(true) }
                        )
                ) {
                    Text(
                        text = "Dashboard",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                // Quit menu
                QuitDropdownMenu(
                    expanded = showQuitMenu,
                    onDismiss = { onShowQuitMenuChange(false) },
                    onQuitRequested = onQuitRequested,
                    scope = scope,
                    snackbarHostState = snackbarHostState
                )
            }
        },
        navigationIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedConnectionStatus(connectionStatus = connectionStatus)
                DataFreshnessIndicator(
                    lastUpdate = lastUpdateTime,
                    connectionStatus = connectionStatus
                )
            }
        },
        actions = {
            // Test data toggle
            IconButton(onClick = onTestDataToggle) {
                Icon(
                    if (useTestData) Icons.Default.BugReport else Icons.Default.CloudDone,
                    contentDescription = if (useTestData) "Switch to Live Data" else "Switch to Test Data",
                    tint = if (useTestData) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                )
            }
            
            // Reconnect button (conditional)
            if (!useTestData && connectionStatus == ConnectionStatus.ERROR) {
                IconButton(onClick = onReconnect) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reconnect")
                }
            }
            
            // Performance metrics toggle
            IconButton(onClick = onPerformanceToggle) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Performance Metrics",
                    tint = if (showPerformanceMetrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Export button
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Default.GetApp,
                    contentDescription = "Export Data",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Settings button
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
            
            // Info button
            IconButton(onClick = onInfo) {
                Icon(Icons.Default.Info, contentDescription = "Connection Info")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuitDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onQuitRequested: (() -> Unit)?,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Quit Application") },
            leadingIcon = {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            onClick = {
                onDismiss()
                if (onQuitRequested != null) {
                    onQuitRequested()
                } else {
                    (context as? ComponentActivity)?.finishAndRemoveTask()
                }
            }
        )
        Divider()
        DropdownMenuItem(
            text = { Text("About") },
            leadingIcon = {
                Icon(Icons.Default.Info, contentDescription = null)
            },
            onClick = {
                onDismiss()
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Claude Hooks Dashboard v1.4 - Space Optimization Edition",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }
}

@Composable
private fun AnimatedConnectionStatus(
    connectionStatus: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    // Enhanced connection status indicator
    Box(
        modifier = modifier.padding(start = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (connectionStatus) {
            ConnectionStatus.CONNECTED -> {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Connected to Redis",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF4CAF50)
                )
            }
            ConnectionStatus.CONNECTING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            ConnectionStatus.ERROR -> {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Connection Error",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
            ConnectionStatus.DISCONNECTED -> {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Disconnected",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}