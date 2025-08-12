#!/usr/bin/env bash

# Debug script to test tool name extraction
set -euo pipefail

# Source Redis configuration
if [[ -f ~/.claude/redis_config.env ]]; then
    source ~/.claude/redis_config.env
fi

echo "Testing tool name extraction with various payloads..."

# Test 1: Payload with Unknown tool_name (should be overridden)
echo "=== Test 1: Unknown tool_name with command field ===" 
echo '{"tool_name": "Unknown", "command": "ls -la"}' | DEBUG=1 ./claude_hook_redis.sh pre_tool_use test-session-1 2>&1 | grep -E "(DEBUG|INFO.*tool|ERROR)" || true

echo ""
echo "=== Test 2: Unknown tool_name with file_path field ==="
echo '{"tool_name": "Unknown", "file_path": "/tmp/test.txt"}' | DEBUG=1 ./claude_hook_redis.sh pre_tool_use test-session-2 2>&1 | grep -E "(DEBUG|INFO.*tool|ERROR)" || true

echo ""
echo "=== Test 3: Valid tool_name ==="
echo '{"tool_name": "Bash", "command": "whoami"}' | DEBUG=1 ./claude_hook_redis.sh pre_tool_use test-session-3 2>&1 | grep -E "(DEBUG|INFO.*tool|ERROR)" || true