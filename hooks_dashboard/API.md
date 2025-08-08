# API Documentation

Complete reference for data models, Redis integration, and hook JSON structure in the Claude Hooks Dashboard.

## Data Models

### Core Domain Models

#### HookData

The primary data model representing a Claude Code hook event.

```kotlin
/**
 * Represents a Claude Code hook event with complete metadata
 * 
 * @param id Unique identifier for the hook event (UUID format)
 * @param hook_type Type of hook from the HookType enumeration
 * @param timestamp ISO 8601 timestamp in Central European Time
 * @param session_id Unique identifier for the Claude Code session
 * @param sequence Sequential number within the session (starts from 1)
 * @param core Core execution data including status and timing
 * @param payload Hook-specific data payload
 * @param context Execution environment context
 */
@Serializable
@Parcelize
@Entity(tableName = "hook_data")
data class HookData(
    @PrimaryKey
    val id: String,
    val hook_type: HookType,
    val timestamp: String,
    val session_id: String,
    val sequence: Long,
    @Embedded(prefix = "core_")
    val core: CoreData,
    @Embedded(prefix = "payload_")
    val payload: PayloadData,
    @Embedded(prefix = "context_")
    val context: ContextData
) : Parcelable
```

#### CoreData

Core execution metadata for all hook types.

```kotlin
/**
 * Core execution data present in all hook events
 * 
 * @param status Current status of the hook execution
 * @param execution_time_ms Execution time in milliseconds (nullable for pending operations)
 */
@Serializable
@Parcelize
data class CoreData(
    val status: HookStatus,
    val execution_time_ms: Long?
) : Parcelable
```

#### PayloadData

Variable payload data specific to each hook type.

```kotlin
/**
 * Hook-specific payload data
 * Different fields are populated based on the hook type
 * 
 * @param prompt User prompt text (for user_prompt_submit hooks)
 * @param tool_name Name of the tool being used (for tool-related hooks)
 * @param tool_input JSON string of tool input parameters
 * @param tool_response JSON string of tool response data
 * @param message General message content (for notifications)
 */
@Serializable
@Parcelize
data class PayloadData(
    val prompt: String? = null,
    val tool_name: String? = null,
    val tool_input: String? = null,
    val tool_response: String? = null,
    val message: String? = null
) : Parcelable
```

#### ContextData

Environment and execution context information.

```kotlin
/**
 * Execution environment context
 * 
 * @param platform Operating system platform
 * @param git_branch Current git branch (if in git repository)
 * @param git_status Git working directory status
 * @param project_type Detected project type
 */
@Serializable
@Parcelize
data class ContextData(
    val platform: Platform,
    val git_branch: String?,
    val git_status: GitStatus?,
    val project_type: ProjectType?
) : Parcelable
```

### Enumerations

#### HookType

All supported Claude Code hook types.

```kotlin
/**
 * Enumeration of all Claude Code hook types
 * 
 * - session_start: New Claude Code session initialization
 * - user_prompt_submit: User input submission to Claude
 * - pre_tool_use: Before tool execution begins
 * - post_tool_use: After tool execution completes
 * - notification: System notifications and alerts
 * - stop_hook: Main session termination
 * - sub_agent_stop_hook: Sub-agent or child session termination
 * - pre_compact: Before context compaction occurs
 */
@Serializable
enum class HookType {
    session_start,
    user_prompt_submit,
    pre_tool_use,
    post_tool_use,
    notification,
    stop_hook,
    sub_agent_stop_hook,
    pre_compact
}
```

#### HookStatus

Execution status enumeration.

```kotlin
/**
 * Status of hook execution
 * 
 * - pending: Operation in progress or queued
 * - success: Operation completed successfully
 * - blocked: Operation blocked by security or validation
 * - error: Operation failed with error
 */
@Serializable
enum class HookStatus {
    pending,
    success,
    blocked,
    error
}
```

#### Platform

Supported operating system platforms.

```kotlin
/**
 * Operating system platforms
 */
@Serializable
enum class Platform {
    darwin,   // macOS
    linux,    // Linux distributions
    windows,  // Windows
    unknown   // Unidentified platform
}
```

