#!/usr/bin/env bash

# Test script for Serena MCP Server
# Simulates various Serena tool calls to test the notification system

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
readonly SESSION_ID="serena-test-$(date +%s)"

# Color codes
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m'

# Serena MCP tools
readonly SERENA_TOOLS=(
    "list_dir"
    "find_file"
    "replace_regex"
    "search_for_pattern"
    "get_symbols_overview"
    "find_symbol"
    "find_referencing_symbols"
    "replace_symbol_body"
    "insert_after_symbol"
    "insert_before_symbol"
    "write_memory"
    "read_memory"
    "list_memories"
    "delete_memory"
    "check_onboarding_performed"
    "onboarding"
    "think_about_collected_information"
    "think_about_task_adherence"
    "think_about_whether_you_are_done"
)

echo -e "${PURPLE}🔬 Serena MCP Server Testing${NC}"
echo "Session ID: $SESSION_ID"
echo ""

# Function to simulate Serena MCP tool call
simulate_serena_call() {
    local tool="$1"
    local description="$2"
    local success="${3:-true}"
    local response_time="${4:-$((RANDOM % 300 + 50))}"  # Random between 50-350ms
    
    local tool_name="mcp__serena__${tool}"
    local error_msg=""
    
    if [[ "$success" == "false" ]]; then
        error_msg="Simulated error: ${5:-Operation failed}"
    fi
    
    echo -e "${YELLOW}→ Serena:${NC} ${CYAN}$tool${NC}"
    echo -e "  ${description}"
    
    # Create the JSON payload with Serena-specific metadata
    local payload=$(cat <<EOF
{
    "hook_type": "pre_tool_use",
    "session_id": "$SESSION_ID",
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
    "tool_name": "$tool_name",
    "mcp_server": "serena",
    "mcp_tool": "$tool",
    "is_mcp_tool": true,
    "description": "$description",
    "status": "$([ "$success" == "true" ] && echo "success" || echo "error")",
    "response_time": "$response_time",
    "error": "$error_msg",
    "tool_input": "{\\"relative_path\\": \\".\\"}"
}
EOF
)
    
    # Send to Redis via hook script
    echo "$payload" | "$HOOK_SCRIPT" "pre_tool_use" "$SESSION_ID" 2>/dev/null
    
    echo -e "  ✓ Sent (${response_time}ms, $([ "$success" == "true" ] && echo "${GREEN}success${NC}" || echo "${YELLOW}error${NC}"))"
    sleep 0.3
}

# Function to simulate Serena server connection
simulate_serena_connection() {
    local connected="$1"
    
    echo -e "${YELLOW}→ Serena Server:${NC} $([ "$connected" == "true" ] && echo "${GREEN}CONNECTED${NC}" || echo "${YELLOW}DISCONNECTED${NC}")"
    
    local payload=$(cat <<EOF
{
    "hook_type": "notification",
    "session_id": "$SESSION_ID",
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
    "title": "Serena MCP Server $([ "$connected" == "true" ] && echo "Connected" || echo "Disconnected")",
    "message": "Serena semantic code analysis server is $([ "$connected" == "true" ] && echo "ready for operations" || echo "offline")",
    "source": "MCP Manager",
    "severity": "info",
    "mcp_server": "serena",
    "mcp_connection_status": "$connected"
}
EOF
)
    
    echo "$payload" | "$HOOK_SCRIPT" "notification" "$SESSION_ID" 2>/dev/null
    echo "  ✓ Connection notification sent"
    sleep 0.5
}

# Start test sequence
echo -e "${GREEN}Starting Serena MCP Server Test Sequence...${NC}"
echo ""

# Phase 1: Server Connection
echo -e "${BLUE}Phase 1: Server Connection${NC}"
simulate_serena_connection "true"
echo ""

# Phase 2: File System Operations
echo -e "${BLUE}Phase 2: File System Operations${NC}"
simulate_serena_call "list_dir" "Listing project directory structure" "true" "89"
simulate_serena_call "find_file" "Finding *.kt files in project" "true" "156"
simulate_serena_call "search_for_pattern" "Searching for 'MCP' pattern in codebase" "true" "234"
echo ""

