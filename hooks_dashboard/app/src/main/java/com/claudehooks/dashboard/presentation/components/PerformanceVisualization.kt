package com.claudehooks.dashboard.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.data.model.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Real-time performance metrics display card
 */
@Composable
fun PerformanceMetricsCard(
    metrics: PerformanceMetrics,
    expanded: Boolean = false,
    onToggleExpanded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (metrics.memoryPressureLevel) {
                MemoryPressureLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                MemoryPressureLevel.HIGH -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with health score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Performance",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Performance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HealthScoreIndicator(
                        score = metrics.healthScore,
                        size = 32.dp
                    )
                    Text(
                        text = "${metrics.healthScore.toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = getHealthScoreColor(metrics.healthScore)
                    )
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricIndicator(
                    icon = Icons.Default.Memory,
                    label = "Memory",
                    value = "${metrics.memoryUsagePercent.toInt()}%",
                    color = getMemoryPressureColor(metrics.memoryPressureLevel),
                    modifier = Modifier.weight(1f)
                )
                MetricIndicator(
                    icon = Icons.Default.Timeline,
                    label = "Events/sec",
                    value = String.format("%.1f", metrics.eventsPerSecond),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricIndicator(
                    icon = Icons.Default.Wifi,
                    label = "Latency",
                    value = "${metrics.connectionLatencyMs}ms",
                    color = getConnectionQualityColor(metrics.connectionQuality),
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Detailed metrics
                DetailedMetrics(metrics)
            }
        }
    }
}

/**
 * Detailed performance metrics view
 */
@Composable
private fun DetailedMetrics(metrics: PerformanceMetrics) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Memory usage bar
        MetricProgressBar(
            label = "Memory Usage",
            value = metrics.memoryUsagePercent,
            maxValue = 100f,
            color = getMemoryPressureColor(metrics.memoryPressureLevel),
            suffix = "${metrics.memoryUsageMB}MB / ${metrics.maxMemoryMB}MB"
        )
        
        // Cache hit rate bar
        MetricProgressBar(
            label = "Cache Hit Rate",
            value = metrics.cacheHitRate * 100f,
            maxValue = 100f,
            color = if (metrics.cacheHitRate > 0.8f) Color(0xFF4CAF50) else Color(0xFFFF9800),
            suffix = "${(metrics.cacheHitRate * 100).toInt()}%"
        )
        
        // CPU usage bar
        MetricProgressBar(
            label = "CPU Usage",
            value = metrics.cpuUsagePercent,
            maxValue = 100f,
            color = when {
                metrics.cpuUsagePercent < 50f -> Color(0xFF4CAF50)
                metrics.cpuUsagePercent < 80f -> Color(0xFFFF9800)
                else -> MaterialTheme.colorScheme.error
            },
            suffix = "${metrics.cpuUsagePercent.toInt()}%"
        )
        
        // Network activity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NetworkIndicator(
                label = "In",
                bytes = metrics.networkBytesIn,
                isIncoming = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            NetworkIndicator(
                label = "Out",
                bytes = metrics.networkBytesOut,
                isIncoming = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Health score circular indicator
 */
@Composable
fun HealthScoreIndicator(
    score: Float,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val animatedScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "health_score_animation"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = this.size.width / 8f
            val radius = (this.size.width - strokeWidth) / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            
            // Background circle
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Progress arc
            val sweepAngle = (animatedScore / 100f) * 360f
            val color = getHealthScoreColor(animatedScore)
            
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Metric indicator with icon and value
 */
@Composable
private fun MetricIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Progress bar for detailed metrics
 */
@Composable
private fun MetricProgressBar(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color,
    suffix: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = suffix,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = (value / maxValue).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = color.copy(alpha = 0.3f)
        )
    }
}

/**
 * Network activity indicator
 */
@Composable
private fun NetworkIndicator(
    label: String,
    bytes: Long,
    isIncoming: Boolean,
    modifier: Modifier = Modifier
) {
    val formattedBytes = formatBytes(bytes)
    val icon = if (isIncoming) Icons.Default.GetApp else Icons.Default.Publish
    val color = if (isIncoming) Color(0xFF4CAF50) else Color(0xFF2196F3)
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$label network activity",
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = formattedBytes,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Get color for health score
 */
private fun getHealthScoreColor(score: Float): Color = when {
    score >= 80f -> Color(0xFF4CAF50)  // Green
    score >= 60f -> Color(0xFF8BC34A)  // Light green
    score >= 40f -> Color(0xFFFF9800)  // Orange
    score >= 20f -> Color(0xFFFF5722)  // Deep orange
    else -> Color(0xFFF44336)          // Red
}

/**
 * Get color for memory pressure level
 */
private fun getMemoryPressureColor(level: MemoryPressureLevel): Color = when (level) {
    MemoryPressureLevel.LOW -> Color(0xFF4CAF50)
    MemoryPressureLevel.MEDIUM -> Color(0xFF8BC34A)
    MemoryPressureLevel.HIGH -> Color(0xFFFF9800)
    MemoryPressureLevel.CRITICAL -> Color(0xFFF44336)
}

/**
 * Get color for connection quality
 */
private fun getConnectionQualityColor(quality: ConnectionQuality): Color = when (quality) {
    ConnectionQuality.EXCELLENT -> Color(0xFF4CAF50)
    ConnectionQuality.GOOD -> Color(0xFF8BC34A)
    ConnectionQuality.FAIR -> Color(0xFFFF9800)
    ConnectionQuality.POOR -> Color(0xFFFF5722)
    ConnectionQuality.VERY_POOR -> Color(0xFFF44336)
}

/**
 * Format bytes to human readable string
 */
private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "${bytes / 1024}KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
    else -> "${bytes / (1024 * 1024 * 1024)}GB"
}