#### GitStatus

Git repository status states.

```kotlin
/**
 * Git working directory status
 * 
 * - clean: No uncommitted changes
 * - dirty: Uncommitted changes present
 * - unknown: Git status could not be determined
 */
@Serializable
enum class GitStatus {
    clean,
    dirty,
    unknown
}
```

#### ProjectType

Detected project types based on file analysis.

```kotlin
/**
 * Project type detection based on file patterns and structure
 */
@Serializable
enum class ProjectType {
    react,    // React/Next.js projects
    kotlin,   // Kotlin/JVM projects
    python,   // Python projects
    android,  // Android projects
    ios,      // iOS/Swift projects
    web,      // General web projects
    unknown   // Unidentified project type
}
```

### Analytics Models

#### DashboardStats

Aggregated statistics for the dashboard analytics view.

```kotlin
/**
 * Dashboard statistics and metrics
 * 
 * @param totalHooks Total number of hooks received
 * @param uniqueSessions Number of unique Claude Code sessions
 * @param hookTypeDistribution Distribution of hooks by type
 * @param statusDistribution Distribution of hooks by status
 * @param averageExecutionTime Average execution time in milliseconds
 * @param platformDistribution Distribution of hooks by platform
 * @param projectTypeDistribution Distribution of hooks by project type
 */
data class DashboardStats(
    val totalHooks: Int = 0,
    val uniqueSessions: Int = 0,
    val hookTypeDistribution: Map<HookType, Int> = emptyMap(),
    val statusDistribution: Map<HookStatus, Int> = emptyMap(),
    val averageExecutionTime: Double = 0.0,
    val platformDistribution: Map<Platform, Int> = emptyMap(),
    val projectTypeDistribution: Map<ProjectType, Int> = emptyMap()
)
```

#### FilterCriteria

Filter parameters for querying hooks.

```kotlin
/**
 * Filtering criteria for hook queries
 * 
 * @param hookTypes Set of hook types to include (empty = all)
 * @param statuses Set of statuses to include (empty = all)
 * @param startTime Start of time range (ISO 8601)
 * @param endTime End of time range (ISO 8601)
 * @param sessionIds Set of session IDs to include (empty = all)
 * @param platforms Set of platforms to include (empty = all)
 * @param searchQuery Text search across prompt and message fields
 */
data class FilterCriteria(
    val hookTypes: Set<HookType> = emptySet(),
    val statuses: Set<HookStatus> = emptySet(),
    val startTime: String? = null,
    val endTime: String? = null,
    val sessionIds: Set<String> = emptySet(),
    val platforms: Set<Platform> = emptySet(),
    val searchQuery: String = ""
)
```

## Redis Integration

### Connection Configuration

#### RedisConfig

Configuration for Redis Cloud connection.

```kotlin
/**
 * Redis connection configuration
 * 
 * @param host Redis server hostname
 * @param port Redis server port
 * @param password Authentication password (optional)
 * @param useTls Enable TLS encryption
 * @param channel Pub/sub channel name
 * @param certPath Path to TLS certificates directory
 */
data class RedisConfig(
    val host: String,
    val port: Int,
    val password: String?,
    val useTls: Boolean = true,
    val channel: String = "hooksdata",
    val certPath: String? = null
) {
    companion object {
        /**
         * Default configuration for Claude Hooks Redis instance
         */
        fun fromEnvironment(): RedisConfig = RedisConfig(
            host = "redis-18773.c311.eu-central-1-1.ec2.redns.redis-cloud.com",
            port = 18773,
            password = System.getenv("REDIS_PASSWORD"),
            useTls = true,
            channel = "hooksdata",
            certPath = "${System.getProperty("user.home")}/.redis/certs/"
        )
    }
}
```

### Redis Service Interface

```kotlin
/**
 * Redis service for pub/sub communication
 */
interface RedisService {
    
    /**
     * Start listening for hook events on the configured channel
     * @throws ConnectionException if connection fails
     */
    suspend fun startListening()
    
    /**
     * Stop listening and close connection
     */
    suspend fun stopListening()
    
    /**
     * Flow of incoming hook events
     */
    val hookEvents: SharedFlow<HookData>
    
    /**
     * Connection state
     */
    val connectionState: StateFlow<ConnectionState>
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
```

