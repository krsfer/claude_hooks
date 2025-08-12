#!/usr/bin/env bash

# Test script for MCP Server Activity Reporting
# Simulates various MCP tool calls to test the notification system

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
readonly SESSION_ID="mcp-test-$(date +%s)"

# Color codes
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly NC='\033[0m'

# Test MCP servers
readonly MCP_SERVERS=("context7" "serena" "sequential-thinking" "magic" "puppeteer" "github" "memory")
readonly MCP_TOOLS=(
    "context7__resolve-library-id"
    "context7__get-library-docs"
    "serena__find_symbol"
    "serena__list_dir"
    "sequential-thinking__sequentialthinking"
    "magic__builder"
    "puppeteer__screenshot"
    "github__create_repository"
    "memory__store"
)

echo -e "${BLUE}🔧 MCP Server Activity Testing${NC}"
echo "Session ID: $SESSION_ID"
echo ""

# Function to simulate MCP tool call
simulate_mcp_tool_call() {
    local server="$1"
    local tool="$2"
    local success="${3:-true}"
    local response_time="${4:-$((RANDOM % 500 + 100))}"  # Random between 100-600ms
    
    local tool_name="mcp__${server}__${tool}"
    local error_msg=""
    
    if [[ "$success" == "false" ]]; then
        error_msg="Simulated error: Connection timeout"
    fi
    
    echo -e "${YELLOW}→ Simulating MCP tool call: ${PURPLE}$server${NC} / ${GREEN}$tool${NC}"
    
    # Create the JSON payload
    local payload=$(cat <<EOF
{
    "hook_type": "pre_tool_use",
    "session_id": "$SESSION_ID",
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
    "tool_name": "$tool_name",
    "mcp_server": "$server",
    "mcp_tool": "$tool",
    "is_mcp_tool": true,
    "description": "Testing MCP server activity: $server/$tool",
    "status": "$([ "$success" == "true" ] && echo "success" || echo "error")",
    "response_time": "$response_time",
    "error": "$error_msg"
}
EOF
)
    
    # Send to Redis via hook script
    echo "$payload" | "$HOOK_SCRIPT" "pre_tool_use" "$SESSION_ID" 2>/dev/null
    
    echo -e "  ✓ Sent (${response_time}ms, $([ "$success" == "true" ] && echo "success" || echo "error"))"
    sleep 0.5
}

# Function to simulate MCP server connection change
simulate_mcp_connection() {
    local server="$1"
    local connected="$2"
    
    echo -e "${YELLOW}→ Simulating MCP server connection: ${PURPLE}$server${NC} - $([ "$connected" == "true" ] && echo "CONNECTED" || echo "DISCONNECTED")"
    
    local payload=$(cat <<EOF
{
    "hook_type": "notification",
    "session_id": "$SESSION_ID",
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
    "title": "MCP Server $([ "$connected" == "true" ] && echo "Connected" || echo "Disconnected")",
    "message": "Server '$server' is now $([ "$connected" == "true" ] && echo "connected and ready" || echo "disconnected")",
    "source": "MCP Manager",
    "severity": "info",
    "mcp_server": "$server",
    "mcp_connection_status": "$connected"
}
EOF
)
    
    echo "$payload" | "$HOOK_SCRIPT" "notification" "$SESSION_ID" 2>/dev/null
    echo "  ✓ Sent connection notification"
    sleep 0.5
}

# Start test sequence
echo -e "${GREEN}Starting MCP Server Activity Test Sequence...${NC}"
echo ""

# 1. Simulate server connections
echo -e "${BLUE}Phase 1: Server Connections${NC}"
for server in "${MCP_SERVERS[@]:0:3}"; do
    simulate_mcp_connection "$server" "true"
done
echo ""

# 2. Simulate successful tool calls
echo -e "${BLUE}Phase 2: Successful Tool Calls${NC}"
simulate_mcp_tool_call "context7" "resolve-library-id" "true" "234"
simulate_mcp_tool_call "context7" "get-library-docs" "true" "567"
simulate_mcp_tool_call "serena" "find_symbol" "true" "123"
simulate_mcp_tool_call "serena" "list_dir" "true" "89"
simulate_mcp_tool_call "sequential-thinking" "sequentialthinking" "true" "1234"
echo ""

# 3. Simulate some errors
echo -e "${BLUE}Phase 3: Error Scenarios${NC}"
simulate_mcp_tool_call "magic" "builder" "false" "5000"
simulate_mcp_tool_call "puppeteer" "screenshot" "false" "3000"
echo ""

# 4. Simulate rapid succession of calls
echo -e "${BLUE}Phase 4: Rapid Succession${NC}"
for i in {1..5}; do
    simulate_mcp_tool_call "context7" "get-library-docs" "true" "$((RANDOM % 200 + 50))"
done
echo ""

# 5. Simulate server disconnection
echo -e "${BLUE}Phase 5: Server Disconnection${NC}"
simulate_mcp_connection "context7" "false"
echo ""

# 6. Show summary
echo -e "${GREEN}✅ MCP Server Activity Test Complete!${NC}"
echo ""
echo "Test Summary:"
echo "- Tested ${#MCP_SERVERS[@]} different MCP servers"
echo "- Simulated $(( 5 + 2 + 5 )) tool calls"
echo "- Tested connection/disconnection events"
echo "- Tested error scenarios"
echo ""
echo "Check your Android device for notifications!"
echo "Session ID: $SESSION_ID"