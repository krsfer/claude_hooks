# Claude Hook Redis Publisher

A robust shell script for sending Claude Code hook data to Redis channel "hooksdata" with proper JSON structure and error handling.

## Features

- **Full Hook Support**: Handles all 8 Claude Code hook types
- **Robust Error Handling**: Comprehensive validation and error reporting
- **Sequence Tracking**: Automatic sequence number management per session
- **Platform Compatibility**: Works on macOS and Linux
- **TLS Support**: Secure Redis connections with optional TLS
- **JSON Validation**: Validates and formats JSON payloads
- **Logging**: Detailed logging with debug mode support
- **Atomic Operations**: Thread-safe sequence number management
- **Context Enrichment**: Automatically adds system context (git, platform, cwd)
- **Persistence Options**: Optional Redis key storage for long-term retention

## Installation

1. Ensure Redis CLI is installed:
```bash
# macOS
brew install redis

# Ubuntu/Debian
sudo apt-get install redis-tools

# RHEL/CentOS
sudo yum install redis
```

2. Make the script executable:
```bash
chmod +x claude_hook_redis.sh
```

## Configuration

### Required Environment Variables

```bash
export REDIS_HOST="your-redis-host.com"
export REDIS_PASSWORD="your-redis-password"
```

### Optional Environment Variables

```bash
# Redis connection
export REDIS_PORT="6380"              # Default: 6380
export REDIS_TLS="true"                # Default: true
export REDIS_TLS_SKIP_VERIFY="false"   # Default: false

# Script behavior
export CLAUDE_HOOKS_LOG="/var/log/claude_hooks.log"  # Default: /tmp/claude_hooks.log
export CLAUDE_HOOKS_SEQ="/var/lib/claude/.seq"       # Default: /tmp/.claude_hooks_seq
export REDIS_PERSIST="true"            # Store hooks in Redis keys (default: false)
export DEBUG="1"                        # Enable debug logging (default: 0)
```

## Usage

### Basic Usage

```bash
# Send hook data from stdin
echo '{"prompt": "Hello Claude"}' | ./claude_hook_redis.sh user_prompt_submit session-123

# Send empty payload
echo '{}' | ./claude_hook_redis.sh notification session-456

# No stdin (uses empty payload)
./claude_hook_redis.sh stop_hook session-789 < /dev/null
```

### Hook Types

All 8 Claude Code hook types are supported:

1. **session_start** - New session initialization
```bash
echo '{
  "source": "startup",
  "cwd": "/home/user/project",
  "transcript_path": "/tmp/transcript.txt"
}' | ./claude_hook_redis.sh session_start sess-001
```

2. **user_prompt_submit** - User submits a prompt
```bash
echo '{
  "prompt": "Write a Python function to sort a list"
}' | ./claude_hook_redis.sh user_prompt_submit sess-001
```

3. **pre_tool_use** - Before tool execution
```bash
echo '{
  "tool_name": "bash",
  "tool_input": {"command": "ls -la"}
}' | ./claude_hook_redis.sh pre_tool_use sess-001
```

4. **post_tool_use** - After tool execution
```bash
echo '{
  "tool_name": "bash",
  "tool_input": {"command": "ls"},
  "tool_response": {"output": "file1.txt\nfile2.txt"},
  "execution_time_ms": 45
}' | ./claude_hook_redis.sh post_tool_use sess-001
```

5. **notification** - System notifications
```bash
echo '{
  "message": "Task completed successfully"
}' | ./claude_hook_redis.sh notification sess-001
```

6. **stop_hook** - Stop hook activation
```bash
echo '{
  "stop_hook_active": true
}' | ./claude_hook_redis.sh stop_hook sess-001
```

7. **sub_agent_stop_hook** - Sub-agent stop hook
```bash
echo '{
  "stop_hook_active": false
}' | ./claude_hook_redis.sh sub_agent_stop_hook sess-001
```

8. **pre_compact** - Before context compaction
```bash
echo '{
  "trigger": "auto",
  "custom_instructions": "Preserve important context"
}' | ./claude_hook_redis.sh pre_compact sess-001
```

## Integration with Claude Code Hooks

### Example Hook Script

Create a hook script that calls the Redis publisher:

```bash
#!/bin/bash
# ~/.claude/hooks/user_prompt_submit.sh

# Read hook data from Claude Code
HOOK_DATA=$(cat)

# Extract session ID from environment or generate one
SESSION_ID="${CLAUDE_SESSION_ID:-$(uuidgen)}"

# Send to Redis
echo "$HOOK_DATA" | /path/to/claude_hook_redis.sh user_prompt_submit "$SESSION_ID"
```

