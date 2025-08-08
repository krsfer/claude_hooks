package com.claudehooks.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claudehooks.dashboard.domain.model.*
import com.claudehooks.dashboard.presentation.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HookCard(
    hook: HookData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row with hook type, status, and timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HookTypeChip(hookType = hook.hook_type)
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusChip(status = hook.core.status)
                }
                
                Text(
                    text = formatTimestamp(hook.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Session and sequence info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Session: ${hook.session_id.takeLast(8)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Seq: ${hook.sequence}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Content preview
            hook.payload.prompt?.let { prompt ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            hook.payload.tool_name?.let { toolName ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = toolName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            hook.payload.message?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Expanded details
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                ExpandedHookDetails(hook = hook)
            }
        }
    }
}

@Composable
private fun HookTypeChip(hookType: HookType) {
    val (color, icon) = getHookTypeColorAndIcon(hookType)
    
    Row(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = hookType.name.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun StatusChip(status: HookStatus) {
    val color = when (status) {
        HookStatus.success -> SuccessColor
        HookStatus.error -> ErrorColor
        HookStatus.pending -> PendingColor
        HookStatus.blocked -> BlockedColor
    }
    
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ExpandedHookDetails(hook: HookData) {
    Column {
        // Context information
        Text(
            text = "Context",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        DetailRow("Platform", hook.context.platform.name)
        hook.context.git_branch?.let { DetailRow("Git Branch", it) }
        hook.context.git_status?.let { DetailRow("Git Status", it.name) }
        hook.context.project_type?.let { DetailRow("Project Type", it.name) }
        
        hook.core.execution_time_ms?.let {
            DetailRow("Execution Time", "${it}ms")
        }
        
        // Tool details
        if (hook.payload.tool_name != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tool Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            hook.payload.tool_input?.let {
                DetailRow("Input", it, isJson = true)
            }
            
            hook.payload.tool_response?.let {
                DetailRow("Response", it, isJson = true)
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isJson: Boolean = false
) {
    Column {
        Row {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(min = 100.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun getHookTypeColorAndIcon(hookType: HookType): Pair<Color, ImageVector> {
    return when (hookType) {
        HookType.session_start -> SessionStartColor to Icons.Default.PlayArrow
        HookType.user_prompt_submit -> UserPromptColor to Icons.Default.Send
        HookType.pre_tool_use -> ToolUseColor to Icons.Default.Build
        HookType.post_tool_use -> ToolUseColor to Icons.Default.CheckCircle
        HookType.notification -> NotificationColor to Icons.Default.Notifications
        HookType.stop_hook -> StopHookColor to Icons.Default.Stop
        HookType.sub_agent_stop_hook -> StopHookColor to Icons.Default.StopCircle
        HookType.pre_compact -> CompactColor to Icons.Default.Compress
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        timestamp.takeLast(8) // Fallback to last 8 characters
    }
}