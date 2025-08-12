#!/usr/bin/env bash

# Test script for Enhanced Toast Messages
# Demonstrates different types of enriched toast content

set -euo pipefail

# Source Redis configuration
if [[ -f ~/.claude/redis_config.env ]]; then
    source ~/.claude/redis_config.env
else
    echo "Error: Redis configuration not found at ~/.claude/redis_config.env"
    exit 1
fi

# Configuration
readonly SCRIPT_NAME="$(basename "$0")"
readonly HOOK_SCRIPT="./claude_hook_redis.sh"
readonly SESSION_ID="toast-test-$(date +%s)"

# Color codes
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m'

echo -e "${BLUE}🍞 Enhanced Toast Testing${NC}"
echo "Session ID: $SESSION_ID"
echo ""
echo "This script creates different types of events to test the enhanced toast messages:"
echo "• MCP events with server/tool/response time details"
echo "• Regular events with session/sequence/tool information"
echo "• Error events with error details"
echo "• System events with various metadata"
echo ""

# Function to simulate various event types
simulate_event() {
    local hook_type="$1"
    local title="$2"
    local message="$3"
    local metadata="$4"
    
    echo -e "${YELLOW}→ Creating:${NC} $title"
    
    # Create the JSON payload
    local payload=$(cat <<EOF
{
    "hook_type": "$hook_type",
    "session_id": "$SESSION_ID",
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
    "title": "$title",
    "message": "$message",
    "source": "Toast Test",
    "severity": "info"$([ -n "$metadata" ] && echo ",$metadata" || echo "")
}
EOF
)
    
    # Send to Redis via hook script
    echo "$payload" | "$HOOK_SCRIPT" "$hook_type" "$SESSION_ID" 2>/dev/null
    echo "  ✓ Sent"
    sleep 0.8
}

echo -e "${GREEN}Starting Enhanced Toast Test Sequence...${NC}"
echo ""

# Test 1: MCP Events with rich metadata
echo -e "${BLUE}Test 1: MCP Events (Rich Server/Tool Info)${NC}"
simulate_event "pre_tool_use" "Context7 Library Lookup" "Resolving React documentation" \
    '"mcp_server": "context7", "mcp_tool": "resolve-library-id", "is_mcp_tool": true, "response_time": "234", "session_id": "sess-12345"'

simulate_event "pre_tool_use" "Serena Symbol Search" "Finding component definitions" \
    '"mcp_server": "serena", "mcp_tool": "find_symbol", "is_mcp_tool": true, "response_time": "89", "session_id": "sess-12345"'

simulate_event "pre_tool_use" "Sequential Thinking Error" "Analysis failed due to timeout" \
    '"mcp_server": "sequential-thinking", "mcp_tool": "sequentialthinking", "is_mcp_tool": true, "response_time": "5000", "error": "Connection timeout after 5s", "session_id": "sess-12345"'

echo ""

# Test 2: Regular Tool Events
echo -e "${BLUE}Test 2: Regular Tool Events (Session/Sequence Info)${NC}"
simulate_event "pre_tool_use" "File Read Operation" "Reading configuration file" \
    '"tool_name": "Read", "session_id": "sess-67890", "sequence": "42"'

simulate_event "post_tool_use" "Bash Command Execution" "npm install completed" \
    '"tool_name": "Bash", "session_id": "sess-67890", "sequence": "43", "command": "npm install --save react"'

simulate_event "pre_tool_use" "WebSearch Query" "Searching for documentation" \
    '"tool_name": "WebSearch", "session_id": "sess-67890", "sequence": "44", "query": "React hooks best practices"'

echo ""

# Test 3: Error Events
echo -e "${BLUE}Test 3: Error Events (Error Details)${NC}"
simulate_event "notification" "Build Failed" "TypeScript compilation errors detected" \
    '"severity": "error", "error": "Type '\''string'\'' is not assignable to type '\''number'\''", "tool_name": "TypeScript", "session_id": "sess-11111"'

simulate_event "notification" "Network Connection Lost" "Redis connection interrupted" \
    '"severity": "warning", "error": "Connection refused on port 6379", "source": "Redis Client", "session_id": "sess-11111"'

echo ""

# Test 4: System Events
echo -e "${BLUE}Test 4: System Events (Various Metadata)${NC}"
simulate_event "session_start" "Claude Session Started" "New development session initiated" \
    '"session_id": "sess-99999", "sequence": "1"'

simulate_event "notification" "Memory Compaction" "Event history compressed" \
    '"compact_reason": "Memory limit exceeded", "session_id": "sess-99999", "sequence": "15"'

simulate_event "notification" "Performance Alert" "High CPU usage detected" \
    '"severity": "warning", "source": "System Monitor", "session_id": "sess-99999"'

echo ""

# Test 5: Events with minimal metadata (fallback test)
echo -e "${BLUE}Test 5: Minimal Events (Fallback Behavior)${NC}"
simulate_event "notification" "Simple Notification" "Basic event with minimal data" ""

simulate_event "notification" "Another Simple Event" "Testing fallback message" \
    '"severity": "info"'

echo ""

echo -e "${GREEN}✅ Enhanced Toast Test Complete!${NC}"
echo ""
echo "Test Summary:"
echo "- Created 12 different event types"
echo "- Tested MCP events with server/tool/response time details"
echo "- Tested regular events with session/sequence/tool information"
echo "- Tested error events with error details"
echo "- Tested system events with various metadata"
echo "- Tested minimal events (fallback behavior)"
echo ""
echo -e "${CYAN}Now tap on event cards in the app to see enriched toast messages!${NC}"
echo ""
echo "Expected enhanced toast examples:"
echo "• MCP events: 🔌 MCP Server: serena • 🛠️ Tool: find_symbol • ⏱️ Response: 89ms • 🔗 Session: sess-123... • 🕐 2s ago"
echo "• Regular events: 🛠️ Tool: Bash • 🔗 Session: sess-678... • 🔢 Sequence: #43 • 🕐 5s ago"  
echo "• Error events: ❌ Error: Connection timeout • 🔗 Session: sess-111... • 🕐 8s ago • ⚠️ ERROR"
echo "• Minimal events: Tap and hold for sharing options"
echo ""
echo "Session ID: $SESSION_ID"