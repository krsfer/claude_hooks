package com.claudehooks.dashboard.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity

/**
 * Advanced filter bar with search, presets, and session filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTypes: Set<HookType>,
    onTypeToggle: (HookType) -> Unit,
    selectedSeverities: Set<Severity>,
    onSeverityToggle: (Severity) -> Unit,
    selectedSessions: Set<String>,
    onSessionToggle: (String) -> Unit,
    availableSessions: List<String>,
    activePreset: FilterPreset?,
    onPresetApply: (FilterPreset) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Bar and Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search events, messages, tools...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            
            // Expand/Collapse button
            IconButton(
                onClick = { isExpanded = !isExpanded }
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse filters" else "Expand filters",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Quick presets button
            IconButton(
                onClick = { showPresets = !showPresets }
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Filter presets",
                    tint = if (activePreset != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Clear all button
            if (hasActiveFilters(searchQuery, selectedTypes, selectedSeverities, selectedSessions)) {
                IconButton(
                    onClick = onClearAll
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAltOff,
                        contentDescription = "Clear all filters",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        // Filter Presets (collapsible)
        AnimatedVisibility(
            visible = showPresets,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FilterPresets(
                activePreset = activePreset,
                onPresetApply = onPresetApply,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Expanded Filters
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ExpandedFilters(
                selectedTypes = selectedTypes,
                onTypeToggle = onTypeToggle,
                selectedSeverities = selectedSeverities,
                onSeverityToggle = onSeverityToggle,
                selectedSessions = selectedSessions,
                onSessionToggle = onSessionToggle,
                availableSessions = availableSessions
            )
        }
        
        // Active filters summary
        if (hasActiveFilters(searchQuery, selectedTypes, selectedSeverities, selectedSessions)) {
            ActiveFiltersSummary(
                searchQuery = searchQuery,
                selectedTypes = selectedTypes,
                selectedSeverities = selectedSeverities,
                selectedSessions = selectedSessions,
                activePreset = activePreset,
                onRemoveSearch = { onSearchQueryChange("") },
                onRemoveType = onTypeToggle,
                onRemoveSeverity = onSeverityToggle,
                onRemoveSession = onSessionToggle,
                onClearPreset = { onPresetApply(FilterPreset.None) }
            )
        }
    }
}

/**
 * Filter presets for common debugging workflows
 */
@Composable
private fun FilterPresets(
    activePreset: FilterPreset?,
    onPresetApply: (FilterPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FilterPreset.getAllPresets()) { preset ->
                PresetChip(
                    preset = preset,
                    isActive = activePreset == preset,
                    onClick = { onPresetApply(preset) }
                )
            }
        }
    }
}

/**
 * Preset chip component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetChip(
    preset: FilterPreset,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = preset.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(preset.name)
            }
        },
        selected = isActive,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = preset.color.copy(alpha = 0.2f),
            selectedLabelColor = preset.color,
            selectedLeadingIconColor = preset.color
        )
    )
}

/**
 * Expanded filters section
 */
