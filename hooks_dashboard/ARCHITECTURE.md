# Architecture Documentation

Comprehensive technical architecture and design patterns for the Claude Hooks Dashboard.

## Architecture Overview

The Claude Hooks Dashboard follows **Clean Architecture** principles with clear separation of concerns across three main layers:

```
┌─────────────────────────────────────────┐
│                 PRESENTATION            │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │    UI       │  │   ViewModels    │   │
│  │ (Compose)   │  │   (MVVM)       │   │
│  └─────────────┘  └─────────────────┘   │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│                 DOMAIN                  │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │  Use Cases  │  │  Repository     │   │
│  │             │  │  Interfaces     │   │
│  └─────────────┘  └─────────────────┘   │
│  ┌─────────────────────────────────────┐ │
│  │          Domain Models              │ │
│  └─────────────────────────────────────┘ │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│                  DATA                   │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │  Repository │  │   Data Sources  │   │
│  │    Impl     │  │ (Local/Remote)  │   │
│  └─────────────┘  └─────────────────┘   │
└─────────────────────────────────────────┘
```

## Layer Details

### Presentation Layer

**MVVM Architecture with Jetpack Compose**

```kotlin
/**
 * Main dashboard screen composable
 * Observes ViewModel state and recomposes on changes
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn {
        items(uiState.hooks) { hook ->
            HookCard(
                hook = hook,
                onHookClick = viewModel::selectHook
            )
        }
    }
}
```

**Key Components:**
- **MainActivity**: Single activity host for Compose UI
- **DashboardScreen**: Main interface displaying hook stream
- **HookCard**: Reusable component for hook visualization
- **FilterBottomSheet**: Advanced filtering interface
- **StatisticsScreen**: Analytics and metrics display

### Domain Layer

**Business Logic & Use Cases**

```kotlin
/**
 * Core domain model representing a Claude Code hook event
 * @param id Unique identifier for the hook event
 * @param hook_type Type of hook (session_start, pre_tool_use, etc.)
 * @param core Core execution data including status and timing
 * @param payload Hook-specific data (prompts, tool usage, etc.)
 * @param context Execution context (platform, git status, project type)
 */
@Entity(tableName = "hook_data")
data class HookData(
    @PrimaryKey val id: String,
    val hook_type: HookType,
    val timestamp: String,
    val session_id: String,
    val sequence: Long,
    @Embedded(prefix = "core_") val core: CoreData,
    @Embedded(prefix = "payload_") val payload: PayloadData,
    @Embedded(prefix = "context_") val context: ContextData
)
```

**Repository Pattern:**
```kotlin
interface HookRepository {
    fun observeHooks(): Flow<List<HookData>>
    suspend fun insertHook(hook: HookData)
    suspend fun getFilteredHooks(criteria: FilterCriteria): List<HookData>
    suspend fun exportToCSV(): String
}
```

### Data Layer

**Repository Implementation with Multiple Data Sources**

```kotlin
@Singleton
class HookRepositoryImpl @Inject constructor(
    private val localDataSource: HookDao,
    private val remoteDataSource: RedisService
) : HookRepository {
    
    override fun observeHooks(): Flow<List<HookData>> = 
        localDataSource.observeAllHooks()
    
    override suspend fun insertHook(hook: HookData) {
        localDataSource.insertHook(hook)
    }
}
```

## Data Flow Architecture

### Real-time Data Pipeline

```
Redis Cloud ──► RedisService ──► Repository ──► ViewModel ──► UI
     │                │             │             │          │
     │                │             │             │          │
   TLS/SSL         Pub/Sub        Room DB      StateFlow   Compose
  Connection      Subscribe     Local Cache   Live Data   Recomposition
```

