# Installation and Setup Guide

This guide provides step-by-step instructions for setting up the Claude Code Redis hook integration system.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Quick Setup](#quick-setup)
- [Manual Installation](#manual-installation)
- [Automated Installation](#automated-installation)
- [Verification](#verification)
- [Next Steps](#next-steps)

## Prerequisites

### System Requirements
- **Operating System**: macOS, Linux (Ubuntu/Debian/RHEL/CentOS)
- **Shell**: Bash 4.0+ or compatible
- **Redis Server**: 5.0+ with optional TLS support
- **Claude Code**: Installed and configured

### Required Software

#### 1. Redis CLI Tools
```bash
# macOS
brew install redis

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install redis-tools

# RHEL/CentOS/Fedora
sudo yum install redis
# or
sudo dnf install redis

# Alpine Linux
apk add redis

# Verify installation
redis-cli --version
```

#### 2. Optional but Recommended
```bash
# jq for JSON processing
# macOS
brew install jq

# Linux
sudo apt-get install jq  # Debian/Ubuntu
sudo yum install jq       # RHEL/CentOS

# uuidgen for session IDs
# Usually pre-installed, verify with:
which uuidgen
```

## Quick Setup

For the fastest setup, use the automated installer:

```bash
# 1. Clone or download the scripts
git clone <repository_url> claude-redis-hooks
cd claude-redis-hooks

# 2. Make scripts executable
chmod +x *.sh

# 3. Run the automated setup
./example_integration.sh setup

# 4. Follow the prompts to:
#    - Enter Redis connection details
#    - Install hook scripts
#    - Test the connection
```

## Manual Installation

### Step 1: Download Scripts

```bash
# Create directory for scripts
mkdir -p ~/claude-redis-hooks
cd ~/claude-redis-hooks

# Download the main scripts
# - claude_hook_redis.sh (main publisher)
# - test_claude_hook_redis.sh (test suite)
# - example_integration.sh (installer)

# Make them executable
chmod +x *.sh
```

### Step 2: Configure Redis Connection

Create the configuration file:

```bash
# Create Claude configuration directory
mkdir -p ~/.claude/logs

# Create configuration file
cat > ~/.claude/redis_config.env << 'EOF'
# Redis Configuration for Claude Hooks
export REDIS_HOST="your-redis-host.com"
export REDIS_PASSWORD="your-redis-password"
export REDIS_PORT="6380"
export REDIS_TLS="true"
export REDIS_PERSIST="false"
export CLAUDE_HOOKS_LOG="$HOME/.claude/logs/hooks.log"
export CLAUDE_HOOKS_SEQ="$HOME/.claude/.sequence"
EOF

# Secure the configuration file
chmod 600 ~/.claude/redis_config.env
```

### Step 3: Test Redis Connection

```bash
# Source the configuration
source ~/.claude/redis_config.env

# Test basic connectivity
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
    --pass "$REDIS_PASSWORD" \
    ${REDIS_TLS:+--tls} ping

# Expected output: PONG
```

### Step 4: Create Hook Scripts

Create the hooks directory:

```bash
mkdir -p ~/.claude/hooks
```

Create a generic hook wrapper:

```bash
cat > ~/.claude/hooks/hook_wrapper.sh << 'EOF'
#!/usr/bin/env bash
# Generic Claude Hook Wrapper

set -euo pipefail

# Get hook type from script name
HOOK_TYPE=$(basename "$0" .sh)

# Load configuration
source "$HOME/.claude/redis_config.env"

# Generate or retrieve session ID
if [[ -z "${CLAUDE_SESSION_ID:-}" ]]; then
    SESSION_FILE="$HOME/.claude/.current_session"
    if [[ -f "$SESSION_FILE" ]]; then
        CLAUDE_SESSION_ID=$(cat "$SESSION_FILE")
    else
        CLAUDE_SESSION_ID="claude-$(date +%Y%m%d-%H%M%S)-$$"
        echo "$CLAUDE_SESSION_ID" > "$SESSION_FILE"
    fi
fi

# Read hook data from stdin
HOOK_DATA=$(cat)

# Send to Redis
echo "$HOOK_DATA" | ~/claude-redis-hooks/claude_hook_redis.sh "$HOOK_TYPE" "$CLAUDE_SESSION_ID"

# Pass through the data (required for Claude Code)
echo "$HOOK_DATA"
EOF

chmod +x ~/.claude/hooks/hook_wrapper.sh
```

Create individual hook scripts:

```bash
# List of all hook types
HOOKS=(
    "session_start"
    "user_prompt_submit"
    "pre_tool_use"
    "post_tool_use"
    "notification"
    "stop_hook"
    "sub_agent_stop_hook"
    "pre_compact"
)

# Create symlinks for each hook
cd ~/.claude/hooks
for hook in "${HOOKS[@]}"; do
    ln -sf hook_wrapper.sh "${hook}.sh"
done
```

### Step 5: Verify Installation

```bash
# Check that all hook scripts exist
ls -la ~/.claude/hooks/

# Test a hook manually
echo '{"test": "message"}' | ~/.claude/hooks/notification.sh

# Check the log file
tail -f ~/.claude/logs/hooks.log
```

## Automated Installation

The `example_integration.sh` script provides an interactive installation process:

### Features
- Interactive configuration setup
- Automatic hook script generation
- Connection testing
- Status monitoring
- Easy uninstallation

### Usage

```bash
# Interactive menu
./example_integration.sh

# Direct commands
./example_integration.sh setup      # Full setup
./example_integration.sh install    # Install hooks only
./example_integration.sh test       # Test connection
./example_integration.sh monitor    # Monitor Redis channel
./example_integration.sh status     # Show installation status
./example_integration.sh uninstall  # Remove hooks
```

### Menu Options

1. **Setup Redis configuration**: Create or update Redis connection settings
2. **Install hooks**: Install all Claude Code hook scripts
3. **Test connection**: Verify Redis connectivity
4. **Send test hook**: Send a test message to Redis
5. **Monitor hooks**: Live monitoring of Redis channel
6. **Show status**: Display current installation status
7. **Uninstall hooks**: Remove all hook scripts
8. **Exit**: Quit the installer

## Verification

### 1. Check Installation Status

```bash
# Using the integration script
./example_integration.sh status

# Manual check
echo "Configuration file: $(test -f ~/.claude/redis_config.env && echo '✓' || echo '✗')"
echo "Hooks directory: $(test -d ~/.claude/hooks && echo '✓' || echo '✗')"
echo "Hook scripts: $(ls -1 ~/.claude/hooks/*.sh 2>/dev/null | wc -l) installed"
```

### 2. Test Redis Connection

```bash
# Source configuration
source ~/.claude/redis_config.env

# Test connection
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
    --pass "$REDIS_PASSWORD" \
    ${REDIS_TLS:+--tls} ping
```

### 3. Send Test Hook

```bash
# Send a test notification
echo '{"message": "Installation test"}' | \
    ~/claude-redis-hooks/claude_hook_redis.sh notification test-session

# Check the log
tail -n 10 ~/.claude/logs/hooks.log
```

### 4. Monitor Redis Channel

In a separate terminal:

```bash
# Monitor the hooksdata channel
source ~/.claude/redis_config.env
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
    --pass "$REDIS_PASSWORD" \
    ${REDIS_TLS:+--tls} SUBSCRIBE hooksdata
```

### 5. Run Test Suite

```bash
# Run comprehensive tests
cd ~/claude-redis-hooks
./test_claude_hook_redis.sh

# Quick test
./test_claude_hook_redis.sh --quick
```

## Next Steps

Once installation is complete:

1. **Configure Claude Code**: Ensure Claude Code is configured to use the hooks directory
2. **Test with Claude Code**: Start a Claude Code session and verify hooks are triggered
3. **Set up monitoring**: Configure your monitoring system to consume Redis messages
4. **Review security**: Ensure Redis credentials are properly secured
5. **Customize hooks**: Modify hook scripts for your specific needs

## Troubleshooting Installation

### Common Issues

**Script not found**
```bash
# Ensure scripts are in the correct location
ls -la ~/claude-redis-hooks/

# Update paths in hook scripts if needed
sed -i 's|~/claude-redis-hooks|/actual/path/to/scripts|g' ~/.claude/hooks/*.sh
```

**Permission denied**
```bash
# Fix script permissions
chmod +x ~/claude-redis-hooks/*.sh
chmod +x ~/.claude/hooks/*.sh

# Fix configuration file permissions
chmod 600 ~/.claude/redis_config.env
```

**Redis connection failed**
```bash
# Verify Redis is running
redis-cli -h localhost ping

# Check network connectivity
nc -zv $REDIS_HOST $REDIS_PORT

# Verify TLS settings
# Try without TLS
REDIS_TLS=false redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --pass "$REDIS_PASSWORD" ping
```

**Hooks not triggering**
```bash
# Verify Claude Code hooks directory
echo "Claude hooks directory: ~/.claude/hooks"

# Check if hooks are executable
ls -la ~/.claude/hooks/*.sh

# Test hook manually
echo '{"test": true}' | ~/.claude/hooks/notification.sh
```

## Security Considerations

1. **Protect Configuration File**
   ```bash
   chmod 600 ~/.claude/redis_config.env
   ls -la ~/.claude/redis_config.env
   ```

2. **Use Environment Variables**
   - Never hardcode passwords in scripts
   - Use the configuration file for sensitive data

3. **Enable TLS for Production**
   ```bash
   export REDIS_TLS="true"
   ```

4. **Restrict Redis Access**
   - Use firewall rules
   - Configure Redis ACLs
   - Use strong passwords

5. **Monitor Access Logs**
   ```bash
   tail -f ~/.claude/logs/hooks.log
   ```

## Support

For issues or questions:
1. Check the [Troubleshooting Guide](TROUBLESHOOTING.md)
2. Review the [Configuration Reference](CONFIGURATION.md)
3. Run the test suite for diagnostics
4. Check Redis connectivity and permissions