@Composable
private fun ExpandedFilters(
    selectedTypes: Set<HookType>,
    onTypeToggle: (HookType) -> Unit,
    selectedSeverities: Set<Severity>,
    onSeverityToggle: (Severity) -> Unit,
    selectedSessions: Set<String>,
    onSessionToggle: (String) -> Unit,
    availableSessions: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Severity filters
        FilterSection(
            title = "Severity",
            icon = Icons.Default.PriorityHigh
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Severity.values()) { severity ->
                    SeverityFilterChip(
                        severity = severity,
                        isSelected = severity in selectedSeverities,
                        onClick = { onSeverityToggle(severity) }
                    )
                }
            }
        }
        
        // Type filters
        FilterSection(
            title = "Event Types",
            icon = Icons.Default.Category
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HookType.values().take(8)) { type ->
                    TypeFilterChip(
                        type = type,
                        isSelected = type in selectedTypes,
                        onClick = { onTypeToggle(type) }
                    )
                }
            }
        }
        
        // Session filters (if available)
        if (availableSessions.isNotEmpty()) {
            FilterSection(
                title = "Sessions",
                icon = Icons.Default.Timeline
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableSessions.take(5)) { session ->
                        SessionFilterChip(
                            sessionId = session,
                            isSelected = session in selectedSessions,
                            onClick = { onSessionToggle(session) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Filter section with title and icon
 */
@Composable
private fun FilterSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}

/**
 * Severity filter chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeverityFilterChip(
    severity: Severity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (severity) {
        Severity.CRITICAL -> Color(0xFFD32F2F)
        Severity.ERROR -> Color(0xFFE53935)
        Severity.WARNING -> Color(0xFFFF9800)
        Severity.INFO -> Color(0xFF1976D2)
    }
    
    FilterChip(
        onClick = onClick,
        label = { Text(severity.name) },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLabelColor = color
        )
    )
}

/**
 * Type filter chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeFilterChip(
    type: HookType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(type.name.replace('_', ' ')) },
        selected = isSelected
    )
}

/**
 * Session filter chip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionFilterChip(
    sessionId: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(sessionId.takeLast(8)) },
        selected = isSelected,
        leadingIcon = {
            Icon(
                Icons.Default.AccountTree,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

/**
 * Active filters summary
 */
@Composable
private fun ActiveFiltersSummary(
    searchQuery: String,
    selectedTypes: Set<HookType>,
    selectedSeverities: Set<Severity>,
    selectedSessions: Set<String>,
    activePreset: FilterPreset?,
    onRemoveSearch: () -> Unit,
    onRemoveType: (HookType) -> Unit,
    onRemoveSeverity: (Severity) -> Unit,
    onRemoveSession: (String) -> Unit,
    onClearPreset: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        // Active preset
        if (activePreset != null && activePreset != FilterPreset.None) {
            item {
                ActiveFilterChip(
                    label = "Preset: ${activePreset.name}",
                    onRemove = onClearPreset,
                    color = activePreset.color
                )
            }
        }
        
        // Search query
        if (searchQuery.isNotEmpty()) {
            item {
                ActiveFilterChip(
                    label = "Search: \"$searchQuery\"",
                    onRemove = onRemoveSearch
                )
            }
        }
        
        // Severities
        items(selectedSeverities.toList()) { severity ->
            ActiveFilterChip(
                label = severity.name,
                onRemove = { onRemoveSeverity(severity) }
            )
        }
        
        // Types
        items(selectedTypes.toList()) { type ->
            ActiveFilterChip(
                label = type.name.replace('_', ' '),
                onRemove = { onRemoveType(type) }
            )
        }
        
        // Sessions
        items(selectedSessions.toList()) { session ->
            ActiveFilterChip(
                label = "Session: ${session.takeLast(8)}",
                onRemove = { onRemoveSession(session) }
            )
        }
    }
}

/**
 * Active filter chip with remove button
 */
@Composable
private fun ActiveFilterChip(
    label: String,
    onRemove: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove filter",
                    modifier = Modifier.size(12.dp),
                    tint = color
                )
            }
        }
    }
}

/**
 * Check if any filters are active
 */
private fun hasActiveFilters(
    searchQuery: String,
    selectedTypes: Set<HookType>,
    selectedSeverities: Set<Severity>,
    selectedSessions: Set<String>
): Boolean {
    return searchQuery.isNotEmpty() || 
           selectedTypes.isNotEmpty() || 
           selectedSeverities.isNotEmpty() || 
           selectedSessions.isNotEmpty()
}

/**
 * Filter presets for common debugging workflows
 */
sealed class FilterPreset(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val types: Set<HookType> = emptySet(),
    val severities: Set<Severity> = emptySet()
) {
    object None : FilterPreset("None", Icons.Default.Clear, Color.Transparent)
    
    object CriticalIssues : FilterPreset(
        name = "Critical Issues",
        icon = Icons.Default.Warning,
        color = Color(0xFFD32F2F),
        severities = setOf(Severity.CRITICAL, Severity.ERROR)
    )
    
    object RecentActivity : FilterPreset(
        name = "Recent Activity",
        icon = Icons.Default.Schedule,
        color = Color(0xFF1976D2),
        types = setOf(HookType.SESSION_START, HookType.USER_PROMPT_SUBMIT, HookType.PRE_TOOL_USE, HookType.POST_TOOL_USE)
    )
    
    object ToolUsage : FilterPreset(
        name = "Tool Usage",
        icon = Icons.Default.Build,
        color = Color(0xFF388E3C),
        types = setOf(HookType.PRE_TOOL_USE, HookType.POST_TOOL_USE)
    )
    
    object SessionFlow : FilterPreset(
        name = "Session Flow",
        icon = Icons.Default.Timeline,
        color = Color(0xFF7B1FA2),
        types = setOf(HookType.SESSION_START, HookType.USER_PROMPT_SUBMIT, HookType.STOP_HOOK)
    )
    
    object Notifications : FilterPreset(
        name = "Notifications",
        icon = Icons.Default.Notifications,
        color = Color(0xFFFF9800),
        types = setOf(HookType.NOTIFICATION)
    )
    
    companion object {
        fun getAllPresets(): List<FilterPreset> = listOf(
            CriticalIssues,
            RecentActivity,
            ToolUsage,
            SessionFlow,
            Notifications
        )
    }
}