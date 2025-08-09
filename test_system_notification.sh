#!/usr/bin/env bash

# Test script to send a system notification through the hook system

set -euo pipefail

# Source the hook script environment
if [[ -f ~/.claude/redis_config.env ]]; then
    source ~/.claude/redis_config.env
fi

# Create system notification JSON payload
cat <<'EOF' | ./claude_hook_redis.sh notification test-session-123
{
    "title": "System Notification Test",
    "message": "This is a test of the system notification detection and toast display functionality.",
    "severity": "info",
    "source": "system"
}
EOF

echo "System notification test sent!"
echo "Check your Android device for:"
echo "1. Toast message saying 'System: System Notification Test'"
echo "2. Android notification in the 'claudehook' channel with 🔔 icon"
echo "3. Green colored notification"