# Setup Guide

Complete installation and configuration guide for the Claude Hooks Dashboard.

## Prerequisites

### Development Environment
- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 17 or later (included with Android Studio)
- **Android SDK**: API level 34 (compileSdk)
- **Minimum Android Version**: API level 24 (Android 7.0)

### Hardware Requirements
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space for Android SDK and project
- **Network**: Stable internet connection for Redis Cloud

### Redis Infrastructure
- **Redis Cloud Instance**: TLS-enabled connection
- **Certificates**: Client certificates for TLS authentication
- **Network Access**: Ability to connect to external Redis instance

## Installation Steps

### 1. Clone Repository
```bash
git clone <repository-url>
cd hooks_dashboard
```

### 2. Android Studio Setup
1. Open Android Studio
2. Select "Open an existing Android Studio project"
3. Navigate to and select the `hooks_dashboard` directory
4. Wait for Gradle sync to complete

### 3. SDK Configuration
Ensure the following are installed in Android Studio SDK Manager:
- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0
- Android Support Repository
- Google Play Services

## Redis Configuration

### 1. Certificate Setup
Create the certificate directory and copy TLS certificates:

```bash
mkdir -p ~/.redis/certs/
cp /path/to/your/redis-client.crt ~/.redis/certs/
cp /path/to/your/redis-client.key ~/.redis/certs/
cp /path/to/your/redis-ca.crt ~/.redis/certs/
```

### 2. Connection Configuration
The app connects to the Redis Cloud instance defined in `RedisConfig.kt`:

**Default Configuration:**
- Host: `redis-18773.c311.eu-central-1-1.ec2.redns.redis-cloud.com`
- Port: `18773`
- Channel: `hooksdata`
- TLS: Enabled
- Certificate Path: `~/.redis/certs/`

### 3. Environment Variables (Optional)
For production deployments, set environment variables:

```bash
export REDIS_HOST="your-redis-host"
export REDIS_PORT="6379"
export REDIS_PASSWORD="your-password"
export REDIS_CHANNEL="hooksdata"
export REDIS_CERT_PATH="/path/to/certs"
```

## Build Configuration

### 1. Gradle Properties
Create or update `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

### 2. Signing Configuration (Optional)
For release builds, add to `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        release {
            storeFile file("path/to/your/keystore.jks")
            storePassword "your_store_password"
            keyAlias "your_key_alias"
            keyPassword "your_key_password"
        }
    }
}
```

## Building the Application

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Install to Device
```bash
# Debug version
./gradlew installDebug

# Release version
./gradlew installRelease
```

## Configuration Options

### 1. Database Configuration
Local Room database settings in `DatabaseModule.kt`:

```kotlin
@Database(
    entities = [HookData::class],
    version = 1,
    exportSchema = false
)
```

### 2. Network Timeouts
Adjust Redis connection timeouts in `NetworkModule.kt`:

```kotlin
@Provides
fun provideRedisClient(): RedisClient {
    return RedisClient.create().apply {
        setDefaultTimeout(Duration.ofSeconds(30))
    }
}
```

### 3. UI Theme Configuration
Customize Material Design 3 theme in `themes.xml`:

```xml
<style name="Theme.ClaudeHooksDashboard" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">@color/md_theme_light_primary</item>
    <item name="colorSecondary">@color/md_theme_light_secondary</item>
</style>
```

## Testing Setup

### 1. Unit Tests
Run unit tests:
```bash
./gradlew test
```

### 2. Instrumented Tests
Run on connected device:
```bash
./gradlew connectedAndroidTest
```

### 3. UI Tests
Ensure device/emulator is connected:
```bash
./gradlew app:connectedAndroidTest
```

## Troubleshooting

### Common Issues

**1. Redis Connection Failed**
- Verify certificates are in correct location (`~/.redis/certs/`)
- Check network connectivity to Redis host
- Confirm TLS is properly configured
- Validate Redis instance is running and accepting connections

**2. Gradle Sync Failed**
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

**3. Build Errors**
- Update Android Studio to latest version
- Clean and rebuild project
- Invalidate caches and restart IDE

**4. Certificate Issues**
```bash
# Verify certificate files exist
ls -la ~/.redis/certs/

# Check certificate validity
openssl x509 -in ~/.redis/certs/redis-client.crt -text -noout
```

**5. Memory Issues**
Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

### Debug Tools

**1. Enable Debug Logging**
In `HooksApplication.kt`:
```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
```

**2. Network Debugging**
Add to `AndroidManifest.xml` for debug builds:
```xml
<application
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config">
```

**3. Database Inspection**
Use Android Studio Database Inspector to view Room database contents.

## Performance Optimization

### 1. Build Performance
```properties
# gradle.properties
android.useAndroidX=true
android.enableJetifier=true
org.gradle.parallel=true
org.gradle.caching=true
kapt.incremental.apt=true
```

### 2. Runtime Performance
- Enable R8 code shrinking for release builds
- Use ProGuard rules for optimization
- Configure appropriate heap sizes

### 3. Network Optimization
- Implement connection pooling for Redis
- Use appropriate timeout values
- Enable compression where applicable

## Security Considerations

### 1. Certificate Management
- Store certificates securely
- Rotate certificates regularly
- Use environment-specific certificates

### 2. Network Security
- Always use TLS for Redis connections
- Validate certificate chains
- Implement proper error handling

### 3. Data Protection
- Encrypt sensitive data at rest
- Implement secure export functionality
- Follow GDPR guidelines for data handling

## Development Environment

### 1. Code Style
Install and configure:
- Kotlin plugin for Android Studio
- ktlint for code formatting
- detekt for static analysis

### 2. Git Hooks
Setup pre-commit hooks:
```bash
# .git/hooks/pre-commit
#!/bin/sh
./gradlew ktlintCheck detekt
```

### 3. Continuous Integration
Example GitHub Actions workflow:
```yaml
name: Android CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'adopt'
    - name: Cache Gradle packages
      uses: actions/cache@v2
      with:
        path: ~/.gradle/caches
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle') }}
    - name: Run tests
      run: ./gradlew test
    - name: Build APK
      run: ./gradlew assembleDebug
```

## Deployment

### 1. Release Preparation
- Update version code and name
- Generate signed APK/Bundle
- Test on multiple devices
- Prepare release notes

### 2. Distribution Options
- Direct APK distribution
- Internal testing via Play Console
- Firebase App Distribution
- GitHub Releases

### 3. Monitoring
- Configure crash reporting (Firebase Crashlytics)
- Set up analytics tracking
- Monitor Redis connection health
- Track app performance metrics

---

*For additional support, consult the [DEVELOPMENT.md](DEVELOPMENT.md) guide or create an issue in the project repository.*