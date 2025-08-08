package com.claudehooks.dashboard.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.data.model.HookType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChips(
    selectedTypes: Set<HookType>,
    onTypeToggle: (HookType) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clear all button
        if (selectedTypes.isNotEmpty()) {
            AssistChip(
                onClick = onClearAll,
                label = { Text("Clear") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        
        // Type filter chips
        HookType.values().forEach { type ->
            FilterChip(
                selected = type in selectedTypes,
                onClick = { onTypeToggle(type) },
                label = { Text(formatTypeName(type)) }
            )
        }
    }
}

private fun formatTypeName(type: HookType): String = when (type) {
    HookType.SESSION_START -> "Session"
    HookType.USER_PROMPT_SUBMIT -> "Prompt"
    HookType.PRE_TOOL_USE -> "Pre-Tool"
    HookType.POST_TOOL_USE -> "Post-Tool"
    HookType.NOTIFICATION -> "Notification"
    HookType.STOP_HOOK -> "Stop"
    HookType.SUB_AGENT_STOP_HOOK -> "Sub-Agent"
    HookType.PRE_COMPACT -> "Compact"
    // Legacy types
    HookType.API_CALL -> "API"
    HookType.DATABASE -> "Database"
    HookType.FILE_SYSTEM -> "Files"
    HookType.NETWORK -> "Network"
    HookType.SECURITY -> "Security"
    HookType.PERFORMANCE -> "Performance"
    HookType.ERROR -> "Errors"
    HookType.CUSTOM -> "Custom"
}