### Message Format

Redis messages are published as JSON strings on the "hooksdata" channel.

## Hook JSON Examples

### session_start Hook

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "hook_type": "session_start",
  "timestamp": "2025-01-10T14:30:00.000+01:00",
  "session_id": "session_123456789",
  "sequence": 1,
  "core": {
    "status": "success",
    "execution_time_ms": 150
  },
  "payload": {
    "prompt": null,
    "tool_name": null,
    "tool_input": null,
    "tool_response": null,
    "message": "Claude Code session started"
  },
  "context": {
    "platform": "darwin",
    "git_branch": "feature/dashboard",
    "git_status": "clean",
    "project_type": "android"
  }
}
```

### user_prompt_submit Hook

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "hook_type": "user_prompt_submit",
  "timestamp": "2025-01-10T14:30:05.250+01:00",
  "session_id": "session_123456789",
  "sequence": 2,
  "core": {
    "status": "success",
    "execution_time_ms": 45
  },
  "payload": {
    "prompt": "Create a new Android activity for user settings",
    "tool_name": null,
    "tool_input": null,
    "tool_response": null,
    "message": null
  },
  "context": {
    "platform": "darwin",
    "git_branch": "feature/dashboard",
    "git_status": "dirty",
    "project_type": "android"
  }
}
```

### pre_tool_use Hook

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "hook_type": "pre_tool_use",
  "timestamp": "2025-01-10T14:30:07.100+01:00",
  "session_id": "session_123456789",
  "sequence": 3,
  "core": {
    "status": "pending",
    "execution_time_ms": null
  },
  "payload": {
    "prompt": null,
    "tool_name": "Write",
    "tool_input": "{\"file_path\":\"/path/to/SettingsActivity.kt\",\"content\":\"class SettingsActivity...\"}",
    "tool_response": null,
    "message": null
  },
  "context": {
    "platform": "darwin",
    "git_branch": "feature/dashboard",
    "git_status": "dirty",
    "project_type": "android"
  }
}
```

### post_tool_use Hook

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "hook_type": "post_tool_use",
  "timestamp": "2025-01-10T14:30:08.350+01:00",
  "session_id": "session_123456789",
  "sequence": 4,
  "core": {
    "status": "success",
    "execution_time_ms": 1250
  },
  "payload": {
    "prompt": null,
    "tool_name": "Write",
    "tool_input": "{\"file_path\":\"/path/to/SettingsActivity.kt\",\"content\":\"class SettingsActivity...\"}",
    "tool_response": "{\"success\":true,\"file_created\":true}",
    "message": null
  },
  "context": {
    "platform": "darwin",
    "git_branch": "feature/dashboard",
    "git_status": "dirty",
    "project_type": "android"
  }
}
```

### notification Hook

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440004",
  "hook_type": "notification",
  "timestamp": "2025-01-10T14:30:10.000+01:00",
  "session_id": "session_123456789",
  "sequence": 5,
  "core": {
    "status": "success",
    "execution_time_ms": 10
  },
  "payload": {
    "prompt": null,
    "tool_name": null,
    "tool_input": null,
    "tool_response": null,
    "message": "File created successfully: SettingsActivity.kt"
  },
  "context": {
    "platform": "darwin",
    "git_branch": "feature/dashboard",
    "git_status": "dirty",
    "project_type": "android"
  }
}
```

### stop_hook Hook

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440005",
  "hook_type": "stop_hook",
  "timestamp": "2025-01-10T14:35:00.000+01:00",
  "session_id": "session_123456789",
  "sequence": 6,
  "core": {
    "status": "success",
    "execution_time_ms": 25
  },
  "payload": {
    "prompt": null,
    "tool_name": null,
    "tool_input": null,
    "tool_response": null,
    "message": "Session ended by user"
  },
  "context": {
    "platform": "darwin",
    "git_branch": "feature/dashboard",
    "git_status": "clean",
    "project_type": "android"
  }
}
```