**Flow Description:**
1. **Redis Connection**: TLS-secured connection to Redis Cloud instance
2. **Pub/Sub Subscription**: Listen to "hooksdata" channel for real-time events
3. **Data Processing**: Parse JSON payloads into domain models
4. **Local Storage**: Persist data in Room database for offline access
5. **Reactive Updates**: Emit changes via Flow to ViewModels
6. **UI Updates**: Compose automatically recomposes on state changes

### Data Processing Pipeline

```kotlin
/**
 * Redis service handling pub/sub connection and data parsing
 */
@Singleton
class RedisService @Inject constructor(
    private val redisClient: RedisClient,
    private val gson: Gson
) {
    private val _hookEvents = MutableSharedFlow<HookData>()
    val hookEvents: SharedFlow<HookData> = _hookEvents.asSharedFlow()
    
    suspend fun startListening() {
        val pubSub = redisClient.connectPubSub()
        pubSub.subscribe("hooksdata") { channel, message ->
            try {
                val hookData = gson.fromJson(message, HookData::class.java)
                _hookEvents.tryEmit(hookData)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse hook data: $message")
            }
        }
    }
}
```

## Dependency Injection Architecture

**Hilt Module Structure**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideRedisClient(): RedisClient {
        val config = RedisConfig.fromEnvironment()
        return RedisClient.create(
            RedisURI.Builder
                .redis(config.host, config.port)
                .withSsl(config.useTls)
                .build()
        )
    }
    
    @Provides
    @Singleton
    fun provideRedisService(
        client: RedisClient,
        gson: Gson
    ): RedisService = RedisService(client, gson)
}
```

### Module Dependency Graph

```
ApplicationComponent
├── NetworkModule
│   ├── RedisClient
│   ├── RedisService
│   └── Gson
├── DatabaseModule
│   ├── HookDatabase
│   └── HookDao
├── RepositoryModule
│   └── HookRepository
└── ViewModelModule
    └── DashboardViewModel
