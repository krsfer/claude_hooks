# Development Guide

Complete development workflow, testing strategy, and contribution guidelines for the Claude Hooks Dashboard.

## Development Environment Setup

### Prerequisites
- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 17 or later
- **Git**: Latest version with LFS support
- **Redis Tools**: redis-cli for testing connections

### IDE Configuration

#### Android Studio Plugins
Install these recommended plugins:
- **Kotlin Multiplatform Mobile**
- **.ignore**
- **GitToolBox**
- **SonarLint**
- **Detekt**

#### Code Style Configuration
1. Import the project code style:
   ```
   File → Settings → Editor → Code Style → Kotlin
   Import Scheme → IntelliJ IDEA code style XML
   ```

2. Configure Ktlint:
   ```bash
   ./gradlew addKtlintFormatGitPreCommitHook
   ```

#### Debugging Configuration
Set up debug configurations for different scenarios:
- **App Debug**: Standard app debugging
- **Redis Debug**: Debug Redis connection issues
- **Unit Tests**: Run unit tests with debugging
- **Integration Tests**: Debug instrumented tests

### Project Structure Overview

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/claudehooks/dashboard/
│   │   │   ├── data/                    # Data layer
│   │   │   │   ├── local/              # Room database
│   │   │   │   │   ├── HookDatabase.kt
│   │   │   │   │   ├── HookDao.kt
│   │   │   │   │   └── Converters.kt
│   │   │   │   ├── remote/             # Redis integration
│   │   │   │   │   ├── RedisService.kt
│   │   │   │   │   ├── RedisConfig.kt
│   │   │   │   │   └── ConnectionManager.kt
│   │   │   │   └── repository/         # Repository implementations
│   │   │   │       └── HookRepositoryImpl.kt
│   │   │   ├── domain/                 # Business logic
│   │   │   │   ├── model/             # Data models
│   │   │   │   │   ├── HookData.kt
│   │   │   │   │   ├── FilterCriteria.kt
│   │   │   │   │   └── DashboardStats.kt
│   │   │   │   ├── repository/        # Repository interfaces
│   │   │   │   │   └── HookRepository.kt
│   │   │   │   └── usecase/           # Use cases
│   │   │   │       ├── GetHooksUseCase.kt
│   │   │   │       └── ExportDataUseCase.kt
│   │   │   ├── presentation/          # UI layer
│   │   │   │   ├── components/       # Reusable components
│   │   │   │   │   ├── HookCard.kt
│   │   │   │   │   ├── FilterBottomSheet.kt
│   │   │   │   │   └── StatisticsCard.kt
│   │   │   │   ├── ui/              # Screens
│   │   │   │   │   ├── DashboardScreen.kt
│   │   │   │   │   └── StatisticsScreen.kt
│   │   │   │   ├── viewmodel/       # ViewModels
│   │   │   │   │   └── DashboardViewModel.kt
│   │   │   │   └── MainActivity.kt
│   │   │   ├── di/                   # Dependency injection
│   │   │   │   ├── DatabaseModule.kt
│   │   │   │   ├── NetworkModule.kt
│   │   │   │   └── RepositoryModule.kt
│   │   │   └── HooksApplication.kt
│   │   └── res/                      # Resources
│   ├── test/                        # Unit tests
│   │   └── java/com/claudehooks/dashboard/
│   │       ├── data/
│   │       ├── domain/
│   │       └── presentation/
│   └── androidTest/                 # Instrumented tests
│       └── java/com/claudehooks/dashboard/
├── build.gradle.kts                # Module build configuration
└── proguard-rules.pro             # ProGuard rules
```

## Development Workflow

### 1. Feature Development Process

#### Branch Strategy
```bash
# Create feature branch
git checkout -b feature/hook-filtering

# Create bugfix branch
git checkout -b bugfix/redis-connection-timeout

# Create hotfix branch
git checkout -b hotfix/critical-crash-fix
```

#### Development Cycle
1. **Planning**: Create or update issue with requirements
2. **Design**: Create architecture document for complex features
3. **Implementation**: Write code following TDD approach
4. **Testing**: Write comprehensive tests
5. **Code Review**: Create pull request for review
6. **Integration**: Merge after approval and CI passes

#### Commit Message Convention
Follow Conventional Commits specification:

```bash
# Feature commits
feat(redis): add connection retry mechanism
feat(ui): implement hook filtering interface

# Bug fixes
fix(database): resolve Room migration crash
fix(ui): fix hook card expand animation

# Documentation
docs(api): update hook JSON examples
docs(setup): add Redis configuration steps