### sub_agent_stop_hook Hook

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440006",
  "hook_type": "sub_agent_stop_hook",
  "timestamp": "2025-01-10T14:32:15.500+01:00",
  "session_id": "session_123456789",
  "sequence": 7,
  "core": {
    "status": "success",
    "execution_time_ms": 75
  },
  "payload": {
    "prompt": null,
    "tool_name": null,
    "tool_input": null,
    "tool_response": null,
    "message": "Sub-agent task completed"
  },
  "context": {
    "platform": "darwin",
    "git_branch": "feature/dashboard",
    "git_status": "dirty",
    "project_type": "android"
  }
}
```

### pre_compact Hook

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440007",
  "hook_type": "pre_compact",
  "timestamp": "2025-01-10T14:33:45.200+01:00",
  "session_id": "session_123456789",
  "sequence": 8,
  "core": {
    "status": "pending",
    "execution_time_ms": null
  },
  "payload": {
    "prompt": null,
    "tool_name": null,
    "tool_input": null,
    "tool_response": null,
    "message": "Context size: 45000 tokens, compaction threshold reached"
  },
  "context": {
    "platform": "darwin",
    "git_branch": "feature/dashboard",
    "git_status": "dirty",
    "project_type": "android"
  }
}
```

## Database Schema

### Room Entity Mapping

The `HookData` entity is mapped to the following SQLite table structure:

```sql
CREATE TABLE hook_data (
    id TEXT PRIMARY KEY NOT NULL,
    hook_type TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    session_id TEXT NOT NULL,
    sequence INTEGER NOT NULL,
    
    -- CoreData fields with 'core_' prefix
    core_status TEXT NOT NULL,
    core_execution_time_ms INTEGER,
    
    -- PayloadData fields with 'payload_' prefix
    payload_prompt TEXT,
    payload_tool_name TEXT,
    payload_tool_input TEXT,
    payload_tool_response TEXT,
    payload_message TEXT,
    
    -- ContextData fields with 'context_' prefix
    context_platform TEXT NOT NULL,
    context_git_branch TEXT,
    context_git_status TEXT,
    context_project_type TEXT
);

-- Indices for common queries
CREATE INDEX idx_hook_data_timestamp ON hook_data(timestamp);
CREATE INDEX idx_hook_data_hook_type ON hook_data(hook_type);
CREATE INDEX idx_hook_data_session_id ON hook_data(session_id);
CREATE INDEX idx_hook_data_status ON hook_data(core_status);
CREATE INDEX idx_hook_data_composite ON hook_data(hook_type, timestamp);
```

## Data Access Patterns

### Repository Interface

```kotlin
interface HookRepository {
    /**
     * Observe all hooks with real-time updates
     */
    fun observeHooks(): Flow<List<HookData>>
    
    /**
     * Get filtered hooks based on criteria
     */
    suspend fun getFilteredHooks(criteria: FilterCriteria): List<HookData>
    
    /**
     * Insert a new hook (from Redis or manual entry)
     */
    suspend fun insertHook(hook: HookData)
    
    /**
     * Get hooks for a specific session
     */
    suspend fun getSessionHooks(sessionId: String): List<HookData>
    
    /**
     * Get dashboard statistics
     */
    suspend fun getDashboardStats(): DashboardStats
    
    /**
     * Export filtered hooks to CSV format
     */
    suspend fun exportToCSV(criteria: FilterCriteria): String
    
    /**
     * Clear old hooks (data retention)
     */
    suspend fun clearHooksOlderThan(timestamp: String)
    
    /**
     * Start Redis connection and begin receiving hooks
     */
    suspend fun startRedisConnection()
    
    /**
     * Stop Redis connection
     */
    suspend fun stopRedisConnection()
}
```

### Query Examples

#### Complex Filtering Query

