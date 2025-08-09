# Claude Hooks Dashboard 🎯

[![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-orange.svg)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Real-time monitoring and visualization for Claude Code AI assistant interactions**

Claude Hooks Dashboard is an Android application that provides comprehensive real-time monitoring and analytics for Claude Code (Anthropic's AI coding assistant) hook events. It connects to Redis pub/sub to capture and visualize all Claude Code interactions, giving developers unprecedented visibility into their AI-assisted development workflows.

![Dashboard Overview](docs/images/dashboard-overview.png)
*Real-time hook monitoring with live-updating timestamps and execution metrics*

## ✨ Key Features

### 🔴 Real-Time Monitoring
- **Live Redis Integration** - Instant event streaming via pub/sub
- **Live Duration Tracking** - Real-time age display that updates every second
- **Session Correlation** - Group events by Claude Code session
- **Auto-Reconnection** - Robust connection handling with fallback

### 📊 Comprehensive Analytics
- **Execution Metrics** - Tool performance and timing analysis
- **Usage Patterns** - Track most-used tools and commands
- **Session Statistics** - Success rates, error tracking, and trends
- **Performance Insights** - Identify bottlenecks and optimization opportunities

### 🎨 Modern Android Experience
- **Material Design 3** - Beautiful, adaptive UI that follows your system theme
- **Jetpack Compose** - Modern reactive UI framework
- **Smart Notifications** - Android system notifications for important events
- **Dark Mode** - Full dark theme support
- **Responsive Design** - Optimized for phones and tablets

### 🔐 Enterprise-Ready
- **Secure TLS Connection** - Certificate-based Redis Cloud authentication
- **Privacy-Focused** - No data leaves your control (Redis → Your Phone)
- **Local Storage** - Events cached locally for offline access
- **Export Capabilities** - CSV export for external analysis

## 🏗️ Architecture

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐    ┌─────────────────┐
│ Claude Code │ -> │ Shell Hooks  │ -> │ Redis Cloud │ -> │ Android App     │
│ Assistant   │    │ Integration  │    │ Pub/Sub     │    │ Dashboard       │
└─────────────┘    └──────────────┘    └─────────────┘    └─────────────────┘
```

The system captures Claude Code hook events through shell script integration, publishes them to Redis, and displays them in real-time on your Android device.

## 🚀 Quick Start

### Prerequisites
- Android 7.0+ (API level 24)
- Claude Code installed and configured
- Redis Cloud account (free tier available)

### 1. Install Android App
```bash
# Clone the repository
git clone https://github.com/yourusername/claude-hooks-dashboard.git
cd claude-hooks-dashboard

# Build and install
cd hooks_dashboard
./gradlew installDebug

# Or install the pre-built APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Configure Redis Integration
```bash
# Set up environment variables
export REDIS_HOST="your-redis-host.com"
export REDIS_PASSWORD="your-redis-password"
export REDIS_PORT="18773"
export REDIS_TLS="true"

# Configure Claude Code hooks
export CLAUDE_HOOKS_LOG="$HOME/.claude/logs/hooks.log"

# Test the connection
./test_claude_hook_redis.sh
```

### 3. Integrate with Claude Code
Add to your Claude Code configuration:
```bash
# In your shell profile (.bashrc, .zshrc, etc.)
export CLAUDE_POST_TOOL_USE_HOOK="/path/to/claude_hook_redis.sh"
export CLAUDE_PRE_TOOL_USE_HOOK="/path/to/claude_hook_redis.sh"
export CLAUDE_USER_PROMPT_SUBMIT_HOOK="/path/to/claude_hook_redis.sh"
```

## ⚙️ Configuration

### Redis Cloud Setup
1. Create a free Redis Cloud account
2. Create a new database with pub/sub enabled
3. Note your connection details:
   - Host: `redis-xxxxx.cloud.redislabs.com`
   - Port: `18773` (typically)
   - Password: Generated password
   - TLS: Enabled

### Android App Configuration
The app automatically discovers Redis configuration from:
1. Environment variables
2. Configuration files in `~/.claude/`
3. Built-in defaults for testing

## 📱 Usage

### Dashboard Overview
- **Live Events** - See Claude Code interactions as they happen
- **Session Tracking** - Monitor current and past sessions
- **Tool Performance** - View execution times and success rates
- **Filtering** - Filter by event type, session, or time range

### Event Types Monitored
| Hook Type | Description | Example |
|-----------|-------------|---------|
| `session_start` | New Claude Code session begins | Session initialization |
| `user_prompt_submit` | User submits query/command | "Create a React component" |
| `pre_tool_use` | Before tool execution | About to run `Read` tool |
| `post_tool_use` | After tool completion | `Edit` completed in 45ms |
| `notification` | System notifications | Build completed successfully |
| `stop_hook` | Session termination | User ends session |
| `sub_agent_stop_hook` | Sub-agent completion | Background task finished |
| `pre_compact` | Memory compaction | Context size reduction |

### Live Features
- **Real-Time Age Display** - Shows "09 14:23:45   00:03:25 ago" format
- **Auto-Refresh** - Updates every second without user action
- **Smart Notifications** - Get alerted to important events
- **Session Correlation** - Track related events across tools

## 🛠️ Development

### Building from Source
```bash
# Prerequisites
# - Android Studio Arctic Fox or later
# - JDK 17+
# - Android SDK with API 34

# Clone and build
git clone https://github.com/yourusername/claude-hooks-dashboard.git
cd claude-hooks-dashboard/hooks_dashboard

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

### Project Structure
```
claude-hooks-dashboard/
├── claude_hook_redis.sh           # Main hook script
├── enhance_hook_payload.sh        # Payload processing
├── hooks_dashboard/               # Android application
│   ├── app/                      # Main application module
│   │   ├── src/main/java/com/claudehooks/dashboard/
│   │   │   ├── data/            # Repository, models, mappers
│   │   │   ├── notification/    # Android notifications
│   │   │   ├── presentation/    # UI components, screens
│   │   │   └── HooksApplication.kt
│   │   └── src/main/res/        # Android resources
│   └── build.gradle.kts         # Build configuration
├── docs/                        # Comprehensive documentation
│   ├── INSTALLATION_SETUP.md
│   ├── CONFIGURATION.md
│   ├── TROUBLESHOOTING.md
│   └── USAGE_EXAMPLES.md
└── test_*.sh                   # Testing utilities
```

### Key Technologies
- **Language**: Kotlin 1.9.10
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Repository pattern
- **Networking**: Lettuce Redis client with Netty
- **Serialization**: Kotlinx Serialization
- **Threading**: Kotlin Coroutines with Flow
- **Logging**: Timber
- **Dependency Injection**: Ready for Hilt/Dagger

## 📚 Documentation

For detailed documentation, see:
- [Installation & Setup](docs/INSTALLATION_SETUP.md)
- [Configuration Guide](docs/CONFIGURATION.md)
- [Usage Examples](docs/USAGE_EXAMPLES.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)
- [API Reference](hooks_dashboard/API.md)
- [Architecture Details](hooks_dashboard/ARCHITECTURE.md)

## 🔧 Troubleshooting

### Common Issues

**App not receiving events**
```bash
# Check Redis connection
./test_claude_hook_redis.sh

# Verify Claude Code hooks are configured
echo $CLAUDE_POST_TOOL_USE_HOOK

# Check logs
tail -f ~/.claude/logs/hooks.log
```

**Connection errors**
- Verify Redis Cloud credentials
- Check firewall/network restrictions
- Ensure TLS certificates are valid

**Performance issues**
- Reduce event retention time
- Enable connection pooling
- Check device storage space

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

### Areas for Contribution
- UI/UX improvements
- Performance optimizations
- Additional hook types
- Export formats
- Documentation improvements
- Testing coverage

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Anthropic** for Claude Code and the hook system architecture
- **Redis Labs** for reliable pub/sub infrastructure
- **Google** for Jetpack Compose and Material Design
- **JetBrains** for Kotlin language and excellent tooling
- **Open Source Community** for the libraries and tools that make this possible

## 📞 Support

- 📖 **Documentation**: Check our [comprehensive docs](docs/)
- 🐛 **Bug Reports**: [GitHub Issues](https://github.com/yourusername/claude-hooks-dashboard/issues)
- 💡 **Feature Requests**: [GitHub Discussions](https://github.com/yourusername/claude-hooks-dashboard/discussions)
- 💬 **Questions**: [GitHub Discussions Q&A](https://github.com/yourusername/claude-hooks-dashboard/discussions/categories/q-a)

---

**Made with ❤️ for developers using AI coding assistants**

*Star ⭐ this repository if you find it useful!*