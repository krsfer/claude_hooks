package com.claudehooks.dashboard.data.repository

import android.content.Context
import com.claudehooks.dashboard.data.mapper.HookDataMapper
import com.claudehooks.dashboard.data.model.DashboardStats
import com.claudehooks.dashboard.data.model.HookEvent
import com.claudehooks.dashboard.data.model.HookType
import com.claudehooks.dashboard.data.model.Severity
import com.claudehooks.dashboard.notification.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

class TestHookDataRepository(private val context: Context? = null) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationService = context?.let { NotificationService(it) }
    
    private val _events = MutableStateFlow<List<HookEvent>>(emptyList())
    val events: StateFlow<List<HookEvent>> = _events.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val eventQueue = ConcurrentLinkedQueue<HookEvent>()
    private val maxEvents = 100
    
    init {
        startSimulation()
    }
    
    private fun startSimulation() {
        scope.launch {
            // Add initial events
            addInitialEvents()
            
            // Simulate live events every 5-15 seconds
            while (true) {
                delay(Random.nextLong(5000, 15000))
                addRandomEvent()
            }
        }
    }
    
    private fun addInitialEvents() {
        val now = Instant.now()
        val initialEvents = listOf(
            createEvent(HookType.SESSION_START, "Session Started", "Claude Code session initiated", now.minus(30, ChronoUnit.MINUTES), "sess-test-001"),
            createEvent(HookType.USER_PROMPT_SUBMIT, "User Prompt", "Use real data in Claude Hooks Dashboard app", now.minus(25, ChronoUnit.MINUTES), "sess-test-001"),
            createEvent(HookType.PRE_TOOL_USE, "Tool Use: LS", "Preparing to execute LS", now.minus(24, ChronoUnit.MINUTES), "sess-test-001"),
            createEvent(HookType.POST_TOOL_USE, "Tool Completed: LS", "Tool execution completed in 120ms", now.minus(23, ChronoUnit.MINUTES), "sess-test-001"),
            createEvent(HookType.PRE_TOOL_USE, "Tool Use: Read", "Preparing to execute Read", now.minus(22, ChronoUnit.MINUTES), "sess-test-001"),
            createEvent(HookType.POST_TOOL_USE, "Tool Completed: Read", "Tool execution completed in 85ms", now.minus(21, ChronoUnit.MINUTES), "sess-test-001"),
            createEvent(HookType.PRE_TOOL_USE, "Tool Use: Edit", "Preparing to execute Edit", now.minus(20, ChronoUnit.MINUTES), "sess-test-001"),
            createEvent(HookType.POST_TOOL_USE, "Tool Completed: Edit", "Tool execution completed in 45ms", now.minus(19, ChronoUnit.MINUTES), "sess-test-001"),
            createEvent(HookType.NOTIFICATION, "Notification: Success", "Redis integration completed successfully", now.minus(18, ChronoUnit.MINUTES), "sess-test-001", Severity.INFO),
            createEvent(HookType.SESSION_START, "Session Started", "New Claude Code session initiated", now.minus(10, ChronoUnit.MINUTES), "sess-test-002"),
            createEvent(HookType.USER_PROMPT_SUBMIT, "User Prompt", "Test the real-time data flow", now.minus(9, ChronoUnit.MINUTES), "sess-test-002"),
            createEvent(HookType.PRE_TOOL_USE, "Tool Use: Bash", "Preparing to execute Bash", now.minus(8, ChronoUnit.MINUTES), "sess-test-002"),
            createEvent(HookType.POST_TOOL_USE, "Tool Completed: Bash", "Tool execution completed in 2340ms", now.minus(7, ChronoUnit.MINUTES), "sess-test-002", Severity.WARNING),
        )
        
        initialEvents.forEach { addEvent(it) }
    }
    
    private fun addRandomEvent() {
        val now = Instant.now()
        val sessionId = listOf("sess-test-001", "sess-test-002", "sess-test-003").random()
        
        val events = listOf(
            { createEvent(HookType.USER_PROMPT_SUBMIT, "User Prompt", "Random user query: ${getRandomPrompt()}", now, sessionId) },
            { createEvent(HookType.PRE_TOOL_USE, "Tool Use: ${getRandomTool()}", "Preparing to execute ${getRandomTool()}", now, sessionId) },
            { createEvent(HookType.POST_TOOL_USE, "Tool Completed: ${getRandomTool()}", "Tool execution completed in ${Random.nextInt(50, 3000)}ms", now, sessionId, 
                if (Random.nextFloat() < 0.1) Severity.WARNING else Severity.INFO) },
            { createEvent(HookType.NOTIFICATION, "Notification: ${getRandomNotification()}", getRandomNotificationMessage(), now, sessionId) }
        )
        
        val newEvent = events.random()()
        addEvent(newEvent)
        
        Timber.d("Generated test event: ${newEvent.type} - ${newEvent.title}")
    }
    
    private fun createEvent(
        type: HookType, 
        title: String, 
        message: String, 
        timestamp: Instant, 
        sessionId: String,
        severity: Severity = Severity.INFO
    ): HookEvent {
        return HookEvent(
            type = type,
            title = title,
            message = message,
            timestamp = timestamp,
            source = when (type) {
                HookType.SESSION_START, HookType.NOTIFICATION -> "Claude Code"
                HookType.USER_PROMPT_SUBMIT -> "Claude Code"
                HookType.PRE_TOOL_USE, HookType.POST_TOOL_USE -> "Tool: ${title.substringAfter(": ")}"
                else -> "System"
            },
            severity = severity,
            metadata = mapOf(
                "session_id" to sessionId,
                "sequence" to Random.nextInt(1, 50).toString(),
                "execution_time_ms" to Random.nextInt(50, 2000).toString(),
                "platform" to "darwin",
                "git_status" to "clean"
            )
        )
    }
    
    private fun addEvent(event: HookEvent) {
        eventQueue.offer(event)
        
        while (eventQueue.size > maxEvents) {
            eventQueue.poll()
        }
        
        _events.value = eventQueue.sortedByDescending { it.timestamp }
        
        // Trigger Android notification for notification-type events (if context provided)
        if (event.type == HookType.NOTIFICATION && notificationService != null) {
            try {
                notificationService.showNotificationForHookEvent(event)
                Timber.d("Triggered test notification for event: ${event.title}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to show test notification for event: ${event.id}")
            }
        }
    }
    
    private fun getRandomPrompt(): String {
        return listOf(
            "How do I implement authentication?",
            "Fix this error in my React app",
            "Create a database migration script",
            "Optimize this SQL query",
            "Add unit tests for this component",
            "Refactor this code to be more readable"
        ).random()
    }
    
    private fun getRandomTool(): String {
        return listOf("Read", "Edit", "Write", "Bash", "Grep", "LS", "Glob").random()
    }
    
    private fun getRandomNotification(): String {
        return listOf("Success", "Warning", "Info", "System").random()
    }
    
    private fun getRandomNotificationMessage(): String {
        return listOf(
            "Build completed successfully",
            "Tests are running in background",
            "Code formatting applied",
            "Git commit created",
            "Dependencies updated",
            "Performance metrics collected"
        ).random()
    }
    
    fun getFilteredEvents(
        typeFilter: Set<HookType> = emptySet(),
        sessionId: String? = null,
        limit: Int = 100
    ): Flow<List<HookEvent>> {
        return events.map { allEvents ->
            var filtered = allEvents
            
            if (typeFilter.isNotEmpty()) {
                filtered = filtered.filter { it.type in typeFilter }
            }
            
            if (sessionId != null) {
                filtered = filtered.filter { it.metadata["session_id"] == sessionId }
            }
            
            filtered.take(limit)
        }.distinctUntilChanged()
    }
    
    fun getDashboardStats(): Flow<DashboardStats> {
        return events.map { allEvents ->
            val sessions = allEvents
                .mapNotNull { it.metadata["session_id"] }
                .toSet()
            
            HookDataMapper.calculateDashboardStats(allEvents, sessions)
        }.distinctUntilChanged()
    }
    
    fun getActiveSessionIds(): Flow<List<String>> {
        return events.map { allEvents ->
            allEvents
                .mapNotNull { it.metadata["session_id"] }
                .distinct()
                .sortedByDescending { sessionId ->
                    allEvents.filter { it.metadata["session_id"] == sessionId }
                        .maxOfOrNull { it.timestamp }
                }
        }.distinctUntilChanged()
    }
    
    fun reconnect() {
        scope.launch {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            delay(1000)
            _connectionStatus.value = ConnectionStatus.CONNECTED
        }
    }
    
    fun disconnect() {
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }
    
    fun cleanOldEvents() {
        val cutoffTime = Instant.now().minus(30, ChronoUnit.MINUTES)
        val currentEvents = _events.value
        val filteredEvents = currentEvents.filter { it.timestamp.isAfter(cutoffTime) }
        
        if (filteredEvents.size != currentEvents.size) {
            _events.value = filteredEvents
            eventQueue.clear()
            eventQueue.addAll(filteredEvents)
            Timber.d("Cleaned ${currentEvents.size - filteredEvents.size} old events")
        }
    }
}