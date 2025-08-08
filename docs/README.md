# Claude Code Redis Hook Integration - Documentation

Comprehensive documentation for the Claude Code Redis hook integration system that enables real-time monitoring and analysis of Claude Code activities through Redis pub/sub messaging.

## 📚 Documentation Overview

This documentation provides complete guidance for installing, configuring, and using the Claude Code Redis hook integration system. The system captures all 8 Claude Code hook types and publishes them to a Redis channel for real-time monitoring and analytics.

### Quick Start

1. **[Installation Guide](INSTALLATION_SETUP.md)** - Get up and running quickly
2. **[Basic Usage Examples](USAGE_EXAMPLES.md#basic-usage)** - Simple examples to start with
3. **[Health Check](MONITORING_DEBUG.md#health-checks)** - Verify your installation

## 📋 Table of Contents

### Core Documentation

| Document | Description |
|----------|-------------|
| **[Installation & Setup](INSTALLATION_SETUP.md)** | Complete installation guide with automated and manual options |
| **[Configuration Reference](CONFIGURATION.md)** | All environment variables, settings, and configuration options |
| **[Usage Examples](USAGE_EXAMPLES.md)** | Practical examples for all 8 hook types with real-world scenarios |
| **[Integration Guide](INTEGRATION.md)** | How to integrate with CI/CD, containers, cloud platforms |

### Operations & Maintenance

| Document | Description |
|----------|-------------|
| **[Troubleshooting Guide](TROUBLESHOOTING.md)** | Common issues, solutions, and diagnostic procedures |
| **[Security & Performance](SECURITY_PERFORMANCE.md)** | Security best practices and performance optimization |
| **[Monitoring & Debug](MONITORING_DEBUG.md)** | Monitoring setup, debugging techniques, and health checks |

## 🚀 Quick Start Guide

### 1. Prerequisites

- **Redis Server** (5.0+) with optional TLS support
- **Redis CLI tools** installed on your system
- **Bash 4.0+** or compatible shell
- **Claude Code** installed and configured

### 2. Installation (2 minutes)

```bash
# Clone or download the scripts
git clone <repository_url> claude-redis-hooks
cd claude-redis-hooks

# Run automated setup
chmod +x *.sh
./example_integration.sh setup

# Follow prompts to configure Redis connection
```

### 3. Test Your Setup

```bash
# Send a test hook
echo '{"message": "Hello, Redis!"}' | ./claude_hook_redis.sh notification test-session

# Monitor the Redis channel
redis-cli SUBSCRIBE hooksdata
```

### 4. Verify Integration

Start Claude Code and verify hooks are being sent to Redis. Check the logs:

```bash
tail -f ~/.claude/logs/hooks.log
```

## 🎯 System Overview

### Architecture

```
Claude Code → Hook Scripts → Redis Publisher → Redis Channel → Your Applications
     ↓              ↓              ↓              ↓
  8 Hook Types → JSON Payload → Redis Pub/Sub → Real-time Analytics
```

### Supported Hook Types

The system supports all 8 Claude Code hook types:

1. **session_start** - New session begins
2. **user_prompt_submit** - User submits a prompt  
3. **pre_tool_use** - Before tool execution
4. **post_tool_use** - After tool execution
5. **notification** - System notifications
6. **stop_hook** - Stop hook activation
7. **sub_agent_stop_hook** - Sub-agent stop hook
8. **pre_compact** - Before context compaction

### Data Flow

1. **Claude Code** triggers hooks during operation
2. **Hook Scripts** (in `~/.claude/hooks/`) capture the data
3. **Redis Publisher** (`claude_hook_redis.sh`) processes and enriches the data
4. **Redis Channel** (`hooksdata`) receives the formatted JSON messages
5. **Your Applications** subscribe to the channel for real-time processing

## 📊 JSON Message Format

Each hook message follows this standardized structure:

```json
{
  "id": "unique-uuid-v4",
  "hook_type": "notification",
  "timestamp": "2025-08-08T10:30:45.123Z",
  "session_id": "claude-session-001",
  "sequence": 42,
  "core": {
    "status": "success",
    "execution_time_ms": 15
  },
  "payload": {
    "message": "Your hook-specific data here"
  },
  "context": {
    "platform": "darwin", 
    "cwd": "/current/directory",
    "git_branch": "main",
    "git_status": "clean",
    "user_agent": "claude-hook-redis/1.0.0"
  },
  "metrics": {
    "script_version": "1.0.0"
  }
}
```

## 🔧 Key Features

### ✅ Complete Hook Coverage
- Supports all 8 Claude Code hook types
- Automatic sequence numbering per session
- Rich context information included

### ✅ Production Ready
- Comprehensive error handling and logging
- TLS/SSL support for secure Redis connections
- Thread-safe sequence number management
- Configurable retry mechanisms

### ✅ Easy Integration
- Simple environment variable configuration
- Automated installation scripts
- Docker and Kubernetes support
- CI/CD pipeline integration

### ✅ Monitoring & Debug
- Real-time monitoring dashboards
- Performance metrics collection
- Health check scripts
- Debug mode with detailed tracing

### ✅ Security Focused
- Credential protection and rotation
- Input validation and sanitization  
- Audit logging capabilities
- Rate limiting support

## 📖 Documentation Highlights

### For Developers

- **[Usage Examples](USAGE_EXAMPLES.md)** - Copy-paste examples for all hook types
- **[Integration Patterns](INTEGRATION.md#integration-patterns)** - Common integration scenarios
- **[API Examples](USAGE_EXAMPLES.md#custom-applications)** - Python, Node.js client libraries

### For System Administrators  

- **[Security Guide](SECURITY_PERFORMANCE.md#security-best-practices)** - Credential management, TLS configuration
- **[Performance Tuning](SECURITY_PERFORMANCE.md#performance-optimization)** - Scaling and optimization strategies
- **[Monitoring Setup](MONITORING_DEBUG.md#system-monitoring)** - Comprehensive monitoring and alerting

### For DevOps Engineers

- **[Container Integration](INTEGRATION.md#container-integration)** - Docker, Kubernetes deployment
- **[CI/CD Examples](INTEGRATION.md#cicd-integration)** - GitHub Actions, Jenkins, GitLab CI
- **[Cloud Platforms](INTEGRATION.md#cloud-platform-integration)** - AWS, Azure, GCP integration

## 🛠️ Common Use Cases

### Development & Testing
- **Code Review Automation** - Track tool usage during code reviews
- **Testing Pipeline Integration** - Monitor test execution and results
- **Performance Analysis** - Analyze Claude Code usage patterns

### Production Monitoring
- **Real-time Analytics** - Live dashboards of Claude Code activity
- **Error Tracking** - Automatic error detection and alerting
- **Usage Metrics** - Track adoption and usage patterns

### Security & Compliance
- **Audit Logging** - Complete audit trail of all activities
- **Access Monitoring** - Track user sessions and actions
- **Compliance Reporting** - Generate compliance reports

## 📞 Getting Help

### Documentation

| Question | See |
|----------|-----|
| How do I install the system? | **[Installation Guide](INSTALLATION_SETUP.md)** |
| What environment variables are available? | **[Configuration Reference](CONFIGURATION.md)** |
| How do I send different hook types? | **[Usage Examples](USAGE_EXAMPLES.md)** |
| How do I integrate with my CI/CD? | **[Integration Guide](INTEGRATION.md)** |
| Something isn't working | **[Troubleshooting Guide](TROUBLESHOOTING.md)** |
| How do I secure my setup? | **[Security Guide](SECURITY_PERFORMANCE.md)** |
| How do I monitor the system? | **[Monitoring Guide](MONITORING_DEBUG.md)** |

### Quick Diagnostics

```bash
# Run health check
./example_integration.sh status

# Test hook sending
echo '{"test": true}' | ./claude_hook_redis.sh notification test

# Check logs  
tail -f ~/.claude/logs/hooks.log

# Monitor Redis channel
redis-cli SUBSCRIBE hooksdata
```

### Common Issues

| Issue | Quick Fix |
|-------|-----------|
| Permission denied | `chmod +x claude_hook_redis.sh` |
| Redis connection failed | Check `REDIS_HOST` and `REDIS_PASSWORD` |
| Hook not triggering | Verify `~/.claude/hooks/` directory and permissions |
| JSON parsing error | Validate JSON with `echo "$JSON" \| jq .` |

## 📈 What's Next?

After getting the basic system running:

1. **[Set up monitoring](MONITORING_DEBUG.md)** - Create dashboards and alerts
2. **[Secure your installation](SECURITY_PERFORMANCE.md)** - Implement security best practices  
3. **[Integrate with your tools](INTEGRATION.md)** - Connect to your existing infrastructure
4. **[Optimize performance](SECURITY_PERFORMANCE.md#performance-optimization)** - Scale for production use

## 🤝 Contributing

This system is designed to be extensible and customizable. Key areas for contribution:

- **Custom Hook Processors** - Add your own hook processing logic
- **Integration Examples** - Share integration patterns for new platforms
- **Monitoring Extensions** - Add new monitoring and alerting capabilities
- **Performance Improvements** - Optimize for high-volume environments

## 📄 License

MIT License - See individual files for specific license information.

---

**Ready to get started?** Begin with the **[Installation Guide](INSTALLATION_SETUP.md)** to set up your Claude Code Redis hook integration in minutes.