```

## Concurrency & Threading Model

### Coroutines Architecture

```kotlin
class DashboardViewModel @Inject constructor(
    private val repository: HookRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        // Observe hooks in ViewModelScope
        viewModelScope.launch {
            repository.observeHooks()
                .flowOn(Dispatchers.IO)
                .collect { hooks ->
                    _uiState.update { it.copy(hooks = hooks) }
                }
        }
        
        // Start Redis connection in ApplicationScope (survives ViewModel)
        applicationScope.launch {
            repository.startRedisConnection()
        }
    }
}
```

**Thread Assignment:**
- **Main Thread**: UI updates and Compose recomposition
- **IO Dispatcher**: Database operations and network calls
- **Default Dispatcher**: Data processing and JSON parsing
- **Application Scope**: Long-running operations like Redis connection

## Database Architecture

### Room Database Schema

```kotlin
@Database(
    entities = [HookData::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HookDatabase : RoomDatabase() {
    abstract fun hookDao(): HookDao
}
```

### DAO with Complex Queries

```kotlin
@Dao
interface HookDao {
    
    @Query("SELECT * FROM hook_data ORDER BY timestamp DESC")
    fun observeAllHooks(): Flow<List<HookData>>
    
    @Query("""
        SELECT * FROM hook_data 
        WHERE (:hookType IS NULL OR hook_type = :hookType)
        AND (:status IS NULL OR core_status = :status)
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
        LIMIT :limit
    """)
    suspend fun getFilteredHooks(
        hookType: HookType?,
        status: HookStatus?,
        startTime: String,
        endTime: String,
        limit: Int
    ): List<HookData>
    
    @Query("""
        SELECT hook_type, COUNT(*) as count
        FROM hook_data 
        GROUP BY hook_type
    """)
    suspend fun getHookTypeDistribution(): List<HookTypeCount>
}
```

## State Management Architecture

### UI State Pattern

```kotlin
data class DashboardUiState(
    val hooks: List<HookData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterCriteria: FilterCriteria = FilterCriteria(),
    val searchQuery: String = "",
    val selectedHook: HookData? = null,
    val statistics: DashboardStats = DashboardStats()
)
```

### State Updates with Immutability

```kotlin
private fun updateFilterCriteria(newCriteria: FilterCriteria) {
    _uiState.update { currentState ->
        currentState.copy(
            filterCriteria = newCriteria,
            isLoading = true
        )
    }
    
    viewModelScope.launch {
        try {
            val filteredHooks = repository.getFilteredHooks(newCriteria)
            _uiState.update { it.copy(hooks = filteredHooks, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message, isLoading = false) }
        }
    }
}
```

## Network Architecture

### Redis TLS Configuration

```kotlin
data class RedisConfig(
    val host: String,
    val port: Int,
    val password: String?,
    val useTls: Boolean = true,
    val channel: String = "hooksdata",
    val certPath: String? = null
) {
    
    /**
     * Creates Redis URI with TLS configuration
     * Supports certificate-based authentication
     */
    fun createRedisURI(): RedisURI {
        return RedisURI.Builder
            .redis(host, port)
            .withSsl(useTls)
            .apply {
                password?.let { withPassword(it.toCharArray()) }
                certPath?.let { withClientCertificate(File("$it/client.crt")) }
            }
            .build()
    }
}
```

### Connection Management

```kotlin
class ConnectionManager @Inject constructor() {
    
    private var connection: StatefulRedisPubSubConnection<String, String>? = null
    private val reconnectDelay = 5000L // 5 seconds
    
    suspend fun ensureConnection(): StatefulRedisPubSubConnection<String, String> {
        return connection?.takeIf { it.isOpen } ?: run {
            createNewConnection().also { connection = it }
        }
    }
    
    private suspend fun createNewConnection() = withContext(Dispatchers.IO) {
        redisClient.connectPubSub().apply {
            addListener(object : RedisPubSubListener<String, String> {
                override fun subscribed(channel: String, count: Long) {
                    Timber.d("Subscribed to channel: $channel")
                }
                
                override fun message(channel: String, message: String) {
                    handleMessage(message)
                }
            })
        }
    }
}
```

## Testing Architecture

### Testing Strategy

```kotlin
// Repository Test with Fake Data Sources
@Test
fun `repository observes hooks from local data source`() = runTest {
    val fakeDao = FakeHookDao()
    val repository = HookRepositoryImpl(fakeDao, FakeRedisService())
    
    val testHook = createTestHookData()
    fakeDao.insertHook(testHook)
    
    repository.observeHooks().test {
        val hooks = awaitItem()
        assertThat(hooks).contains(testHook)
    }
}

// ViewModel Test with Mock Repository
@Test
fun `viewModel updates state when hooks are received`() = runTest {
    val mockRepository = mockk<HookRepository>()
    every { mockRepository.observeHooks() } returns flowOf(listOf(testHook))
    
    val viewModel = DashboardViewModel(mockRepository, testScope)
    
    assertThat(viewModel.uiState.value.hooks).contains(testHook)
}

// UI Test with Compose Test Rule
@Test
fun `dashboard screen displays hook cards`() {
    composeTestRule.setContent {
        DashboardScreen(viewModel = testViewModel)
    }
    
    composeTestRule.onNodeWithText("session_start").assertIsDisplayed()
    composeTestRule.onNodeWithTag("hook_card_0").assertExists()
}
```

## Performance Optimizations

### Memory Management

```kotlin
// Paging for large datasets
@Query("SELECT * FROM hook_data ORDER BY timestamp DESC")
fun observeHookssPaged(): PagingSource<Int, HookData>

// Lazy loading with Flow
fun observeHooks(): Flow<List<HookData>> = hookDao.observeAllHooks()
    .distinctUntilChanged()
    .conflate() // Keep only latest emission
```

### Database Optimization

```kotlin
// Indices for common queries
@Entity(
    tableName = "hook_data",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["hook_type"]),
        Index(value = ["session_id"]),
        Index(value = ["hook_type", "timestamp"])
    ]
)
```

### UI Performance

```kotlin
// Stable keys for LazyColumn
@Composable
fun HooksList(hooks: List<HookData>) {
    LazyColumn {
        items(
            items = hooks,
            key = { hook -> hook.id } // Stable key for recomposition
        ) { hook ->
            HookCard(hook = hook)
        }
    }
}

