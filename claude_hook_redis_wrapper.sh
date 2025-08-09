#!/usr/bin/env bash

# Claude Hook Redis Wrapper
# Enhances payloads before sending to Redis
# Version: 1.0.0

set -euo pipefail

# Get the directory of this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Source Redis configuration
if [[ -f ~/.claude/redis_config.env ]]; then
    source ~/.claude/redis_config.env
fi

# Validate arguments
if [[ $# -lt 2 ]]; then
    echo "Error: Missing required arguments" >&2
    echo "Usage: $0 <hook_type> <session_id>" >&2
    echo "Payload is read from stdin" >&2
    exit 1
fi

hook_type="$1"
session_id="$2"

# Read payload from stdin
payload=""
if [[ ! -t 0 ]]; then
    payload=$(cat)
fi

# Default to empty JSON if no payload
if [[ -z "$payload" ]]; then
    payload="{}"
fi

# Log the original payload for debugging
if [[ "${DEBUG:-0}" == "1" ]]; then
    echo "[DEBUG] Original payload: $payload" >&2
    echo "[DEBUG] Hook type: $hook_type" >&2
    echo "[DEBUG] Session ID: $session_id" >&2
fi

# Enhance the payload
enhanced_payload=$(echo "$payload" | "${SCRIPT_DIR}/enhance_hook_payload.sh" "$hook_type")

# Log the enhanced payload for debugging
if [[ "${DEBUG:-0}" == "1" ]]; then
    echo "[DEBUG] Enhanced payload: $enhanced_payload" >&2
fi

# Send the enhanced payload to the original Redis hook script
echo "$enhanced_payload" | "${SCRIPT_DIR}/claude_hook_redis.sh" "$hook_type" "$session_id"