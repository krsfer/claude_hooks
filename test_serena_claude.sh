#!/usr/bin/env bash

# Test Serena MCP Server with actual Claude Code commands
# This script demonstrates how to trigger Serena MCP notifications through Claude

set -euo pipefail

# Color codes
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly PURPLE='\033[0;35m'
readonly CYAN='\033[0;36m'
readonly NC='\033[0m'

echo -e "${PURPLE}🔬 Serena MCP Testing with Claude Code${NC}"
echo ""
echo "This script will show you how to test Serena MCP server with Claude Code."
echo ""

echo -e "${BLUE}Available Serena Tools:${NC}"
echo ""
echo -e "${CYAN}File Operations:${NC}"
echo "  • mcp__serena__list_dir - List directory contents"
echo "  • mcp__serena__find_file - Find files by pattern"
echo "  • mcp__serena__search_for_pattern - Search for patterns in code"
echo ""

echo -e "${CYAN}Symbol Operations:${NC}"
echo "  • mcp__serena__get_symbols_overview - Get file symbols overview"
echo "  • mcp__serena__find_symbol - Find specific symbols"
echo "  • mcp__serena__find_referencing_symbols - Find symbol references"
echo ""

echo -e "${CYAN}Code Modification:${NC}"
echo "  • mcp__serena__replace_regex - Replace with regex"
echo "  • mcp__serena__replace_symbol_body - Replace symbol body"
echo "  • mcp__serena__insert_after_symbol - Insert after symbol"
echo "  • mcp__serena__insert_before_symbol - Insert before symbol"
echo ""

echo -e "${CYAN}Memory Operations:${NC}"
echo "  • mcp__serena__write_memory - Save project information"
echo "  • mcp__serena__read_memory - Read saved information"
echo "  • mcp__serena__list_memories - List all memories"
echo "  • mcp__serena__delete_memory - Delete a memory"
echo ""

echo -e "${CYAN}Analysis Operations:${NC}"
echo "  • mcp__serena__think_about_collected_information - Analyze information"
echo "  • mcp__serena__think_about_task_adherence - Check task progress"
echo "  • mcp__serena__think_about_whether_you_are_done - Verify completion"
echo ""

echo -e "${BLUE}Example Claude Commands to Test Serena:${NC}"
echo ""

echo -e "${YELLOW}1. Basic File Operations:${NC}"
echo '   claude "Use serena to list all files in the current directory"'
echo '   claude "Use serena to find all Kotlin files"'
echo '   claude "Use serena to search for MCP-related code"'
echo ""

echo -e "${YELLOW}2. Symbol Analysis:${NC}"
echo '   claude "Use serena to analyze the symbols in NotificationService.kt"'
echo '   claude "Use serena to find the McpServerManager class"'
echo '   claude "Use serena to find all references to showNotificationForHookEvent"'
echo ""

echo -e "${YELLOW}3. Code Understanding:${NC}"
echo '   claude "Use serena to understand the MCP notification implementation"'
echo '   claude "Use serena to analyze how hooks are processed"'
echo '   claude "Use serena to find and explain the Redis integration"'
echo ""

echo -e "${YELLOW}4. Memory Operations:${NC}"
echo '   claude "Use serena to save notes about the MCP architecture"'
echo '   claude "Use serena to list all saved project memories"'
echo '   claude "Use serena to read the architecture notes"'
echo ""

echo -e "${YELLOW}5. Complex Analysis:${NC}"
echo '   claude "Use serena to analyze the entire notification system architecture"'
echo '   claude "Use serena to find all MCP-related code and create a summary"'
echo '   claude "Use serena to check if the MCP implementation is complete"'
echo ""

echo -e "${BLUE}Testing with Direct MCP Commands:${NC}"
echo ""
echo "You can also use the --serena flag or mention serena directly:"
echo ""
echo '   claude --serena "Analyze the project structure"'
echo '   claude "Ask serena to find all hook types"'
echo '   claude "Have serena check the onboarding status"'
echo ""

echo -e "${GREEN}Monitor Notifications:${NC}"
echo ""
echo "When testing, watch for:"
echo "• Purple 🔌 notifications for MCP operations"
echo "• Server connection status (green ✅ or red ❌)"
echo "• Tool call statistics in notification titles"
echo "• Response times and success rates"
echo ""

echo -e "${CYAN}Tips for Testing:${NC}"
echo "1. Open the Hooks Dashboard app on your Android device"
echo "2. Filter by 'MCP' or 'serena' to see only Serena operations"
echo "3. Watch the toast messages for immediate feedback"
echo "4. Check notification statistics (calls, success count)"
echo "5. Monitor error notifications for failed operations"
echo ""

echo -e "${PURPLE}Ready to test!${NC}"
echo "Run any of the example commands above to see Serena MCP notifications."