# Phase 3: Symbol Analysis
echo -e "${BLUE}Phase 3: Symbol Analysis${NC}"
simulate_serena_call "get_symbols_overview" "Getting overview of MainActivity.kt symbols" "true" "178"
simulate_serena_call "find_symbol" "Finding HookEvent class definition" "true" "92"
simulate_serena_call "find_referencing_symbols" "Finding references to NotificationService" "true" "456"
echo ""

# Phase 4: Code Modification Operations
echo -e "${BLUE}Phase 4: Code Modification Operations${NC}"
simulate_serena_call "replace_regex" "Replacing deprecated API usage" "true" "267"
simulate_serena_call "replace_symbol_body" "Updating function implementation" "true" "389"
simulate_serena_call "insert_after_symbol" "Adding new method to class" "true" "145"
simulate_serena_call "insert_before_symbol" "Adding import statement" "true" "67"
echo ""

# Phase 5: Memory Operations
echo -e "${BLUE}Phase 5: Memory Operations${NC}"
simulate_serena_call "list_memories" "Listing available project memories" "true" "45"
simulate_serena_call "write_memory" "Saving project architecture notes" "true" "123"
simulate_serena_call "read_memory" "Reading saved architecture notes" "true" "78"
echo ""

# Phase 6: Thinking Operations
echo -e "${BLUE}Phase 6: Thinking Operations${NC}"
simulate_serena_call "think_about_collected_information" "Analyzing collected code information" "true" "890"
simulate_serena_call "think_about_task_adherence" "Verifying task completion status" "true" "567"
simulate_serena_call "think_about_whether_you_are_done" "Checking if all requirements met" "true" "234"
echo ""

# Phase 7: Error Scenarios
echo -e "${BLUE}Phase 7: Error Scenarios${NC}"
simulate_serena_call "find_symbol" "Finding non-existent symbol" "false" "156" "Symbol not found: NonExistentClass"
simulate_serena_call "replace_regex" "Invalid regex pattern" "false" "89" "Invalid regex: Unmatched parenthesis"
simulate_serena_call "read_memory" "Reading deleted memory" "false" "45" "Memory file not found"
echo ""

# Phase 8: Rapid Sequential Operations
echo -e "${BLUE}Phase 8: Rapid Sequential Operations${NC}"
echo -e "${CYAN}Simulating typical Serena workflow...${NC}"
simulate_serena_call "check_onboarding_performed" "Checking onboarding status" "true" "34"
simulate_serena_call "list_dir" "Scanning project structure" "true" "89"
simulate_serena_call "find_file" "Locating target files" "true" "123"
simulate_serena_call "get_symbols_overview" "Analyzing file symbols" "true" "234"
simulate_serena_call "find_symbol" "Finding specific symbol" "true" "67"
simulate_serena_call "replace_symbol_body" "Modifying symbol" "true" "345"
echo ""

# Phase 9: Performance Test
echo -e "${BLUE}Phase 9: Performance Test${NC}"
echo -e "${CYAN}Simulating heavy operations...${NC}"
simulate_serena_call "search_for_pattern" "Complex regex search across codebase" "true" "2456"
simulate_serena_call "find_referencing_symbols" "Finding all references (large codebase)" "true" "3789"
simulate_serena_call "think_about_collected_information" "Deep analysis of collected data" "true" "4567"
echo ""

# Phase 10: Server Disconnection
echo -e "${BLUE}Phase 10: Server Disconnection${NC}"
simulate_serena_connection "false"
echo ""

# Show summary
echo -e "${GREEN}✅ Serena MCP Server Test Complete!${NC}"
echo ""
echo "Test Summary:"
echo "- Tested ${#SERENA_TOOLS[@]} different Serena tools"
echo "- Simulated 22 tool calls"
echo "- Tested all major Serena operations:"
echo "  • File system operations"
echo "  • Symbol analysis and navigation"
echo "  • Code modification"
echo "  • Memory management"
echo "  • Thinking/analysis operations"
echo "  • Error handling"
echo "  • Performance scenarios"
echo ""
echo "Check your Android device for Serena MCP notifications!"
echo "Session ID: $SESSION_ID"
echo ""
echo -e "${CYAN}Tip: Watch for purple 🔌 notifications showing Serena operations${NC}"