package com.claudehooks.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatRelativeTime(event.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun formatRelativeTime(timestamp: Instant): String {
    val now = Instant.now()
    val minutes = ChronoUnit.MINUTES.between(timestamp, now)
    
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60} hours ago"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                .withZone(ZoneId.systemDefault())
            formatter.format(timestamp)
        }
    }
}