# Refactoring
refactor(viewmodel): extract common state logic
refactor(database): optimize hook queries

# Tests
test(repository): add integration test suite
test(ui): add Compose UI tests for dashboard
```

### 2. Code Quality Standards

#### Kotlin Coding Conventions

**Function Naming:**
```kotlin
// Good - descriptive and clear
fun parseHookDataFromJson(jsonString: String): HookData
fun validateRedisConnection(): Boolean

// Avoid - abbreviations and unclear names
fun parseHD(json: String): HookData
fun validate(): Boolean
```

**Data Classes:**
```kotlin
// Use data classes for models
data class HookData(
    val id: String,
    val hookType: HookType,
    val timestamp: String
) {
    /**
     * Validates hook data integrity
     * @return List of validation errors, empty if valid
     */
    fun validate(): List<ValidationError> {
        // Implementation
    }
}
```

**Extension Functions:**
```kotlin
// Use extension functions for utility methods
fun String.toHookTimestamp(): LocalDateTime = 
    LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME)

fun HookData.isToolRelated(): Boolean = 
    hookType in setOf(HookType.pre_tool_use, HookType.post_tool_use)
```

#### KDoc Documentation Standards

All public APIs must include comprehensive KDoc comments:

```kotlin
/**
 * Repository for managing Claude Code hook data with Redis and local storage.
 * 
 * This repository provides a unified interface for accessing hook data from both
 * Redis pub/sub streams and local Room database. It handles connection management,
 * data synchronization, and offline capabilities.
 * 
 * @see HookData for data model details
 * @see RedisService for Redis integration
 * 
 * @since 1.0.0
 */
interface HookRepository {
    
    /**
     * Observes hook data changes in real-time.
     * 
     * This Flow emits the latest list of hooks whenever new data arrives
     * from Redis or local database changes occur. The Flow is backed by
     * Room's observable queries and automatically updates the UI.
     * 
     * @return Flow of hook lists, ordered by timestamp descending
     * @throws DatabaseException if local database access fails
     * 
     * @sample
     * ```kotlin
     * repository.observeHooks()
     *     .flowOn(Dispatchers.IO)
     *     .collect { hooks ->
     *         updateUI(hooks)
     *     }
     * ```
     */
    fun observeHooks(): Flow<List<HookData>>
    
    /**
     * Retrieves hooks matching the specified filter criteria.
     * 
     * @param criteria Filter parameters including hook types, status, time range
     * @return Filtered list of hooks, may be empty
     * @throws FilterException if criteria contains invalid parameters
     * @throws DatabaseException if query execution fails
     * 
     * @since 1.0.0
     */
    suspend fun getFilteredHooks(criteria: FilterCriteria): List<HookData>
}
```

#### Error Handling Patterns

```kotlin
// Use sealed classes for typed errors
sealed class HookError : Exception() {
    data class NetworkError(val cause: Throwable) : HookError()
    data class DatabaseError(val query: String, val cause: Throwable) : HookError()
    data class ParseError(val json: String, val cause: Throwable) : HookError()
    object ConfigurationError : HookError()
}

// Use Result type for operations that can fail
suspend fun loadHooks(): Result<List<HookData>> = try {
    val hooks = hookDao.getAllHooks()
    Result.success(hooks)
} catch (e: SQLiteException) {
    Result.failure(HookError.DatabaseError("getAllHooks", e))
} catch (e: Exception) {
    Result.failure(e)
}

// Handle errors in ViewModels
private fun handleError(error: Throwable) {
    val userMessage = when (error) {
        is HookError.NetworkError -> "Connection failed. Please check your network."
        is HookError.DatabaseError -> "Data access failed. Please restart the app."
        is HookError.ParseError -> "Invalid data received. Please report this issue."
        else -> "An unexpected error occurred."
    }
    
    _uiState.update { it.copy(error = userMessage, isLoading = false) }
}
```

## Testing Strategy

### 1. Unit Testing

#### Repository Testing with Fakes
```kotlin
class HookRepositoryTest {
    
    private lateinit var repository: HookRepositoryImpl
    private lateinit var fakeDao: FakeHookDao
    private lateinit var fakeRedisService: FakeRedisService
    
    @Before
    fun setup() {
        fakeDao = FakeHookDao()
        fakeRedisService = FakeRedisService()
        repository = HookRepositoryImpl(fakeDao, fakeRedisService)
    }
    
    @Test
    fun `observeHooks returns data from local database`() = runTest {
        // Given
        val expectedHook = createTestHookData(hookType = HookType.session_start)
        fakeDao.insertHook(expectedHook)
        
        // When & Then
        repository.observeHooks().test {
            val hooks = awaitItem()
            assertThat(hooks).containsExactly(expectedHook)
            awaitComplete()
        }
    }
    
