#!/usr/bin/env bash

# Test script for pattern detection functionality

set -euo pipefail

# Source Redis configuration
if [[ -f ~/.claude/redis_config.env ]]; then
    source ~/.claude/redis_config.env
fi

echo "Testing pattern detection functionality..."

# Test 1: Session start hook
echo -e "\n🧪 Test 1: Session start hook"
cat <<'EOF' | DEBUG=1 ./claude_hook_redis.sh session_start test-session-pattern-001
{
    "status": "starting",
    "version": "claude-3.5-sonnet"
}
EOF

# Test 2: User prompt submit hook  
echo -e "\n🧪 Test 2: User prompt submit hook"
cat <<'EOF' | DEBUG=1 ./claude_hook_redis.sh user_prompt_submit test-session-pattern-002
{
    "prompt": "Hello Claude, can you help me with coding?",
    "timestamp": "2024-01-01T12:00:00Z"
}
EOF

# Test 3: User prompt with "User Prompt" text
echo -e "\n🧪 Test 3: Payload containing 'User Prompt' text"
cat <<'EOF' | DEBUG=1 ./claude_hook_redis.sh user_prompt_submit test-session-pattern-003
{
    "prompt": "Add notify when User Prompt is detected",
    "context": "This is a test of user prompt detection"
}
EOF

# Test 4: Session started pattern in content
echo -e "\n🧪 Test 4: Payload containing 'Session Started' text"
cat <<'EOF' | DEBUG=1 ./claude_hook_redis.sh user_prompt_submit test-session-pattern-004
{
    "prompt": "Notify also when Session Started is detected",
    "context": "This should trigger session started pattern detection"
}
EOF

# Test 5: Notification hook (should not trigger recursion)
echo -e "\n🧪 Test 5: Notification hook (recursion prevention)"
cat <<'EOF' | DEBUG=1 ./claude_hook_redis.sh notification test-session-pattern-005
{
    "title": "Test Notification",
    "message": "This contains User Prompt and Session Started but should not recurse",
    "severity": "info"
}
EOF

echo -e "\n✅ Pattern detection tests completed!"
echo "Check ~/.claude/logs/hooks.log for detailed pattern detection logs"
echo "Check your Android device for notifications if connected to Redis"