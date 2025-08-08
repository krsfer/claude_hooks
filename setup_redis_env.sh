#!/bin/bash
# Script to set up Redis environment from existing environment variables

echo "Setting up Redis environment configuration..."

# Check if environment variables are already set
if [[ -n "$REDIS_HOST" && -n "$REDIS_PORT" && -n "$REDIS_PASSWORD" ]]; then
    echo "Found existing Redis environment variables:"
    echo "  REDIS_HOST: $REDIS_HOST"
    echo "  REDIS_PORT: $REDIS_PORT"
    echo "  REDIS_PASSWORD: [HIDDEN]"
    echo "  REDIS_TLS: ${REDIS_TLS:-not set}"
    
    # Update the config file with actual values
    cat > ~/.claude/redis_config.env << EOF
#!/bin/bash
# Redis Configuration for Claude Code Hooks
# Generated: $(date)
# Auto-configured from environment variables

# Required variables
export REDIS_HOST="$REDIS_HOST"
export REDIS_PASSWORD="$REDIS_PASSWORD"
export REDIS_PORT="${REDIS_PORT:-6380}"

# Optional variables  
export REDIS_TLS="${REDIS_TLS:-true}"
export REDIS_TLS_SKIP_VERIFY="${REDIS_TLS_SKIP_VERIFY:-false}"

# Script behavior
export CLAUDE_HOOKS_LOG="\${HOME}/.claude/logs/hooks.log"
export CLAUDE_HOOKS_SEQ="\${HOME}/.claude/.sequence"
export REDIS_PERSIST="${REDIS_PERSIST:-false}"
export DEBUG="${DEBUG:-0}"
export CLAUDE_HOOKS_DEBUG="${CLAUDE_HOOKS_DEBUG:-0}"

# Monitoring
export ALERT_METHOD="${ALERT_METHOD:-log}"

# Performance
export REDIS_CONNECT_TIMEOUT="${REDIS_CONNECT_TIMEOUT:-5}"
export REDIS_COMMAND_TIMEOUT="${REDIS_COMMAND_TIMEOUT:-10}"
export MAX_PAYLOAD_SIZE="${MAX_PAYLOAD_SIZE:-1048576}"

# Log rotation
export LOG_MAX_SIZE_MB="${LOG_MAX_SIZE_MB:-50}"
export LOG_KEEP_FILES="${LOG_KEEP_FILES:-5}"
EOF
    
    chmod 600 ~/.claude/redis_config.env
    echo "Configuration saved to ~/.claude/redis_config.env"
    
else
    echo "Redis environment variables not found in current environment."
    echo "Please set the following environment variables:"
    echo "  export REDIS_HOST='your-redis-host'"
    echo "  export REDIS_PORT='18773'  # or your Redis port"
    echo "  export REDIS_PASSWORD='your-redis-password'"
    echo ""
    echo "Then run this script again."
    exit 1
fi

echo ""
echo "Testing Redis connection..."
if command -v redis-cli &> /dev/null; then
    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} ping 2>/dev/null; then
        echo "✅ Redis connection successful!"
    else
        echo "⚠️  Redis connection failed. Please check your credentials and network."
    fi
else
    echo "⚠️  redis-cli not found. Install with: brew install redis"
fi