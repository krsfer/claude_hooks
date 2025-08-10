# Configuration Reference

Complete reference for all configuration options in the Claude Code Redis hook integration system.

## Table of Contents
- [Environment Variables](#environment-variables)
- [Configuration File](#configuration-file)
- [Hook Configuration](#hook-configuration)
- [Redis Settings](#redis-settings)
- [Logging Configuration](#logging-configuration)
- [Advanced Options](#advanced-options)

## Environment Variables

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `REDIS_HOST` | Redis server hostname or IP address | `redis.example.com`, `192.168.1.100` |
| `REDIS_PASSWORD` | Redis authentication password | `SecurePass123!` |

### Connection Settings

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `REDIS_PORT` | Redis server port | `6380` | `6379`, `6380` |
| `REDIS_TLS` | Enable TLS/SSL connection | `true` | `true`, `false` |
| `REDIS_TLS_SKIP_VERIFY` | Skip TLS certificate verification | `false` | `true`, `false` |

### Storage Options

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `REDIS_PERSIST` | Store hooks in Redis keys | `false` | `true`, `false` |
| `REDIS_CHANNEL` | Redis pub/sub channel name | `hooksdata` | `hooks`, `claude-events` |

### Logging Settings

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `CLAUDE_HOOKS_LOG` | Log file path | `/tmp/claude_hooks.log` | `~/.claude/logs/hooks.log` |
| `CLAUDE_HOOKS_SEQ` | Sequence file path | `/tmp/.claude_hooks_seq` | `~/.claude/.sequence` |
| `DEBUG` | Enable debug logging | `0` | `1`, `0` |
| `CLAUDE_HOOKS_DEBUG` | Enable hook-specific debug | `0` | `1`, `0` |

### Session Management

| Variable | Description | Default | Example |
|----------|-------------|---------|---------|
| `CLAUDE_SESSION_ID` | Current session identifier | (generated) | `sess-2025-08-07-001` |

## Configuration File

### Standard Configuration File

Location: `~/.claude/redis_config.env`

```bash
# Redis Configuration for Claude Hooks
# This file contains sensitive information - keep it secure!

# Required Settings
export REDIS_HOST="redis.example.com"
export REDIS_PASSWORD="YourSecurePassword"

# Connection Settings
export REDIS_PORT="6380"
export REDIS_TLS="true"
export REDIS_TLS_SKIP_VERIFY="false"

# Storage Settings
export REDIS_PERSIST="true"
export REDIS_CHANNEL="hooksdata"

# Logging Settings
export CLAUDE_HOOKS_LOG="$HOME/.claude/logs/hooks.log"
export CLAUDE_HOOKS_SEQ="$HOME/.claude/.sequence"
export DEBUG="0"
export CLAUDE_HOOKS_DEBUG="0"

# Session Settings (optional)
# export CLAUDE_SESSION_ID="custom-session-id"
```

### Development Configuration

```bash
# Development environment settings
export REDIS_HOST="localhost"
export REDIS_PASSWORD="dev_password"
export REDIS_PORT="6379"
export REDIS_TLS="false"
export REDIS_PERSIST="true"
export DEBUG="1"
export CLAUDE_HOOKS_DEBUG="1"
export CLAUDE_HOOKS_LOG="/tmp/claude_hooks_dev.log"
```

### Production Configuration

```bash
# Production environment settings
export REDIS_HOST="redis-prod.example.com"
export REDIS_PASSWORD="${REDIS_PROD_PASSWORD}"  # From secrets manager
export REDIS_PORT="6380"
export REDIS_TLS="true"
export REDIS_TLS_SKIP_VERIFY="false"
export REDIS_PERSIST="true"
export DEBUG="0"
export CLAUDE_HOOKS_LOG="/var/log/claude/hooks.log"
export CLAUDE_HOOKS_SEQ="/var/lib/claude/.sequence"
```

## Hook Configuration

### Hook Script Configuration

Each hook script can have its own configuration:

```bash
#!/usr/bin/env bash
# ~/.claude/hooks/user_prompt_submit.sh

# Hook-specific configuration
HOOK_ENABLED="${USER_PROMPT_HOOK_ENABLED:-true}"
HOOK_TIMEOUT="${USER_PROMPT_HOOK_TIMEOUT:-5}"
HOOK_RETRY="${USER_PROMPT_HOOK_RETRY:-3}"

# Load global configuration
source "$HOME/.claude/redis_config.env"

# Hook-specific Redis channel (optional)
REDIS_CHANNEL="${USER_PROMPT_CHANNEL:-hooksdata}"

# Process hook...
```

### Disabling Specific Hooks

```bash
# Disable specific hooks without removing them
export SESSION_START_HOOK_ENABLED="false"
export PRE_TOOL_USE_HOOK_ENABLED="false"
```

### Hook Timeout Configuration

```bash
# Set timeouts for individual hooks (seconds)
export HOOK_TIMEOUT_DEFAULT="5"
export SESSION_START_TIMEOUT="10"
export PRE_TOOL_USE_TIMEOUT="3"
export POST_TOOL_USE_TIMEOUT="3"
```

## Redis Settings

### Connection Pooling

```bash
# Redis connection pool settings
export REDIS_MAX_RETRIES="3"
export REDIS_RETRY_DELAY="1"  # seconds
export REDIS_CONNECT_TIMEOUT="5"  # seconds
export REDIS_COMMAND_TIMEOUT="10"  # seconds
```

### TLS/SSL Configuration

```bash
# TLS certificate paths (optional)
export REDIS_TLS_CERT="/path/to/client.crt"
export REDIS_TLS_KEY="/path/to/client.key"
export REDIS_TLS_CA="/path/to/ca.crt"

# TLS options
export REDIS_TLS_SKIP_VERIFY="false"  # Never use in production
export REDIS_TLS_SERVER_NAME="redis.example.com"
```

### Redis Persistence Configuration

When `REDIS_PERSIST="true"`, data is stored using these patterns:

```bash
# Key patterns
export REDIS_KEY_PREFIX="hooks"
export REDIS_KEY_TTL="86400"  # 24 hours in seconds

# Index configuration
export REDIS_INDEX_HOOKS="true"
export REDIS_INDEX_SESSIONS="true"
export REDIS_INDEX_STATS="true"

# Storage limits
export REDIS_MAX_HOOKS_PER_SESSION="1000"
export REDIS_MAX_SESSIONS="100"
```

### Redis Cluster Configuration

```bash
# Redis Cluster support
export REDIS_CLUSTER_ENABLED="false"
export REDIS_CLUSTER_NODES="node1:6379,node2:6379,node3:6379"
export REDIS_CLUSTER_REPLICA_COUNT="1"
```

## Logging Configuration

### Log Levels

```bash
# Log level: ERROR, WARN, INFO, DEBUG
export LOG_LEVEL="INFO"

# Component-specific logging
export LOG_REDIS="INFO"
export LOG_HOOKS="DEBUG"
export LOG_JSON="WARN"
```

### Log Rotation

```bash
# Log rotation settings
export LOG_MAX_SIZE="10M"  # Max log file size
export LOG_MAX_FILES="5"    # Number of rotated files to keep
export LOG_COMPRESS="true"  # Compress rotated logs
```

### Log Format

```bash
# Log format options
export LOG_FORMAT="json"  # json, text, structured
export LOG_TIMESTAMP="ISO8601"  # ISO8601, UNIX, CUSTOM
export LOG_INCLUDE_CALLER="true"  # Include file:line in logs
```

### Custom Log Handler

```bash
# Custom log handler script
export LOG_HANDLER="/path/to/custom_logger.sh"

# Example custom logger
cat > /path/to/custom_logger.sh << 'EOF'
#!/bin/bash
# Custom log handler
while IFS= read -r line; do
    # Send to syslog
    logger -t "claude-hooks" "$line"
    # Also write to file
    echo "$line" >> /var/log/claude/custom.log
done
EOF
```

## Advanced Options

### Performance Tuning

```bash
# Performance settings
export BATCH_SIZE="10"  # Batch operations
export BUFFER_SIZE="4096"  # I/O buffer size
export PARALLEL_HOOKS="false"  # Process hooks in parallel
export ASYNC_LOGGING="true"  # Non-blocking logging
```

### Error Handling

```bash
# Error handling configuration
export ERROR_RETRY_COUNT="3"
export ERROR_RETRY_DELAY="1"  # seconds
export ERROR_FALLBACK="log"  # log, ignore, fail
export ERROR_NOTIFICATION="email"  # email, slack, webhook
```

### Monitoring Integration

```bash
# Metrics and monitoring
export METRICS_ENABLED="true"
export METRICS_ENDPOINT="http://metrics.example.com/push"
export METRICS_INTERVAL="60"  # seconds

# StatsD integration
export STATSD_ENABLED="true"
export STATSD_HOST="localhost"
export STATSD_PORT="8125"
export STATSD_PREFIX="claude.hooks"

# Prometheus integration
export PROMETHEUS_ENABLED="true"
export PROMETHEUS_PORT="9090"
export PROMETHEUS_PATH="/metrics"
```

### Security Settings

```bash
# Security configuration
export SECURE_MODE="true"
export VALIDATE_JSON="true"
export SANITIZE_INPUT="true"
export MAX_PAYLOAD_SIZE="1048576"  # 1MB in bytes
export ALLOWED_HOOK_TYPES="session_start,user_prompt_submit,notification"
```

### Filtering and Transformation

```bash
# Content filtering
export FILTER_ENABLED="true"
export FILTER_PATTERNS="/path/to/filter_patterns.txt"
export FILTER_SENSITIVE="true"  # Remove sensitive data

# Data transformation
export TRANSFORM_ENABLED="true"
export TRANSFORM_SCRIPT="/path/to/transform.sh"
export COMPRESS_PAYLOAD="gzip"  # none, gzip, zstd
```

## Configuration Examples

### Minimal Configuration

```bash
# Absolute minimum required
export REDIS_HOST="localhost"
export REDIS_PASSWORD="password"
```

### Docker Configuration

```bash
# Docker environment
export REDIS_HOST="redis"  # Docker service name
export REDIS_PORT="6379"
export REDIS_TLS="false"
export CLAUDE_HOOKS_LOG="/app/logs/hooks.log"
export CLAUDE_HOOKS_SEQ="/app/data/.sequence"
```

### Kubernetes Configuration

```yaml
# ConfigMap for Claude Hooks
apiVersion: v1
kind: ConfigMap
metadata:
  name: claude-hooks-config
data:
  REDIS_HOST: "redis-service.default.svc.cluster.local"
  REDIS_PORT: "6379"
  REDIS_TLS: "true"
  REDIS_PERSIST: "true"
  DEBUG: "0"
```

### AWS Configuration

```bash
# AWS ElastiCache Redis
export REDIS_HOST="claude-redis.abc123.ng.0001.use1.cache.amazonaws.com"
export REDIS_PORT="6379"
export REDIS_TLS="true"
export REDIS_AUTH_TOKEN="${AWS_REDIS_AUTH_TOKEN}"

# AWS CloudWatch Logs
export LOG_HANDLER="aws"
export AWS_LOG_GROUP="/aws/claude/hooks"
export AWS_LOG_STREAM="$(hostname)"
```

### Azure Configuration

```bash
# Azure Cache for Redis
export REDIS_HOST="claude-redis.redis.cache.windows.net"
export REDIS_PORT="6380"
export REDIS_TLS="true"
export REDIS_PASSWORD="${AZURE_REDIS_KEY}"

# Azure Monitor
export AZURE_APP_INSIGHTS="true"
export AZURE_INSTRUMENTATION_KEY="${AZURE_APP_INSIGHTS_KEY}"
```

## Configuration Validation

### Validation Script

```bash
#!/bin/bash
# validate_config.sh - Validate Claude Hooks configuration

# Source configuration
source ~/.claude/redis_config.env

# Validation functions
validate_required() {
    local var_name=$1
    local var_value=${!var_name}
    if [[ -z "$var_value" ]]; then
        echo "ERROR: Required variable $var_name is not set"
        return 1
    fi
    echo "✓ $var_name is set"
    return 0
}

validate_boolean() {
    local var_name=$1
    local var_value=${!var_name}
    if [[ "$var_value" != "true" && "$var_value" != "false" ]]; then
        echo "ERROR: $var_name must be 'true' or 'false', got: $var_value"
        return 1
    fi
    echo "✓ $var_name is valid boolean"
    return 0
}

validate_port() {
    local var_name=$1
    local var_value=${!var_name}
    if ! [[ "$var_value" =~ ^[0-9]+$ ]] || [ "$var_value" -lt 1 ] || [ "$var_value" -gt 65535 ]; then
        echo "ERROR: $var_name must be a valid port number (1-65535), got: $var_value"
        return 1
    fi
    echo "✓ $var_name is valid port"
    return 0
}

# Run validations
echo "Validating configuration..."
validate_required "REDIS_HOST"
validate_required "REDIS_PASSWORD"
validate_port "REDIS_PORT"
validate_boolean "REDIS_TLS"
validate_boolean "REDIS_PERSIST"

# Test Redis connection
echo "Testing Redis connection..."
if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
    --pass "$REDIS_PASSWORD" \
    ${REDIS_TLS:+--tls} ping > /dev/null 2>&1; then
    echo "✓ Redis connection successful"
else
    echo "ERROR: Cannot connect to Redis"
    exit 1
fi

echo "Configuration validation complete!"
```

## Best Practices

1. **Security**
   - Store passwords in environment variables or secrets managers
   - Use TLS for production environments
   - Restrict file permissions: `chmod 600 ~/.claude/redis_config.env`

2. **Performance**
   - Enable persistence only when needed
   - Use appropriate log levels in production
   - Configure connection pooling for high-volume environments

3. **Reliability**
   - Set appropriate timeouts
   - Configure retry mechanisms
   - Enable monitoring and alerting

4. **Maintenance**
   - Rotate logs regularly
   - Monitor disk space for logs and sequence files
   - Periodically clean up old Redis keys if persistence is enabled

5. **Development vs Production**
   - Use separate configuration files
   - Enable debug logging only in development
   - Use different Redis databases or instances