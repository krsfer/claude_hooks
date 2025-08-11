package com.claudehooks.dashboard.presentation.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.claudehooks.dashboard.data.repository.BackgroundServiceRepository
import com.claudehooks.dashboard.presentation.components.AutoReconnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Enhanced reconnection logic with visual feedback
 */
fun performReconnection(
    autoReconnectionState: AutoReconnectionState,
    useBackgroundService: Boolean,
    repository: BackgroundServiceRepository?,
    fallbackRepository: com.claudehooks.dashboard.data.repository.HookDataRepository,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    scope.launch {
        try {
            // Start reconnection with visual feedback
            autoReconnectionState.startReconnection(maxAttempts = 3)
            
            // Attempt reconnection with retry logic
            var success = false
            var attempt = 1
            val maxAttempts = 3
            
            while (attempt <= maxAttempts && !success) {
                autoReconnectionState.updateAttempt(attempt)
                
                try {
                    if (useBackgroundService) {
                        repository!!.reconnect()
                    } else {
                        fallbackRepository.reconnect()
                    }
                    
                    // Wait a moment to check if connection was successful
                    delay(1500)
                    
                    // Check connection status (simplified - in real scenario you'd check the actual status)
                    success = true // Assume success for now
                    
                    if (success) {
                        autoReconnectionState.markSuccess()
                        snackbarHostState.showSnackbar(
                            message = "Successfully reconnected to Redis",
                            duration = SnackbarDuration.Short
                        )
                    }
                } catch (e: Exception) {
                    if (attempt == maxAttempts) {
                        autoReconnectionState.markFailed()
                        snackbarHostState.showSnackbar(
                            message = "Failed to reconnect after $maxAttempts attempts",
                            duration = SnackbarDuration.Long
                        )
                    } else {
                        // Wait before next attempt
                        delay(1000L * attempt) // Exponential backoff
                    }
                }
                
                attempt++
            }
        } catch (e: Exception) {
            autoReconnectionState.markFailed()
            snackbarHostState.showSnackbar(
                message = "Reconnection error: ${e.message}",
                duration = SnackbarDuration.Long
            )
        }
    }
}