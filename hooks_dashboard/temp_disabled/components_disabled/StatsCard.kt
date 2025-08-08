package com.claudehooks.dashboard.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.domain.model.*
import com.claudehooks.dashboard.presentation.theme.*

@Composable
fun StatsOverview(
    stats: DashboardStats,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            StatCard(
                title = "Total Hooks",
                value = stats.totalHooks.toString(),
                icon = Icons.Default.DataArray,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        item {
            StatCard(
                title = "Active Sessions",
                value = stats.activeSessions.toString(),
                icon = Icons.Default.PlayArrow,
                color = SuccessColor
            )
        }
        
        item {
            StatCard(
                title = "Success Rate",
                value = "${(stats.successRate * 100).toInt()}%",
                icon = Icons.Default.CheckCircle,
                color = if (stats.successRate > 0.8f) SuccessColor else ErrorColor
            )
        }
        
        item {
            StatCard(
                title = "Avg Exec Time",
                value = "${stats.averageExecutionTime ?: 0}ms",
                icon = Icons.Default.Schedule,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        item {
            StatCard(
                title = "Hooks/Hour",
                value = stats.hooksPerHour.toInt().toString(),
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HookTypeDistributionCard(
    hookTypeCounts: Map<HookType, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Hook Type Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val total = hookTypeCounts.values.sum().toFloat()
            
            hookTypeCounts.entries.sortedByDescending { it.value }.forEach { (hookType, count) ->
                val percentage = if (total > 0) (count / total * 100) else 0f
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HookTypeIndicator(hookType = hookType)
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = hookType.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = percentage / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = getHookTypeColor(hookType)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "$count (${percentage.toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformDistributionCard(
    platformCounts: Map<Platform, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Platform Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                platformCounts.entries.forEach { (platform, count) ->
                    PlatformStatItem(
                        platform = platform,
                        count = count,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HookTypeIndicator(hookType: HookType) {
    val color = getHookTypeColor(hookType)
    
    Box(
        modifier = Modifier
            .size(16.dp)
            .padding(2.dp)
    ) {
        Icon(
            imageVector = getHookTypeIcon(hookType),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun PlatformStatItem(
    platform: Platform,
    count: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = getPlatformIcon(platform),
            contentDescription = null,
            tint = getPlatformColor(platform),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = platform.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getHookTypeColor(hookType: HookType): Color {
    return when (hookType) {
        HookType.session_start -> SessionStartColor
        HookType.user_prompt_submit -> UserPromptColor
        HookType.pre_tool_use -> ToolUseColor
        HookType.post_tool_use -> ToolUseColor
        HookType.notification -> NotificationColor
        HookType.stop_hook -> StopHookColor
        HookType.sub_agent_stop_hook -> StopHookColor
        HookType.pre_compact -> CompactColor
    }
}

private fun getHookTypeIcon(hookType: HookType): ImageVector {
    return when (hookType) {
        HookType.session_start -> Icons.Default.PlayArrow
        HookType.user_prompt_submit -> Icons.Default.Send
        HookType.pre_tool_use -> Icons.Default.Build
        HookType.post_tool_use -> Icons.Default.CheckCircle
        HookType.notification -> Icons.Default.Notifications
        HookType.stop_hook -> Icons.Default.Stop
        HookType.sub_agent_stop_hook -> Icons.Default.StopCircle
        HookType.pre_compact -> Icons.Default.Compress
    }
}

private fun getPlatformColor(platform: Platform): Color {
    return when (platform) {
        Platform.darwin -> DarwinColor
        Platform.linux -> LinuxColor
        Platform.windows -> WindowsColor
        Platform.unknown -> Color.Gray
    }
}

private fun getPlatformIcon(platform: Platform): ImageVector {
    return when (platform) {
        Platform.darwin -> Icons.Default.Laptop
        Platform.linux -> Icons.Default.Computer
        Platform.windows -> Icons.Default.DesktopWindows
        Platform.unknown -> Icons.Default.DeviceUnknown
    }
}