// Remember expensive calculations
@Composable
fun StatisticsView(hooks: List<HookData>) {
    val statistics = remember(hooks) {
        hooks.groupBy { it.hook_type }.mapValues { it.value.size }
    }
    // ... UI code
}
```

## Security Architecture

### Data Protection

```kotlin
// Secure data export
class SecureExportManager @Inject constructor() {
    
    suspend fun exportData(data: List<HookData>): String {
        return withContext(Dispatchers.IO) {
            // Remove sensitive information before export
            val sanitizedData = data.map { hook ->
                hook.copy(
                    payload = hook.payload.copy(
                        prompt = hook.payload.prompt?.sanitize(),
                        tool_input = hook.payload.tool_input?.sanitize()
                    )
                )
            }
            generateCSV(sanitizedData)
        }
    }
    
    private fun String.sanitize(): String {
        // Remove potentially sensitive patterns
        return this.replace(Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""), "[EMAIL]")
                  .replace(Regex("""\d{3}-\d{2}-\d{4}"""), "[SSN]")
    }
}
```

### Network Security

```kotlin
// TLS Certificate Validation
class TlsConfig {
    fun createSslContext(): SslContext {
        return SslContextBuilder.forClient()
            .trustManager(loadTrustStore())
            .keyManager(loadKeyStore())
            .protocols("TLSv1.3", "TLSv1.2")
            .build()
    }
    
    private fun loadTrustStore(): TrustManagerFactory {
        val trustStore = KeyStore.getInstance("PKCS12")
        FileInputStream("~/.redis/certs/ca.crt").use { fis ->
            trustStore.load(fis, null)
        }
        return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(trustStore) }
    }
}
```

## Error Handling Architecture

### Comprehensive Error Strategy

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Repository with error handling
suspend fun getHooks(): Result<List<HookData>> = try {
    Result.Success(hookDao.getAllHooks())
} catch (e: SQLException) {
    Timber.e(e, "Database error")
    Result.Error(DatabaseException("Failed to load hooks", e))
} catch (e: Exception) {
    Timber.e(e, "Unexpected error")
    Result.Error(UnknownException("Unexpected error occurred", e))
}

// UI error handling
@Composable
fun ErrorHandler(error: String?, onRetry: () -> Unit) {
    error?.let { errorMessage ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Error: $errorMessage",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
```

## Monitoring & Observability

### Logging Strategy

```kotlin
// Structured logging with Timber
class ProductionTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.INFO) {
            val logEntry = LogEntry(
                level = priority.toLogLevel(),
                tag = tag ?: "Unknown",
                message = message,
                timestamp = System.currentTimeMillis(),
                thread = Thread.currentThread().name,
                exception = t?.stackTraceToString()
            )
            
            // Send to logging service or local storage
            logService.log(logEntry)
        }
    }
}
```

### Performance Monitoring

```kotlin
// Method execution timing
inline fun <T> measureTimeAndLog(operation: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    return try {
        block()
    } finally {
        val executionTime = System.currentTimeMillis() - startTime
        Timber.d("$operation completed in ${executionTime}ms")
        
        // Track performance metrics
        if (executionTime > SLOW_OPERATION_THRESHOLD) {
            Timber.w("Slow operation detected: $operation took ${executionTime}ms")
        }
    }
}
```

---

*This architecture documentation provides a comprehensive overview of the technical design and implementation patterns used in the Claude Hooks Dashboard. For implementation details, refer to the source code and accompanying documentation.*