    @Test
    fun `getFilteredHooks applies criteria correctly`() = runTest {
        // Given
        val sessionStartHook = createTestHookData(hookType = HookType.session_start)
        val toolUseHook = createTestHookData(hookType = HookType.pre_tool_use)
        fakeDao.insertHook(sessionStartHook)
        fakeDao.insertHook(toolUseHook)
        
        val criteria = FilterCriteria(hookTypes = setOf(HookType.session_start))
        
        // When
        val filteredHooks = repository.getFilteredHooks(criteria)
        
        // Then
        assertThat(filteredHooks).containsExactly(sessionStartHook)
    }
}
```

#### ViewModel Testing
```kotlin
@ExperimentalCoroutinesExt
class DashboardViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: DashboardViewModel
    private lateinit var mockRepository: HookRepository
    
    @Before
    fun setup() {
        mockRepository = mockk()
        every { mockRepository.observeHooks() } returns flowOf(emptyList())
        
        viewModel = DashboardViewModel(mockRepository, TestScope())
    }
    
    @Test
    fun `initial state is loading with empty hooks`() {
        // Given & When
        val initialState = viewModel.uiState.value
        
        // Then
        assertThat(initialState.hooks).isEmpty()
        assertThat(initialState.isLoading).isFalse()
        assertThat(initialState.error).isNull()
    }
    
    @Test
    fun `updateFilter triggers repository query`() = runTest {
        // Given
        val criteria = FilterCriteria(hookTypes = setOf(HookType.session_start))
        coEvery { mockRepository.getFilteredHooks(any()) } returns emptyList()
        
        // When
        viewModel.updateFilterCriteria(criteria)
        
        // Then
        coVerify { mockRepository.getFilteredHooks(criteria) }
        assertThat(viewModel.uiState.value.filterCriteria).isEqualTo(criteria)
    }
}
```

### 2. Integration Testing

#### Database Integration Tests
```kotlin
@RunWith(AndroidJUnit4::class)
class HookDatabaseTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: HookDatabase
    private lateinit var hookDao: HookDao
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HookDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        hookDao = database.hookDao()
    }
    
    @After
    fun closeDb() {
        database.close()
    }
    
    @Test
    fun insertAndGetHook() = runTest {
        // Given
        val hook = createTestHookData()
        
        // When
        hookDao.insertHook(hook)
        val retrieved = hookDao.getHookById(hook.id)
        
        // Then
        assertThat(retrieved).isEqualTo(hook)
    }
    
    @Test
    fun observeHooksEmitsUpdates() = runTest {
        // Given
        val hook1 = createTestHookData(id = "1")
        val hook2 = createTestHookData(id = "2")
        
        // When & Then
        hookDao.observeAllHooks().test {
            // Initial empty state
            assertThat(awaitItem()).isEmpty()
            
            // Insert first hook
            hookDao.insertHook(hook1)
            assertThat(awaitItem()).containsExactly(hook1)
            
            // Insert second hook
            hookDao.insertHook(hook2)
            assertThat(awaitItem()).containsExactlyElementsIn(listOf(hook2, hook1))
        }
    }
}
```

### 3. UI Testing with Compose

#### Screen Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var testViewModel: DashboardViewModel
    
    @Before
    fun setup() {
        val mockRepository = mockk<HookRepository>()
        every { mockRepository.observeHooks() } returns flowOf(createTestHooks())
        testViewModel = DashboardViewModel(mockRepository, TestScope())
    }
    
    @Test
    fun dashboardScreenDisplaysHooks() {
        // Given
        val testHooks = createTestHooks()
        
        // When
        composeTestRule.setContent {
            DashboardScreen(viewModel = testViewModel)
        }
        
        // Then
        composeTestRule.onNodeWithText("session_start").assertIsDisplayed()
        composeTestRule.onNodeWithText("pre_tool_use").assertIsDisplayed()
    }
    
    @Test
    fun clickingHookExpandsDetails() {
        // Given
        composeTestRule.setContent {
            DashboardScreen(viewModel = testViewModel)
        }
        
        // When
        composeTestRule.onNodeWithTag("hook_card_0").performClick()
        
        // Then
        composeTestRule.onNodeWithText("Hook Details").assertIsDisplayed()
    }
    
    @Test
    fun filterBottomSheetOpensOnFabClick() {
        // Given
        composeTestRule.setContent {
            DashboardScreen(viewModel = testViewModel)
        }
        
        // When
        composeTestRule.onNodeWithContentDescription("Filter").performClick()
        
        // Then
        composeTestRule.onNodeWithText("Filter Options").assertIsDisplayed()
    }
}
```

