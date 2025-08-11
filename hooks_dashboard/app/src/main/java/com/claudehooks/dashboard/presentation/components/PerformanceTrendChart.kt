package com.claudehooks.dashboard.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.data.model.PerformanceHistory
import com.claudehooks.dashboard.data.model.PerformanceTrendPoint
import kotlin.math.max
import kotlin.math.min

/**
 * Performance trend chart types
 */
enum class TrendChartType {
    MEMORY_USAGE,
    EVENT_RATE,
    CONNECTION_LATENCY,
    HEALTH_SCORE
}

/**
 * Performance trend chart with historical data visualization
 */
@Composable
fun PerformanceTrendChart(
    history: PerformanceHistory,
    chartType: TrendChartType,
    expanded: Boolean = false,
    onToggleExpanded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val data = when (chartType) {
        TrendChartType.MEMORY_USAGE -> history.memoryUsage
        TrendChartType.EVENT_RATE -> history.eventRate
        TrendChartType.CONNECTION_LATENCY -> history.connectionLatency
        TrendChartType.HEALTH_SCORE -> history.healthScore
    }
    
    val chartInfo = getChartInfo(chartType)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = chartInfo.icon,
                        contentDescription = chartInfo.title,
                        tint = chartInfo.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = chartInfo.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (data.isNotEmpty()) {
                        val currentValue = data.lastOrNull()?.value ?: 0f
                        val average = data.map { it.value }.average().toFloat()
                        
                        Text(
                            text = "${formatValue(currentValue, chartInfo.unit)} ${getTrendIndicator(data)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = chartInfo.color
                        )
                        
                        Text(
                            text = "Avg: ${formatValue(average, chartInfo.unit)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chart
            val chartHeight = if (expanded) 120.dp else 60.dp
            
            if (data.isEmpty()) {
                EmptyChart(
                    height = chartHeight,
                    message = "No data available"
                )
            } else {
                TrendLineChart(
                    data = data,
                    color = chartInfo.color,
                    height = chartHeight,
                    maxValue = chartInfo.maxValue,
                    showLabels = expanded
                )
            }
            
            if (expanded && data.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Statistics
                PerformanceStatistics(
                    data = data,
                    unit = chartInfo.unit,
                    color = chartInfo.color
                )
            }
        }
    }
}

/**
 * Trend line chart visualization
 */
