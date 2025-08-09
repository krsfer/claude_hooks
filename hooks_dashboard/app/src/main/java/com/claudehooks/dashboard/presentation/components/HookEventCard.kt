package com.claudehooks.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HookEventCard(
    event: HookEvent,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(Instant.now()) }
    var showAbsoluteTime by remember { mutableStateOf(false) }
    
    LaunchedEffect(event.timestamp) {
        while (true) {
            delay(1000L) // Update every second
            currentTime = Instant.now()
        }
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.severity) {
                Severity.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                Severity.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                Severity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(getTypeColor(event.type).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getTypeIcon(event.type),
                    contentDescription = null,
                    tint = getTypeColor(event.type),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = event.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Severity indicator
                    SeverityChip(severity = event.severity)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = getTimeAgeColor(event.timestamp, currentTime)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showAbsoluteTime) formatAbsoluteTime(event.timestamp) 
                                   else formatDurationAge(event.timestamp, currentTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = getTimeAgeColor(event.timestamp, currentTime),
                            modifier = Modifier.clickable { 
                                showAbsoluteTime = !showAbsoluteTime 
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = event.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityChip(severity: Severity) {
    val (backgroundColor, textColor) = when (severity) {
        Severity.CRITICAL -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        Severity.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Severity.WARNING -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        Severity.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor.copy(alpha = 0.9f),
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = severity.name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun getTypeIcon(type: HookType): ImageVector = when (type) {
    HookType.SESSION_START -> Icons.Default.PlayArrow
    HookType.USER_PROMPT_SUBMIT -> Icons.Default.Message
    HookType.PRE_TOOL_USE -> Icons.Default.Build
    HookType.POST_TOOL_USE -> Icons.Default.Done
    HookType.NOTIFICATION -> Icons.Default.Notifications
    HookType.STOP_HOOK -> Icons.Default.Stop
    HookType.SUB_AGENT_STOP_HOOK -> Icons.Default.StopCircle
    HookType.PRE_COMPACT -> Icons.Default.Compress
    // Legacy types
    HookType.API_CALL -> Icons.Default.Cloud
    HookType.DATABASE -> Icons.Default.Storage
    HookType.FILE_SYSTEM -> Icons.Default.Folder
    HookType.NETWORK -> Icons.Default.Wifi
    HookType.SECURITY -> Icons.Default.Security
    HookType.PERFORMANCE -> Icons.Default.Speed
    HookType.ERROR -> Icons.Default.Error
    HookType.CUSTOM -> Icons.Default.Extension
}

private fun getTypeColor(type: HookType): Color = when (type) {
    HookType.SESSION_START -> Color(0xFF4CAF50)
    HookType.USER_PROMPT_SUBMIT -> Color(0xFF2196F3)
    HookType.PRE_TOOL_USE -> Color(0xFFFF9800)
    HookType.POST_TOOL_USE -> Color(0xFF4CAF50)
    HookType.NOTIFICATION -> Color(0xFF9C27B0)
    HookType.STOP_HOOK -> Color(0xFFF44336)
    HookType.SUB_AGENT_STOP_HOOK -> Color(0xFFE91E63)
    HookType.PRE_COMPACT -> Color(0xFF00BCD4)
    // Legacy types
    HookType.API_CALL -> Color(0xFF4CAF50)
    HookType.DATABASE -> Color(0xFF2196F3)
    HookType.FILE_SYSTEM -> Color(0xFFFF9800)
    HookType.NETWORK -> Color(0xFF9C27B0)
    HookType.SECURITY -> Color(0xFFF44336)
    HookType.PERFORMANCE -> Color(0xFF00BCD4)
    HookType.ERROR -> Color(0xFFE91E63)
    HookType.CUSTOM -> Color(0xFF607D8B)
}

private fun formatDurationAge(timestamp: Instant, currentTime: Instant): String {
    return formatRelativeTime(timestamp, currentTime)
}

/**
 * Enhanced relative time formatting with intuitive display
 * Examples: "just now", "5s ago", "2m ago", "1h ago", "3d ago"
 */
private fun formatRelativeTime(timestamp: Instant, currentTime: Instant): String {
    val totalSeconds = ChronoUnit.SECONDS.between(timestamp, currentTime)
    
    return when {
        totalSeconds < 10 -> "just now"
        totalSeconds < 60 -> "${totalSeconds}s ago"
        totalSeconds < 3600 -> "${totalSeconds / 60}m ago"
        totalSeconds < 86400 -> "${totalSeconds / 3600}h ago"
        totalSeconds < 2592000 -> "${totalSeconds / 86400}d ago" // 30 days
        else -> {
            // For very old events, show the date
            val formatter = DateTimeFormatter.ofPattern("MMM dd")
                .withZone(ZoneId.systemDefault())
            formatter.format(timestamp)
        }
    }
}

/**
 * Get color based on event age for visual hierarchy
 */
private fun getTimeAgeColor(timestamp: Instant, currentTime: Instant): Color {
    val totalSeconds = ChronoUnit.SECONDS.between(timestamp, currentTime)
    
    return when {
        totalSeconds < 300 -> Color(0xFF4CAF50)     // Fresh: Green (< 5 min)
        totalSeconds < 3600 -> Color(0xFF2196F3)    // Recent: Blue (< 1 hour)  
        totalSeconds < 86400 -> Color(0xFFFF9800)   // Today: Orange (< 1 day)
        else -> Color(0xFF9E9E9E)                   // Old: Gray (> 1 day)
    }
}

/**
 * Format absolute timestamp for tap-to-reveal functionality
 */
private fun formatAbsoluteTime(timestamp: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(timestamp)
}