#### Component Testing
```kotlin
class HookCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun hookCardDisplaysAllData() {
        // Given
        val testHook = createTestHookData(
            hookType = HookType.user_prompt_submit,
            core = CoreData(status = HookStatus.success, execution_time_ms = 150)
        )
        
        // When
        composeTestRule.setContent {
            HookCard(
                hook = testHook,
                onHookClick = { }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("user_prompt_submit").assertIsDisplayed()
        composeTestRule.onNodeWithText("success").assertIsDisplayed()
        composeTestRule.onNodeWithText("150ms").assertIsDisplayed()
    }
    
    @Test
    fun hookCardHandlesClickEvents() {
        // Given
        var clickedHook: HookData? = null
        val testHook = createTestHookData()
        
        // When
        composeTestRule.setContent {
            HookCard(
                hook = testHook,
                onHookClick = { clickedHook = it }
            )
        }
        
        composeTestRule.onNodeWithTag("hook_card").performClick()
        
        // Then
        assertThat(clickedHook).isEqualTo(testHook)
    }
}
```

### 4. Performance Testing

#### Database Performance Tests
```kotlin
@Test
fun largeDatasetPerformance() = runTest {
    // Given - Insert 10,000 hooks
    val hooks = (1..10_000).map { createTestHookData(id = it.toString()) }
    
    // When - Measure insertion time
    val insertionTime = measureTimeMillis {
        hookDao.insertHooks(hooks)
    }
    
    // Then - Should complete within reasonable time
    assertThat(insertionTime).isLessThan(5_000) // 5 seconds
    
    // When - Measure query time
    val queryTime = measureTimeMillis {
        hookDao.getAllHooks()
    }
    
    // Then - Should query quickly
    assertThat(queryTime).isLessThan(1_000) // 1 second
}
```

#### Memory Performance Tests
```kotlin
@Test
fun memoryUsageUnderLoad() {
    // Given
    val runtime = Runtime.getRuntime()
    val initialMemory = runtime.totalMemory() - runtime.freeMemory()
    
    // When - Process large dataset
    repeat(1000) {
        val largeHook = createTestHookData(
            payload = PayloadData(
                prompt = "Very long prompt text".repeat(100)
            )
        )
        // Process hook
    }
    
    // Force garbage collection
    runtime.gc()
    Thread.sleep(100)
    
    // Then - Memory should not increase significantly
    val finalMemory = runtime.totalMemory() - runtime.freeMemory()
    val memoryIncrease = finalMemory - initialMemory
    
    assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024) // 50MB
}
```

## Code Quality Tools

### 1. Static Analysis

#### Detekt Configuration
Create `detekt.yml`:
```yaml
complexity:
  ComplexMethod:
    threshold: 15
  LongMethod:
    threshold: 60
  LongParameterList:
    functionThreshold: 6
    constructorThreshold: 7

naming:
  FunctionNaming:
    excludes: ['**/test/**', '**/androidTest/**']
  VariableNaming:
    excludes: ['**/test/**', '**/androidTest/**']

style:
  MaxLineLength:
    maxLineLength: 120
  ForbiddenComment:
    values: ['TODO:', 'FIXME:', 'STOPSHIP:']
    excludes: ['**/test/**', '**/androidTest/**']
```

Run detekt:
```bash
./gradlew detekt
```

#### Ktlint Integration
Add to `build.gradle.kts`:
```kotlin
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

ktlint {
    version.set("0.50.0")
    debug.set(true)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
}
```

Run ktlint:
```bash
./gradlew ktlintCheck
./gradlew ktlintFormat
```

### 2. Test Coverage

#### JaCoCo Configuration
Add to `build.gradle.kts`:
```kotlin
plugins {
    jacoco
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest", "createDebugCoverageReport")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )
    
    val debugTree = fileTree("${buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    
    val mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files([mainSrc]))
    classDirectories.setFrom(files([debugTree]))
    executionData.setFrom(fileTree(buildDir) {
        include("**/*.exec", "**/*.ec")
    })
}
```

Generate coverage report:
```bash
./gradlew jacocoTestReport
```

### 3. Continuous Integration