@Composable
private fun TrendLineChart(
    data: List<PerformanceTrendPoint>,
    color: Color,
    height: Dp,
    maxValue: Float? = null,
    showLabels: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "chart_animation"
    )
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = if (showLabels) 24.dp else 8.dp)
    ) {
        if (data.size < 2) return@Canvas
        
        val width = size.width
        val canvasHeight = size.height
        val padding = if (showLabels) 20f else 10f
        
        val minValue = data.minOfOrNull { it.value } ?: 0f
        val actualMaxValue = maxValue ?: (data.maxOfOrNull { it.value } ?: 100f)
        val valueRange = actualMaxValue - minValue
        
        if (valueRange <= 0) return@Canvas
        
        // Calculate points
        val points = data.mapIndexed { index, point ->
            val x = (index.toFloat() / (data.size - 1)) * width
            val y = canvasHeight - padding - ((point.value - minValue) / valueRange) * (canvasHeight - 2 * padding)
            Offset(x, y)
        }
        
        // Draw grid lines (if expanded)
        if (showLabels) {
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            val gridLines = 5
            repeat(gridLines) { i ->
                val y = padding + (i * (canvasHeight - 2 * padding) / (gridLines - 1))
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        
        // Draw area under curve
        val areaPath = Path().apply {
            moveTo(points.first().x, canvasHeight - padding)
            points.forEachIndexed { index, point ->
                val progressIndex = (index * animatedProgress).toInt()
                if (progressIndex < points.size) {
                    lineTo(points[progressIndex].x, points[progressIndex].y)
                }
            }
            lineTo(points.last().x, canvasHeight - padding)
            close()
        }
        
        drawPath(
            path = areaPath,
            color = color.copy(alpha = 0.1f)
        )
        
        // Draw trend line
        for (i in 0 until (points.size - 1).coerceAtMost((points.size * animatedProgress).toInt())) {
            drawLine(
                color = color,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        // Draw data points
        points.forEachIndexed { index, point ->
            if (index < (points.size * animatedProgress).toInt()) {
                drawCircle(
                    color = color,
                    radius = 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
        }
        
        // Highlight current value
        if (animatedProgress >= 1f && points.isNotEmpty()) {
            val lastPoint = points.last()
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = 8.dp.toPx(),
                center = lastPoint
            )
            drawCircle(
                color = color,
                radius = 6.dp.toPx(),
                center = lastPoint
            )
        }
    }
}

/**
 * Empty chart placeholder
 */
@Composable
private fun EmptyChart(
    height: Dp,
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Performance statistics display
 */
@Composable
private fun PerformanceStatistics(
    data: List<PerformanceTrendPoint>,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val values = data.map { it.value }
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 0f
    val avg = values.average().toFloat()
    val latest = values.lastOrNull() ?: 0f
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem(
                label = "Current",
                value = formatValue(latest, unit),
                color = color
            )
            StatisticItem(
                label = "Average",
                value = formatValue(avg, unit),
                color = MaterialTheme.colorScheme.primary
            )
            StatisticItem(
                label = "Min",
                value = formatValue(min, unit),
                color = Color(0xFF4CAF50)
            )
            StatisticItem(
                label = "Max",
                value = formatValue(max, unit),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Individual statistic item
 */
@Composable
private fun StatisticItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
 * Get chart configuration information
 */
private fun getChartInfo(chartType: TrendChartType): ChartInfo = when (chartType) {
    TrendChartType.MEMORY_USAGE -> ChartInfo(
        title = "Memory Usage",
        icon = Icons.Default.Memory,
        color = Color(0xFF2196F3),
        unit = "%",
        maxValue = 100f
    )
    TrendChartType.EVENT_RATE -> ChartInfo(
        title = "Events/sec",
        icon = Icons.Default.Timeline,
        color = Color(0xFF4CAF50),
        unit = "/s",
        maxValue = null
    )
    TrendChartType.CONNECTION_LATENCY -> ChartInfo(
        title = "Latency",
        icon = Icons.Default.Wifi,
        color = Color(0xFFFF9800),
        unit = "ms",
        maxValue = null
    )
    TrendChartType.HEALTH_SCORE -> ChartInfo(
        title = "Health Score",
        icon = Icons.Default.Favorite,
        color = Color(0xFFE91E63),
        unit = "%",
        maxValue = 100f
    )
}

/**
 * Chart configuration data class
 */
private data class ChartInfo(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val unit: String,
    val maxValue: Float?
)

/**
 * Get trend indicator (up/down arrow)
 */
private fun getTrendIndicator(data: List<PerformanceTrendPoint>): String {
    if (data.size < 2) return ""
    
    val recent = data.takeLast(5).map { it.value }
    val earlier = data.dropLast(5).takeLast(5).map { it.value }
    
    if (recent.isEmpty() || earlier.isEmpty()) return ""
    
    val recentAvg = recent.average()
    val earlierAvg = earlier.average()
    
    return when {
        recentAvg > earlierAvg * 1.05 -> "↗"
        recentAvg < earlierAvg * 0.95 -> "↘"
        else -> "→"
    }
}

/**
 * Format value with appropriate precision
 */
private fun formatValue(value: Float, unit: String): String = when (unit) {
    "%" -> "${value.toInt()}%"
    "ms" -> "${value.toInt()}ms"
    "/s" -> String.format("%.1f", value)
    else -> String.format("%.1f", value)
}