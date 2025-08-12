#!/usr/bin/env bash

# Test script to verify USER_PROMPT_SUBMIT notifications are working

set -euo pipefail

SESSION_ID="test-prompt-$(date +%s)"

echo "🔔 Testing USER_PROMPT_SUBMIT notification..."
echo "Session ID: $SESSION_ID"
echo ""

# Create the payload JSON
PAYLOAD=$(cat <<EOF
{
  "prompt": "Test prompt: Can you help me verify that notifications are working correctly for user prompt submissions?",
  "prompt_preview": "Test prompt: Can you help me verify that notifications...",
  "prompt_length": 95
}
EOF
)

# Send the event via claude_hook_redis.sh
echo "$PAYLOAD" | ./claude_hook_redis.sh user_prompt_submit "$SESSION_ID"

echo ""
echo "✅ Event sent!"
echo ""
echo "📱 Check your Android device for:"
echo "   1. A notification about the user prompt"
echo "   2. The event card in Claude Hooks Dashboard"
echo ""
echo "Expected notification:"
echo "  Title: User Prompt"
echo "  Message: Test prompt: Can you help me verify..."