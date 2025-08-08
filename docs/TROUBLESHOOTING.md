# Troubleshooting Guide

Comprehensive troubleshooting guide for common issues with the Claude Code Redis hook integration.

## Table of Contents
- [Quick Diagnostics](#quick-diagnostics)
- [Common Issues](#common-issues)
- [Connection Problems](#connection-problems)
- [Hook Execution Issues](#hook-execution-issues)
- [Data and Formatting Issues](#data-and-formatting-issues)
- [Performance Issues](#performance-issues)
- [Debugging Techniques](#debugging-techniques)
- [Recovery Procedures](#recovery-procedures)

## Quick Diagnostics

### Health Check Script

```bash
#!/bin/bash
# Quick health check for Claude Redis Hooks

echo "Claude Redis Hooks Health Check"
echo "================================"

# Check configuration
if [ -f ~/.claude/redis_config.env ]; then
    echo "✓ Configuration file exists"
    source ~/.claude/redis_config.env
else
    echo "✗ Configuration file missing"
    exit 1
fi

# Check Redis connection
if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
    --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} ping > /dev/null 2>&1; then
    echo "✓ Redis connection successful"
else
    echo "✗ Redis connection failed"
fi

# Check hook scripts
if [ -d ~/.claude/hooks ]; then
    hook_count=$(ls -1 ~/.claude/hooks/*.sh 2>/dev/null | wc -l)
    echo "✓ Hook scripts installed: $hook_count"
else
    echo "✗ Hook directory missing"
fi

# Check main script
if [ -x ./claude_hook_redis.sh ]; then
    echo "✓ Main script is executable"
else
    echo "✗ Main script not found or not executable"
fi

# Check log file
if [ -f "$CLAUDE_HOOKS_LOG" ]; then
    echo "✓ Log file exists"
    recent_logs=$(tail -n 1 "$CLAUDE_HOOKS_LOG" 2>/dev/null)
    if [ -n "$recent_logs" ]; then
        echo "  Last log entry: $(echo "$recent_logs" | cut -d' ' -f1-3)"
    fi
else
    echo "⚠ Log file not found (will be created on first use)"
fi

# Test hook sending
echo -n "Testing hook send... "
if echo '{"test": true}' | ./claude_hook_redis.sh notification test-health 2>/dev/null; then
    echo "✓ Success"
else
    echo "✗ Failed"
fi
```

### Common Diagnostic Commands

```bash
# Check Redis connectivity
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD ping

# Test TLS connection
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD --tls ping

# Check Redis info
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD INFO server

# Monitor Redis channel
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD MONITOR

# Check recent logs
tail -f ~/.claude/logs/hooks.log

# Debug mode test
DEBUG=1 echo '{"test": true}' | ./claude_hook_redis.sh notification test-debug
```

## Common Issues

### Issue: "Command not found"

**Symptom:**
```
bash: ./claude_hook_redis.sh: No such file or directory
```

**Solutions:**

1. Check script location:
```bash
ls -la claude_hook_redis.sh
pwd  # Verify current directory
```

2. Use full path:
```bash
/full/path/to/claude_hook_redis.sh notification test
```

3. Add to PATH:
```bash
export PATH="$PATH:/path/to/claude-hooks"
```

4. Fix permissions:
```bash
chmod +x claude_hook_redis.sh
```

### Issue: "Permission denied"

**Symptom:**
```
bash: ./claude_hook_redis.sh: Permission denied
```

**Solutions:**

1. Make script executable:
```bash
chmod +x claude_hook_redis.sh
chmod +x ~/.claude/hooks/*.sh
```

2. Check file ownership:
```bash
ls -la claude_hook_redis.sh
# If needed:
sudo chown $USER:$USER claude_hook_redis.sh
```

3. Check directory permissions:
```bash
chmod 755 ~/.claude/hooks
chmod 600 ~/.claude/redis_config.env
```

### Issue: "Environment variables not set"

**Symptom:**
```
[ERROR] REDIS_HOST environment variable is required
```

**Solutions:**

1. Set environment variables:
```bash
export REDIS_HOST="your-redis-host"
export REDIS_PASSWORD="your-password"
```

2. Source configuration file:
```bash
source ~/.claude/redis_config.env
```

3. Check variable values:
```bash
echo "REDIS_HOST: $REDIS_HOST"
echo "REDIS_PASSWORD: ${REDIS_PASSWORD:+[SET]}"
```

4. Add to shell profile:
```bash
echo 'source ~/.claude/redis_config.env' >> ~/.bashrc
# or for zsh:
echo 'source ~/.claude/redis_config.env' >> ~/.zshrc
```

## Connection Problems

### Issue: "Could not connect to Redis"

**Symptom:**
```
Could not connect to Redis at redis.example.com:6380: Connection refused
```

**Solutions:**

1. Verify Redis is running:
```bash
# Check if Redis is accessible
nc -zv $REDIS_HOST $REDIS_PORT

# Test with telnet
telnet $REDIS_HOST $REDIS_PORT
```

2. Check network connectivity:
```bash
# Ping the host (if ICMP is allowed)
ping -c 3 $REDIS_HOST

# DNS resolution
nslookup $REDIS_HOST
dig $REDIS_HOST
```

3. Verify credentials:
```bash
# Test authentication
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD ping
```

4. Check firewall rules:
```bash
# Local firewall
sudo iptables -L -n | grep 6380
sudo ufw status | grep 6380

# Test from different network
curl -v telnet://$REDIS_HOST:$REDIS_PORT
```

### Issue: "TLS handshake failed"

**Symptom:**
```
Error: Connection reset by peer
```

**Solutions:**

1. Verify TLS settings:
```bash
# Test without TLS
REDIS_TLS=false redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD ping

# Test with TLS
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD --tls ping
```

2. Skip certificate verification (development only):
```bash
export REDIS_TLS_SKIP_VERIFY=true
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD --tls --insecure ping
```

3. Check certificate:
```bash
# View server certificate
openssl s_client -connect $REDIS_HOST:$REDIS_PORT -servername $REDIS_HOST
```

### Issue: "Authentication failed"

**Symptom:**
```
NOAUTH Authentication required
ERR invalid password
```

**Solutions:**

1. Verify password:
```bash
# Check if password is set
echo "Password is: ${REDIS_PASSWORD:+[SET]}"

# Test with redis-cli
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass "$REDIS_PASSWORD" ping
```

2. Check for special characters:
```bash
# Escape special characters in password
export REDIS_PASSWORD='your$special@password!'
```

3. Use auth token (AWS ElastiCache):
```bash
export REDIS_AUTH_TOKEN="your-auth-token"
redis-cli -h $REDIS_HOST -p $REDIS_PORT --user default --pass $REDIS_AUTH_TOKEN ping
```

## Hook Execution Issues

### Issue: "Invalid hook type"

**Symptom:**
```
[ERROR] Invalid hook type: invalid_hook
```

**Solutions:**

1. Check valid hook types:
```bash
# Valid types are:
# session_start, user_prompt_submit, pre_tool_use, post_tool_use,
# notification, stop_hook, sub_agent_stop_hook, pre_compact

# Correct usage:
echo '{}' | ./claude_hook_redis.sh notification session-123
```

2. Check for typos:
```bash
# Common mistakes:
# "session-start" should be "session_start"
# "pre_tool" should be "pre_tool_use"
```

### Issue: "Hooks not triggering"

**Symptom:**
Hooks are installed but not being executed by Claude Code.

**Solutions:**

1. Verify hook directory:
```bash
# Check Claude Code hooks directory
ls -la ~/.claude/hooks/

# Ensure scripts are executable
chmod +x ~/.claude/hooks/*.sh
```

2. Test hooks manually:
```bash
# Test individual hook
echo '{"test": true}' | ~/.claude/hooks/notification.sh
```

3. Check Claude Code configuration:
```bash
# Verify Claude Code is using the correct hooks directory
# The directory should be ~/.claude/hooks/
```

4. Monitor hook execution:
```bash
# Add debug logging to hook script
cat >> ~/.claude/hooks/notification.sh << 'EOF'
echo "[$(date)] Hook triggered: notification" >> /tmp/hook_debug.log
EOF

# Monitor the debug log
tail -f /tmp/hook_debug.log
```

### Issue: "Sequence number issues"

**Symptom:**
```
Could not acquire lock for sequence file
```

**Solutions:**

1. Remove stale lock:
```bash
rm -f ~/.claude/.sequence.lock
rm -f /tmp/.claude_hooks_seq.lock
```

2. Reset sequence file:
```bash
# Backup existing
mv ~/.claude/.sequence ~/.claude/.sequence.backup

# Create new
touch ~/.claude/.sequence
chmod 644 ~/.claude/.sequence
```

3. Check disk space:
```bash
df -h /tmp
df -h ~/.claude
```

## Data and Formatting Issues

### Issue: "Invalid JSON"

**Symptom:**
```
parse error: Invalid numeric literal at line 1, column 10
```

**Solutions:**

1. Validate JSON:
```bash
# Test with jq
echo '{"test": true}' | jq .

# Common issues:
# - Missing quotes around strings
# - Trailing commas
# - Single quotes instead of double quotes
```

2. Fix common JSON errors:
```bash
# Escape special characters
echo '{"message": "Line with \"quotes\""}' | jq .

# Multi-line strings
cat << 'EOF' | jq .
{
  "message": "Multi\nline\nstring"
}
EOF
```

3. Use JSON builders:
```bash
# Using jq to build JSON
jq -n \
  --arg msg "Hello, World!" \
  --arg user "$USER" \
  '{message: $msg, user: $user}'
```

### Issue: "Payload too large"

**Symptom:**
```
Payload exceeds maximum size limit
```

**Solutions:**

1. Check payload size:
```bash
echo '{"data": "..."}' | wc -c
```

2. Compress large payloads:
```bash
# Compress before sending
echo '{"large": "data"}' | gzip | base64 | \
  jq -R '{compressed: true, data: .}' | \
  ./claude_hook_redis.sh notification session-123
```

3. Split into multiple hooks:
```bash
# Split large data
split_and_send() {
  local data="$1"
  local chunk_size=1000
  local part=0
  
  while [ -n "$data" ]; do
    chunk="${data:0:$chunk_size}"
    data="${data:$chunk_size}"
    
    echo "{\"part\": $part, \"data\": \"$chunk\"}" | \
      ./claude_hook_redis.sh notification session-123
    
    ((part++))
  done
}
```

## Performance Issues

### Issue: "Slow hook execution"

**Symptom:**
Hooks take several seconds to complete.

**Solutions:**

1. Enable async processing:
```bash
# Send hooks in background
echo '{"data": "test"}' | ./claude_hook_redis.sh notification session-123 &
```

2. Check network latency:
```bash
# Measure Redis latency
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD --latency

# Ping test
ping -c 10 $REDIS_HOST
```

3. Optimize Redis connection:
```bash
# Use connection pooling
export REDIS_MAX_RETRIES=1
export REDIS_CONNECT_TIMEOUT=2
```

### Issue: "High memory usage"

**Solutions:**

1. Clean up old logs:
```bash
# Rotate logs
mv ~/.claude/logs/hooks.log ~/.claude/logs/hooks.log.old
touch ~/.claude/logs/hooks.log

# Compress old logs
gzip ~/.claude/logs/*.old
```

2. Clear sequence file:
```bash
# Remove old sessions from sequence file
awk -F: '$1 ~ /^'$(date +%Y%m%d)'/' ~/.claude/.sequence > /tmp/sequence.new
mv /tmp/sequence.new ~/.claude/.sequence
```

## Debugging Techniques

### Enable Debug Mode

```bash
# Global debug
export DEBUG=1

# Run with debug
DEBUG=1 ./claude_hook_redis.sh notification test-session < test.json

# Add to configuration
echo 'export DEBUG=1' >> ~/.claude/redis_config.env
```

### Trace Execution

```bash
# Bash trace mode
bash -x ./claude_hook_redis.sh notification test-session < test.json

# Set trace in script
set -x  # Enable trace
set +x  # Disable trace
```

### Monitor Redis

```bash
# Monitor all Redis commands
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD MONITOR

# Subscribe to channel
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD SUBSCRIBE hooksdata

# Check published messages count
redis-cli -h $REDIS_HOST -p $REDIS_PORT --pass $REDIS_PASSWORD PUBSUB NUMSUB hooksdata
```

### Logging Analysis

```bash
# Check error patterns
grep ERROR ~/.claude/logs/hooks.log | tail -20

# Count hook types
grep "Processing hook" ~/.claude/logs/hooks.log | \
  awk '{print $NF}' | sort | uniq -c

# Find failed hooks
grep "Failed" ~/.claude/logs/hooks.log

# Performance analysis
grep "execution_time_ms" ~/.claude/logs/hooks.log | \
  awk -F'"' '{print $4}' | sort -n | tail -10
```

## Recovery Procedures

### Full Reset

```bash
#!/bin/bash
# Complete reset of Claude Redis Hooks

echo "Resetting Claude Redis Hooks..."

# Backup current configuration
mkdir -p ~/.claude/backup
cp ~/.claude/redis_config.env ~/.claude/backup/ 2>/dev/null
cp -r ~/.claude/hooks ~/.claude/backup/ 2>/dev/null

# Remove current installation
rm -rf ~/.claude/hooks
rm -f ~/.claude/.sequence
rm -f ~/.claude/.sequence.lock
rm -f ~/.claude/.current_session

# Clear logs
> ~/.claude/logs/hooks.log

# Reinstall
./example_integration.sh install

echo "Reset complete. Please reconfigure with: ./example_integration.sh setup"
```

### Restore from Backup

```bash
#!/bin/bash
# Restore from backup

if [ -d ~/.claude/backup ]; then
    cp ~/.claude/backup/redis_config.env ~/.claude/
    cp -r ~/.claude/backup/hooks ~/.claude/
    echo "Restored from backup"
else
    echo "No backup found"
fi
```

### Emergency Stop

```bash
#!/bin/bash
# Stop all hook processing

# Kill any running hook processes
pkill -f claude_hook_redis.sh

# Disable hooks temporarily
for hook in ~/.claude/hooks/*.sh; do
    mv "$hook" "$hook.disabled"
done

echo "All hooks disabled. To re-enable:"
echo "for f in ~/.claude/hooks/*.disabled; do mv \"\$f\" \"\${f%.disabled}\"; done"
```

## Getting Help

If issues persist:

1. **Collect Diagnostics:**
```bash
# Generate diagnostic report
cat > /tmp/claude_hooks_diagnostic.txt << EOF
Date: $(date)
System: $(uname -a)
Shell: $SHELL
Redis CLI: $(redis-cli --version)
Environment:
$(env | grep -E '^(REDIS_|CLAUDE_)' | sed 's/PASSWORD=.*/PASSWORD=[REDACTED]/')

Recent Logs:
$(tail -50 ~/.claude/logs/hooks.log)

Hook Scripts:
$(ls -la ~/.claude/hooks/)

Test Results:
$(./test_claude_hook_redis.sh --quick 2>&1)
EOF

echo "Diagnostic report saved to /tmp/claude_hooks_diagnostic.txt"
```

2. **Check Documentation:**
   - Review [Configuration Reference](CONFIGURATION.md)
   - Check [Usage Examples](USAGE_EXAMPLES.md)
   - Read [Integration Guide](INTEGRATION.md)

3. **Common Solutions Checklist:**
   - [ ] Redis connection verified
   - [ ] Environment variables set
   - [ ] Scripts are executable
   - [ ] Correct hook directory
   - [ ] Valid JSON payloads
   - [ ] Sufficient disk space
   - [ ] Proper file permissions
   - [ ] No stale lock files