### Setting Up All Hooks

```bash
#!/bin/bash
# setup_hooks.sh

HOOKS_DIR="$HOME/.claude/hooks"
SCRIPT_PATH="/path/to/claude_hook_redis.sh"

# Create hooks directory
mkdir -p "$HOOKS_DIR"

# Create hook scripts for all types
for hook in session_start user_prompt_submit pre_tool_use post_tool_use \
           notification stop_hook sub_agent_stop_hook pre_compact; do
    
    cat > "$HOOKS_DIR/${hook}.sh" << 'EOF'
#!/bin/bash
SESSION_ID="${CLAUDE_SESSION_ID:-$(uuidgen)}"
HOOK_DATA=$(cat)
echo "$HOOK_DATA" | /path/to/claude_hook_redis.sh HOOK_TYPE "$SESSION_ID"
EOF
    
    # Replace HOOK_TYPE with actual hook name
    sed -i "s/HOOK_TYPE/${hook}/g" "$HOOKS_DIR/${hook}.sh"
    chmod +x "$HOOKS_DIR/${hook}.sh"
done
```

## JSON Structure

The script generates JSON following this structure:

```json
{
  "id": "unique-uuid-v4",
  "hook_type": "hook_type_name",
  "timestamp": "2025-08-07T10:30:45.123Z",
  "session_id": "session-uuid",
  "sequence": 42,
  "core": {
    "status": "success",
    "execution_time_ms": 5
  },
  "payload": {
    // Hook-specific data from stdin
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

## Redis Storage

### Channel Publishing

Data is published to the `hooksdata` channel:
```bash
PUBLISH hooksdata '{"id":"...","hook_type":"...",...}'
```

### Optional Persistence

When `REDIS_PERSIST=true`, data is also stored in Redis keys:

```bash
# Hook data
HSET hooks:session-id:sequence data '{json}'

# Indexes
ZADD hooks:index:hook_type timestamp session-id:sequence
ZADD hooks:session:session-id timestamp sequence

# Statistics
HINCRBY hooks:stats:2025-08-07 hook_type_count 1
```

## Testing

Run the test suite:

```bash
# Run all tests
./test_claude_hook_redis.sh

# Quick tests only
./test_claude_hook_redis.sh --quick

# With debug output
DEBUG=1 ./test_claude_hook_redis.sh
```

## Monitoring

### Log Files

Check the log file for detailed information:
```bash
tail -f /tmp/claude_hooks.log
```

### Debug Mode

Enable debug output:
```bash
DEBUG=1 ./claude_hook_redis.sh notification test-session < payload.json
```

### Redis Monitoring

Monitor the Redis channel:
```bash
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD SUBSCRIBE hooksdata
```

## Error Handling

The script uses specific exit codes:

- `0` - Success
- `1` - Invalid arguments
- `2` - Redis connection/operation error
- `3` - JSON parsing error
- `4` - Environment configuration error

## Performance Considerations

- **Sequence Numbers**: Cached per session with atomic file locking
- **JSON Processing**: Uses `jq` when available for validation
- **Redis Connection**: Reuses connection for multiple operations
- **Logging**: Async logging to prevent blocking
- **Payload Size**: Handles large payloads efficiently

## Security

- **Password Protection**: Redis password never logged
- **TLS Support**: Encrypted connections to Redis
- **Input Validation**: All inputs validated and sanitized
- **File Permissions**: Secure handling of sequence and log files
- **No Code Execution**: No eval or dynamic code execution

## Troubleshooting

### Common Issues

1. **Redis connection failed**
   - Check REDIS_HOST and REDIS_PORT
   - Verify REDIS_PASSWORD is correct
   - Check TLS settings match your Redis configuration

2. **Permission denied**
   - Ensure script is executable: `chmod +x claude_hook_redis.sh`
   - Check write permissions for log and sequence files

3. **Invalid JSON**
   - Validate JSON with: `echo "$JSON" | jq .`
   - Check for special characters that need escaping

4. **Sequence number issues**
   - Check sequence file permissions
   - Remove lock file if stuck: `rm /tmp/.claude_hooks_seq.lock`

### Debug Commands

```bash
# Test Redis connection
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD ping

# Validate JSON payload
echo '{"test": "data"}' | jq .

# Check script syntax
bash -n claude_hook_redis.sh

# Run with trace
bash -x claude_hook_redis.sh notification test-123 < test.json
```

## License

MIT License - See LICENSE file for details

## Contributing

Contributions are welcome! Please ensure:
- Code follows existing style
- Tests pass
- Documentation is updated
- Error handling is comprehensive

## Version History

- 1.0.0 - Initial release with full hook support