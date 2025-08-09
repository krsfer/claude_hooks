#!/usr/bin/env bash

# Debug hook to capture raw payload structure
set -euo pipefail

# Source Redis configuration
if [[ -f ~/.claude/redis_config.env ]]; then
    source ~/.claude/redis_config.env
fi

hook_type="$1"
session_id="$2"

# Capture timestamp
timestamp=$(date '+%Y-%m-%d %H:%M:%S')

# Read stdin payload
payload=""
if [[ ! -t 0 ]]; then
    payload=$(cat)
fi

# Log everything to a debug file (use allowed directory)
{
    echo "=== $timestamp ==="
    echo "Hook Type: $hook_type"
    echo "Session ID: $session_id"
    echo "Payload Length: ${#payload}"
    echo "Raw Payload: $payload"
    echo "==="
    echo ""
} >> "/Users/chris/dev/src/android/claude_hooks/debug_payloads.log"

# Pass through to the wrapper for normal processing
if [[ -n "$payload" ]]; then
    echo "$payload" | /Users/chris/dev/src/android/claude_hooks/claude_hook_redis_wrapper.sh "$hook_type" "$session_id"
else
    echo "{}" | /Users/chris/dev/src/android/claude_hooks/claude_hook_redis_wrapper.sh "$hook_type" "$session_id"
fi