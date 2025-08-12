#!/bin/bash

# Test script for USER_PROMPT_SUBMIT and SESSION_START notifications
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOK_SCRIPT="$SCRIPT_DIR/claude_hook_redis.sh"

echo "🔬 Testing USER_PROMPT_SUBMIT and SESSION_START notifications..."
echo "=================================================================="

# Test with a simple prompt
echo "📝 Test 1: Sending user prompt submission event..."
echo '{"prompt": "Test prompt: How do I implement a notification system?"}' | "$HOOK_SCRIPT" user_prompt_submit test-session-prompt

sleep 2

# Test with a longer prompt
echo "📝 Test 2: Sending longer user prompt submission event..."
echo '{"prompt": "This is a longer test prompt that should trigger a notification. I am asking about implementing a comprehensive notification system with multiple channels and priority levels. This should definitely show up as a system notification with the green color and vibration pattern."}' | "$HOOK_SCRIPT" user_prompt_submit test-session-prompt

sleep 2

# Test with special characters
echo "📝 Test 3: Sending prompt with special characters..."
echo '{"prompt": "What is 2+2? Can you explain with code examples?"}' | "$HOOK_SCRIPT" user_prompt_submit test-session-prompt

sleep 2

# Test session_start hook
echo "🚀 Test 4: Sending session start event..."
echo '{"session_id": "test-session-start", "timestamp": "2025-08-11T22:30:00Z"}' | "$HOOK_SCRIPT" session_start test-session-start

echo ""
echo "✅ Test complete! Check your Android device for notifications."
echo "You should see:"
echo "  - 4 notifications total"
echo "  - 3 green notifications (system notifications) for user prompts"
echo "  - 1 notification for session start"
echo "  - Each with vibration pattern"
echo "  - Toast messages saying 'System: User Prompt' and session info"
echo ""
echo "If notifications didn't appear, check:"
echo "  1. App is running"
echo "  2. Notifications are enabled for Claude Hooks Dashboard"
echo "  3. Redis connection is working"