```kotlin
@Query("""
    SELECT * FROM hook_data 
    WHERE (:hookTypes IS NULL OR hook_type IN (:hookTypes))
    AND (:statuses IS NULL OR core_status IN (:statuses))
    AND (:sessionIds IS NULL OR session_id IN (:sessionIds))
    AND (:platforms IS NULL OR context_platform IN (:platforms))
    AND (
        :searchQuery IS NULL OR :searchQuery = '' OR
        payload_prompt LIKE '%' || :searchQuery || '%' OR
        payload_message LIKE '%' || :searchQuery || '%' OR
        payload_tool_name LIKE '%' || :searchQuery || '%'
    )
    AND timestamp BETWEEN :startTime AND :endTime
    ORDER BY timestamp DESC
    LIMIT :limit OFFSET :offset
""")
suspend fun getFilteredHooks(
    hookTypes: List<String>?,
    statuses: List<String>?,
    sessionIds: List<String>?,
    platforms: List<String>?,
    searchQuery: String?,
    startTime: String,
    endTime: String,
    limit: Int,
    offset: Int
): List<HookData>
```

#### Statistics Aggregation Query

```kotlin
@Query("""
    SELECT 
        hook_type,
        COUNT(*) as count,
        AVG(CASE WHEN core_execution_time_ms IS NOT NULL THEN core_execution_time_ms END) as avg_execution_time,
        SUM(CASE WHEN core_status = 'success' THEN 1 ELSE 0 END) as success_count,
        SUM(CASE WHEN core_status = 'error' THEN 1 ELSE 0 END) as error_count
    FROM hook_data 
    WHERE timestamp >= :startTime
    GROUP BY hook_type
    ORDER BY count DESC
""")
suspend fun getHookTypeStatistics(startTime: String): List<HookTypeStats>
```

## Error Handling

### Error Types

```kotlin
sealed class HookDataException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ParseException(message: String, cause: Throwable) : HookDataException(message, cause)
    class ConnectionException(message: String, cause: Throwable) : HookDataException(message, cause)
    class DatabaseException(message: String, cause: Throwable) : HookDataException(message, cause)
    class ExportException(message: String, cause: Throwable) : HookDataException(message, cause)
}
```

### JSON Parsing with Error Recovery

```kotlin
class HookDataParser @Inject constructor(
    private val gson: Gson
) {
    fun parseHookData(json: String): Result<HookData> = try {
        val hookData = gson.fromJson(json, HookData::class.java)
        Result.Success(hookData)
    } catch (e: JsonSyntaxException) {
        Timber.w(e, "Failed to parse hook data: $json")
        Result.Error(HookDataException.ParseException("Invalid JSON format", e))
    } catch (e: Exception) {
        Timber.e(e, "Unexpected error parsing hook data")
        Result.Error(HookDataException.ParseException("Unexpected parsing error", e))
    }
}
```

## Data Validation

### Model Validation

```kotlin
fun HookData.validate(): List<ValidationError> {
    val errors = mutableListOf<ValidationError>()
    
    if (id.isBlank()) errors.add(ValidationError.EMPTY_ID)
    if (session_id.isBlank()) errors.add(ValidationError.EMPTY_SESSION_ID)
    if (sequence < 1) errors.add(ValidationError.INVALID_SEQUENCE)
    if (!isValidTimestamp(timestamp)) errors.add(ValidationError.INVALID_TIMESTAMP)
    
    // Validate payload based on hook type
    when (hook_type) {
        HookType.user_prompt_submit -> {
            if (payload.prompt.isNullOrBlank()) {
                errors.add(ValidationError.MISSING_PROMPT)
            }
        }
        HookType.pre_tool_use, HookType.post_tool_use -> {
            if (payload.tool_name.isNullOrBlank()) {
                errors.add(ValidationError.MISSING_TOOL_NAME)
            }
        }
        // ... other validations
    }
    
    return errors
}

enum class ValidationError {
    EMPTY_ID,
    EMPTY_SESSION_ID,
    INVALID_SEQUENCE,
    INVALID_TIMESTAMP,
    MISSING_PROMPT,
    MISSING_TOOL_NAME
}
```

---

*This API documentation provides comprehensive reference material for all data structures, Redis integration patterns, and JSON formats used in the Claude Hooks Dashboard. For implementation examples, refer to the source code and accompanying documentation.*