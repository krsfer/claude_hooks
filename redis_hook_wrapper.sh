#!/usr/bin/env bash

# Redis Hook Wrapper
# Automatically sources Redis configuration and calls the main hook script
# Usage: redis_hook_wrapper.sh <hook_type> <session_id> < payload.json

set -euo pipefail

# Source Redis configuration
source ~/.claude/redis_config.env 2>/dev/null || {
    echo "Warning: Could not load Redis configuration from ~/.claude/redis_config.env" >&2
}

# Call the main hook script with all arguments
exec "/Users/chris/dev/src/android/claude_hooks/claude_hook_redis.sh" "$@"