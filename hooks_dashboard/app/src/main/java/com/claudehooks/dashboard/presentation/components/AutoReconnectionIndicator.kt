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
import kotlinx.coroutines.delay

/**
 * Auto-reconnection states for visual feedback
 */
enum class ReconnectionState {
    IDLE,           // Not attempting reconnection
    ATTEMPTING,     // Currently attempting to reconnect
    SUCCESS,        // Reconnection successful
    FAILED          // Reconnection failed
}

/**
 * Auto-reconnection indicator with enhanced visual feedback
 */
@Composable
fun AutoReconnectionIndicator(
    reconnectionState: ReconnectionState,
    attemptCount: Int = 0,
    maxAttempts: Int = 5,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Only show indicator when reconnecting or showing result
    if (reconnectionState == ReconnectionState.IDLE) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "reconnection_animation")
    
    // Pulsing animation for attempting state
    val attemptingPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reconnectionState == ReconnectionState.ATTEMPTING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "attempting_pulse"
    )
    
    // Success glow animation
    val successGlow by infiniteTransition.animateFloat(
        initialValue = if (reconnectionState == ReconnectionState.SUCCESS) 0.4f else 0f,
        targetValue = if (reconnectionState == ReconnectionState.SUCCESS) 0.8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "success_glow"
    )
    
    // Failure shake animation
    val failureShake by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reconnectionState == ReconnectionState.FAILED) 2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "failure_shake"
    )
    
    // Auto-dismiss success state after 3 seconds
    LaunchedEffect(reconnectionState) {
        if (reconnectionState == ReconnectionState.SUCCESS) {
            delay(3000L)
            onDismiss()
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .scale(attemptingPulse)
            .offset(x = failureShake.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (reconnectionState) {
                ReconnectionState.ATTEMPTING -> MaterialTheme.colorScheme.primaryContainer
                ReconnectionState.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = successGlow)
                ReconnectionState.FAILED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = when (reconnectionState) {
                ReconnectionState.ATTEMPTING -> 8.dp
                ReconnectionState.SUCCESS -> 12.dp
                ReconnectionState.FAILED -> 6.dp
                else -> 0.dp
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status icon with animation
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (reconnectionState) {
                            ReconnectionState.ATTEMPTING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ReconnectionState.SUCCESS -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            ReconnectionState.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (reconnectionState) {
                    ReconnectionState.ATTEMPTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    ReconnectionState.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Reconnection successful",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    ReconnectionState.FAILED -> {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Reconnection failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> Unit
                }
            }
            
            // Status text and progress
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (reconnectionState) {
                        ReconnectionState.ATTEMPTING -> "Reconnecting..."
                        ReconnectionState.SUCCESS -> "Connection Restored"
                        ReconnectionState.FAILED -> "Reconnection Failed"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when (reconnectionState) {
                        ReconnectionState.ATTEMPTING -> MaterialTheme.colorScheme.onPrimaryContainer
                        ReconnectionState.SUCCESS -> Color(0xFF4CAF50)
                        ReconnectionState.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                when (reconnectionState) {
                    ReconnectionState.ATTEMPTING -> {
                        Text(
                            text = "Attempt $attemptCount of $maxAttempts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Progress indicator showing attempt progress
                        LinearProgressIndicator(
                            progress = attemptCount.toFloat() / maxAttempts.toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                    ReconnectionState.SUCCESS -> {
                        Text(
                            text = "Data streaming resumed",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50).copy(alpha = 0.8f)
                        )
                    }
                    ReconnectionState.FAILED -> {
                        Text(
                            text = "Try refreshing or check connection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                    else -> Unit
                }
            }
            
            // Dismiss button for completed states
            if (reconnectionState in setOf(ReconnectionState.SUCCESS, ReconnectionState.FAILED)) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Auto-reconnection manager that tracks reconnection attempts
 */
@Composable
fun rememberAutoReconnectionState(): AutoReconnectionState {
    return remember { AutoReconnectionState() }
}

/**
 * State holder for auto-reconnection logic
 */
class AutoReconnectionState {
    private val _reconnectionState = mutableStateOf(ReconnectionState.IDLE)
    val reconnectionState: State<ReconnectionState> = _reconnectionState
    
    private val _attemptCount = mutableStateOf(0)
    val attemptCount: State<Int> = _attemptCount
    
    private val _maxAttempts = mutableStateOf(5)
    val maxAttempts: State<Int> = _maxAttempts
    
    /**
     * Start a reconnection attempt
     */
    fun startReconnection(maxAttempts: Int = 5) {
        _maxAttempts.value = maxAttempts
        _attemptCount.value = 1
        _reconnectionState.value = ReconnectionState.ATTEMPTING
    }
    
    /**
     * Update attempt count during reconnection
     */
    fun updateAttempt(attempt: Int) {
        _attemptCount.value = attempt
    }
    
    /**
     * Mark reconnection as successful
     */
    fun markSuccess() {
        _reconnectionState.value = ReconnectionState.SUCCESS
    }
    
    /**
     * Mark reconnection as failed
     */
    fun markFailed() {
        _reconnectionState.value = ReconnectionState.FAILED
    }
    
    /**
     * Reset to idle state
     */
    fun reset() {
        _reconnectionState.value = ReconnectionState.IDLE
        _attemptCount.value = 0
    }
}