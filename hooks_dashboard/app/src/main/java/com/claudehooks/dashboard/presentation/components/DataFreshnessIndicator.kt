package com.claudehooks.dashboard.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.service.ConnectionStatus
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Data freshness levels for visual feedback
 */
enum class DataFreshness {
    LIVE,        // Data is actively updating (< 5 seconds)
    FRESH,       // Data is recent (< 30 seconds)
    STALE,       // Data is getting old (< 2 minutes)
    VERY_STALE   // Data is very old (> 2 minutes)
}

/**
 * Calculate data freshness based on last update time
 */
fun calculateDataFreshness(lastUpdate: Instant?, currentTime: Instant = Instant.now()): DataFreshness {
    if (lastUpdate == null) return DataFreshness.VERY_STALE
    
    val secondsAgo = ChronoUnit.SECONDS.between(lastUpdate, currentTime)
    return when {
        secondsAgo < 5 -> DataFreshness.LIVE
        secondsAgo < 30 -> DataFreshness.FRESH
        secondsAgo < 120 -> DataFreshness.STALE
        else -> DataFreshness.VERY_STALE
    }
}

/**
 * Real-time data freshness indicator with live pulse animations
 */
@Composable
fun DataFreshnessIndicator(
    lastUpdate: Instant?,
    connectionStatus: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Instant.now()) }
    
    // Update current time every second for real-time freshness calculation
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = Instant.now()
        }
    }
    
    val freshness = calculateDataFreshness(lastUpdate, currentTime)
    
    val infiniteTransition = rememberInfiniteTransition(label = "data_freshness")
    
    // Live pulse animation for active data
    val livePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (freshness == DataFreshness.LIVE && connectionStatus == ConnectionStatus.CONNECTED) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_pulse"
    )
    
    // Stale data warning blink
    val staleWarning by infiniteTransition.animateFloat(
        initialValue = if (freshness == DataFreshness.VERY_STALE) 0.3f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stale_warning"
    )
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Data freshness pulse dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .scale(livePulse)
                .clip(CircleShape)
                .background(
                    when (freshness) {
                        DataFreshness.LIVE -> Color(0xFF4CAF50)
                        DataFreshness.FRESH -> Color(0xFF8BC34A)
                        DataFreshness.STALE -> Color(0xFFFF9800)
                        DataFreshness.VERY_STALE -> Color(0xFFF44336).copy(alpha = staleWarning)
                    }
                )
        )
        
        // Freshness text
        Text(
            text = when (freshness) {
                DataFreshness.LIVE -> "Live"
                DataFreshness.FRESH -> "Fresh"
                DataFreshness.STALE -> "Stale"
                DataFreshness.VERY_STALE -> "Very Stale"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when (freshness) {
                DataFreshness.LIVE -> Color(0xFF4CAF50)
                DataFreshness.FRESH -> Color(0xFF8BC34A)
                DataFreshness.STALE -> Color(0xFFFF9800)
                DataFreshness.VERY_STALE -> Color(0xFFF44336).copy(alpha = staleWarning)
            },
            fontWeight = if (freshness == DataFreshness.LIVE) FontWeight.Bold else FontWeight.Normal
        )
        
        // Last update time
        if (lastUpdate != null) {
            Text(
                text = formatLastUpdateTime(lastUpdate, currentTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Enhanced data freshness banner with reconnection suggestions
 */
@Composable
fun DataFreshnessBanner(
    lastUpdate: Instant?,
    connectionStatus: ConnectionStatus,
    onReconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val freshness = calculateDataFreshness(lastUpdate)
    
    // Only show banner for stale data or connection issues
    if (freshness in setOf(DataFreshness.STALE, DataFreshness.VERY_STALE) || 
        connectionStatus in setOf(ConnectionStatus.ERROR, ConnectionStatus.DISCONNECTED)) {
        
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    freshness == DataFreshness.VERY_STALE -> MaterialTheme.colorScheme.errorContainer
                    connectionStatus == ConnectionStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                    else -> warningContainer
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            connectionStatus == ConnectionStatus.ERROR -> Icons.Default.CloudOff
                            freshness == DataFreshness.VERY_STALE -> Icons.Default.Schedule
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = when {
                            freshness == DataFreshness.VERY_STALE -> MaterialTheme.colorScheme.onErrorContainer
                            connectionStatus == ConnectionStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                            else -> onWarningContainer
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                connectionStatus == ConnectionStatus.ERROR -> "Connection Lost"
                                freshness == DataFreshness.VERY_STALE -> "Data is Very Stale"
                                else -> "Data May Be Outdated"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                freshness == DataFreshness.VERY_STALE -> MaterialTheme.colorScheme.onErrorContainer
                                connectionStatus == ConnectionStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                                else -> onWarningContainer
                            }
                        )
                        Text(
                            text = when {
                                connectionStatus == ConnectionStatus.ERROR -> "Unable to receive new events"
                                freshness == DataFreshness.VERY_STALE -> "Last update: ${lastUpdate?.let { formatLastUpdateTime(it) } ?: "Unknown"}"
                                else -> "Consider refreshing for latest data"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                freshness == DataFreshness.VERY_STALE -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                connectionStatus == ConnectionStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                else -> onWarningContainer.copy(alpha = 0.8f)
                            }
                        )
                    }
                }
                
                if (connectionStatus in setOf(ConnectionStatus.ERROR, ConnectionStatus.DISCONNECTED)) {
                    TextButton(onClick = onReconnectClick) {
                        Text(
                            text = "Reconnect",
                            color = when {
                                freshness == DataFreshness.VERY_STALE -> MaterialTheme.colorScheme.onErrorContainer
                                connectionStatus == ConnectionStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                                else -> onWarningContainer
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format last update time for display
 */
private fun formatLastUpdateTime(lastUpdate: Instant, currentTime: Instant = Instant.now()): String {
    val secondsAgo = ChronoUnit.SECONDS.between(lastUpdate, currentTime)
    
    return when {
        secondsAgo < 5 -> "now"
        secondsAgo < 60 -> "${secondsAgo}s ago"
        secondsAgo < 3600 -> "${secondsAgo / 60}m ago"
        secondsAgo < 86400 -> "${secondsAgo / 3600}h ago"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMM dd HH:mm")
                .withZone(ZoneId.systemDefault())
            formatter.format(lastUpdate)
        }
    }
}

/**
 * Container colors for warning banner based on Material3 theme
 */
private val warningContainer: Color = Color(0xFFFFF3E0)
private val onWarningContainer: Color = Color(0xFFE65100)