#### GitHub Actions Workflow
Create `.github/workflows/android.yml`:
```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run ktlint
      run: ./gradlew ktlintCheck
    
    - name: Run detekt
      run: ./gradlew detekt
    
    - name: Run unit tests
      run: ./gradlew test
    
    - name: Generate test report
      run: ./gradlew jacocoTestReport
    
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: ./app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
    
    - name: Build debug APK
      run: ./gradlew assembleDebug
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: debug-apk
        path: app/build/outputs/apk/debug/*.apk

  instrumented-test:
    runs-on: macos-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run instrumented tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 29
        script: ./gradlew connectedCheck
```

## Contributing Guidelines

### 1. Pull Request Process

#### Before Creating a PR
1. **Update from main**: `git pull origin main`
2. **Run all checks**: `./gradlew check`
3. **Update documentation**: Update relevant docs for changes
4. **Add tests**: Ensure new code has appropriate test coverage

#### PR Template
```markdown
## Description
Brief description of changes made.

## Type of Change
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that causes existing functionality to change)
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Code is commented where necessary
- [ ] Documentation updated
- [ ] Tests pass locally
- [ ] No lint/detekt warnings
```

#### Review Criteria
Reviewers should check:
- **Functionality**: Does the code work as intended?
- **Architecture**: Follows established patterns?
- **Testing**: Adequate test coverage?
- **Performance**: No performance regressions?
- **Security**: No security vulnerabilities?
- **Documentation**: KDoc and README updates?

### 2. Release Process

#### Version Management
Follow Semantic Versioning (SemVer):
- **MAJOR**: Breaking API changes
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

Update version in `build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        versionCode = 2
        versionName = "1.1.0"
    }
}
```

#### Release Checklist
1. **Update version numbers**
2. **Update changelog**
3. **Run full test suite**
4. **Create release branch**: `release/1.1.0`
5. **Generate signed APK**
6. **Test on multiple devices**
7. **Create GitHub release with notes**
8. **Merge to main and tag**

### 3. Issue Management

#### Bug Report Template
```markdown
**Bug Description**
A clear description of the bug.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

**Expected Behavior**
What you expected to happen.

**Screenshots**
If applicable, add screenshots.

**Device Information:**
 - Device: [e.g. Pixel 6]
 - OS: [e.g. Android 13]
 - App Version: [e.g. 1.0.0]

**Additional Context**
Any other context about the problem.
```

#### Feature Request Template
```markdown
**Feature Description**
A clear description of the feature you'd like to see.

**Problem Statement**
What problem does this feature solve?

**Proposed Solution**
Describe the solution you'd like.

**Alternatives Considered**
Describe alternatives you've considered.

**Additional Context**
Any other context or screenshots about the feature.
```

### 4. Documentation Standards

#### Code Documentation
- **All public APIs**: Must have KDoc comments
- **Complex logic**: Add inline comments explaining why
- **TODOs**: Include issue numbers or context
- **Examples**: Provide usage examples in KDoc

#### Architecture Documentation
- **Update ARCHITECTURE.md** for architectural changes
- **Update API.md** for data model changes
- **Update SETUP.md** for configuration changes
- **Create ADRs** (Architecture Decision Records) for significant decisions

## Troubleshooting Development Issues

### Common Build Issues

#### Gradle Sync Problems
```bash
# Clear Gradle caches
./gradlew clean
rm -rf ~/.gradle/caches/

# Refresh dependencies
./gradlew --refresh-dependencies
```

#### Hilt Compilation Errors
```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Check for circular dependencies
./gradlew app:dependencies --configuration debugCompileClasspath
```

#### Room Schema Issues
```bash
# Reset database schema
rm -rf app/schemas/
./gradlew clean
./gradlew assembleDebug
```

### Redis Connection Issues

#### Certificate Problems
```bash
# Verify certificates exist
ls -la ~/.redis/certs/

# Test connection manually
redis-cli --tls \
  --cert ~/.redis/certs/redis-client.crt \
  --key ~/.redis/certs/redis-client.key \
  --cacert ~/.redis/certs/redis-ca.crt \
  -h redis-18773.c311.eu-central-1-1.ec2.redns.redis-cloud.com \
  -p 18773 \
  ping
```

#### Network Debugging
Add to `AndroidManifest.xml` for debug builds:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="true">
</application>
```

### Performance Profiling

#### Memory Profiling
1. Connect device and run app
2. Open Android Studio Profiler
3. Select Memory profiler
4. Capture heap dump during heavy operations
5. Analyze object retention and leaks

#### Network Profiling
1. Use Network Profiler in Android Studio
2. Monitor Redis connection patterns
3. Check for connection leaks
4. Verify TLS handshake performance

---

*This development guide provides comprehensive information for contributing to the Claude Hooks Dashboard project. For specific technical questions, refer to the architectural documentation or create an issue for discussion.*