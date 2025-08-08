# Claude Hooks Dashboard

A real-time Android application for monitoring and analyzing Claude Code hook events from Redis.

## Overview

The Claude Hooks Dashboard provides a comprehensive real-time interface for monitoring Claude Code execution hooks. It connects to a Redis Cloud instance, subscribes to hook events, and displays them in an intuitive Material Design 3 interface with advanced filtering, search, and analytics capabilities.

## Key Features

### Real-time Monitoring
- Live Redis pub/sub connection with automatic reconnection
- Real-time display of all 8 Claude Code hook types
- Session-based grouping and tracking
- Central European Time (CET) timestamp display

### Hook Types Supported
1. **session_start** - New Claude Code session initialization
2. **user_prompt_submit** - User input submission
3. **pre_tool_use** - Before tool execution
4. **post_tool_use** - After tool execution completion
5. **notification** - System notifications and alerts
6. **stop_hook** - Session termination events
7. **sub_agent_stop_hook** - Sub-agent termination
8. **pre_compact** - Context compaction events

### Analytics & Insights
- Session statistics and metrics
- Tool usage frequency analysis
- Execution time tracking and performance insights
- Status distribution (success/error/blocked/pending)
- Platform and project type analytics

### User Experience
- Material Design 3 with dynamic theming
- Advanced filtering by hook type, status, time range
- Full-text search across all hook data
- Export to CSV for external analysis
- Offline capability with local SQLite storage
- Pull-to-refresh and infinite scroll

### Data Management
- Local Room database for persistence
- Secure TLS connection to Redis Cloud
- Certificate-based authentication
- Data export and backup capabilities

## Screenshots

*Dashboard Overview*: Real-time hook stream with expandable cards showing hook details, timestamps, and status indicators.

*Filter Interface*: Bottom sheet with comprehensive filtering options including hook types, status, date ranges, and session selection.

*Statistics View*: Analytics dashboard showing hook distribution, session metrics, tool usage patterns, and performance statistics.

*Hook Details*: Detailed view of individual hooks with JSON payload visualization, context information, and execution metadata.

## Quick Start

1. **Prerequisites**
   - Android Studio Hedgehog or later
   - Android SDK 24+ (Android 7.0)
   - Redis Cloud instance with TLS certificates

2. **Setup**
   ```bash
   git clone <repository-url>
   cd hooks_dashboard
   ```

3. **Configuration**
   - Place Redis TLS certificates in `~/.redis/certs/`
   - Configure Redis connection in `RedisConfig.kt`

4. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

See [SETUP.md](SETUP.md) for detailed installation instructions.

## Architecture

Built with modern Android architecture patterns:
- **MVVM** with Jetpack Compose
- **Clean Architecture** with domain/data/presentation layers
- **Dependency Injection** using Hilt
- **Reactive Programming** with Kotlin Coroutines and Flow
- **Local Storage** with Room database
- **Network Layer** using Lettuce Redis client

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed technical documentation.

## Technology Stack

### Core Technologies
- **Kotlin** - Primary language
- **Jetpack Compose** - Modern UI framework
- **Material Design 3** - Design system
- **Hilt** - Dependency injection
- **Room** - Local database
- **Coroutines & Flow** - Asynchronous programming

### Network & Data
- **Lettuce** - Redis client with pub/sub support
- **Kotlinx Serialization** - JSON parsing
- **OpenCSV** - Data export functionality

### Development & Testing
- **Timber** - Logging framework
- **JUnit** - Unit testing
- **Espresso** - UI testing

## Project Structure

```
app/src/main/java/com/claudehooks/dashboard/
├── data/                   # Data layer
│   ├── local/             # Room database
│   ├── remote/            # Redis client
│   └── repository/        # Repository implementations
├── domain/                # Business logic
│   ├── model/            # Data models
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Use cases
├── presentation/          # UI layer
│   ├── components/       # Reusable UI components
│   ├── ui/              # Screens and composables
│   └── viewmodel/       # ViewModels
└── di/                   # Dependency injection modules
```

## Documentation

- [SETUP.md](SETUP.md) - Installation and configuration
- [ARCHITECTURE.md](ARCHITECTURE.md) - Technical architecture
- [API.md](API.md) - Data models and Redis integration
- [DEVELOPMENT.md](DEVELOPMENT.md) - Development workflow

## Contributing

Please read [DEVELOPMENT.md](DEVELOPMENT.md) for details on our code of conduct and development process.

## Security

- TLS encryption for Redis connections
- Certificate-based authentication
- No sensitive data in logs
- Secure data export handling

## Performance

- Efficient Redis connection pooling
- Local caching with Room database
- Lazy loading for large datasets
- Memory-optimized UI components

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and feature requests, please use the GitHub issue tracker.

---

*Built with ❤️ for Claude Code monitoring and analytics*