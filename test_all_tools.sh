#!/usr/bin/env bash

# Comprehensive test script for all Claude Code tool types
# Tests that tool_name is properly extracted and displayed

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Generate unique session ID
SESSION_ID="test-tools-$(date +%s)"

echo -e "${BLUE}🔧 Claude Code Tools Test Suite${NC}"
echo -e "${BLUE}================================${NC}"
echo "Session ID: $SESSION_ID"
echo ""

# Counter for test results
TESTS_PASSED=0
TESTS_FAILED=0

# Function to send a tool event
send_tool_event() {
    local hook_type="$1"
    local tool_name="$2"
    local payload="$3"
    local test_name="$4"
    
    echo -e "${YELLOW}Testing: $test_name${NC}"
    
    # Add tool_name to the payload
    local full_payload=$(echo "$payload" | jq --arg tn "$tool_name" '. + {tool_name: $tn}')
    
    # Send the event
    if echo "$full_payload" | ./claude_hook_redis.sh "$hook_type" "$SESSION_ID" 2>/dev/null; then
        echo -e "${GREEN}✅ $test_name sent successfully${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ $test_name failed to send${NC}"
        ((TESTS_FAILED++))
    fi
    
    # Small delay between events
    sleep 0.5
    echo ""
}

# Test Bash tool
echo -e "${BLUE}1. Testing Bash Tool${NC}"
send_tool_event "pre_tool_use" "Bash" '{
    "command": "ls -la /tmp",
    "tool_input": {"command": "ls -la /tmp"}
}' "Bash Pre-Tool Use"

send_tool_event "post_tool_use" "Bash" '{
    "command": "ls -la /tmp",
    "tool_response": "file1.txt\nfile2.txt",
    "execution_time_ms": 45,
    "success": true
}' "Bash Post-Tool Use"

# Test Read tool
echo -e "${BLUE}2. Testing Read Tool${NC}"
send_tool_event "pre_tool_use" "Read" '{
    "file_path": "/Users/test/document.txt",
    "tool_input": {"file_path": "/Users/test/document.txt"}
}' "Read Pre-Tool Use"

send_tool_event "post_tool_use" "Read" '{
    "file_path": "/Users/test/document.txt",
    "tool_response": "File contents here...",
    "execution_time_ms": 12,
    "success": true
}' "Read Post-Tool Use"

# Test Write tool
echo -e "${BLUE}3. Testing Write Tool${NC}"
send_tool_event "pre_tool_use" "Write" '{
    "file_path": "/Users/test/newfile.txt",
    "content": "New file content",
    "tool_input": {"file_path": "/Users/test/newfile.txt", "content": "New file content"}
}' "Write Pre-Tool Use"

send_tool_event "post_tool_use" "Write" '{
    "file_path": "/Users/test/newfile.txt",
    "execution_time_ms": 23,
    "success": true
}' "Write Post-Tool Use"

# Test Edit tool
echo -e "${BLUE}4. Testing Edit Tool${NC}"
send_tool_event "pre_tool_use" "Edit" '{
    "file_path": "/Users/test/existing.txt",
    "old_string": "old text",
    "new_string": "new text",
    "tool_input": {"file_path": "/Users/test/existing.txt", "old_string": "old text", "new_string": "new text"}
}' "Edit Pre-Tool Use"

send_tool_event "post_tool_use" "Edit" '{
    "file_path": "/Users/test/existing.txt",
    "execution_time_ms": 18,
    "success": true
}' "Edit Post-Tool Use"

# Test MultiEdit tool
echo -e "${BLUE}5. Testing MultiEdit Tool${NC}"
send_tool_event "pre_tool_use" "MultiEdit" '{
    "file_path": "/Users/test/multi.txt",
    "edits": [{"old_string": "foo", "new_string": "bar"}],
    "tool_input": {"file_path": "/Users/test/multi.txt", "edits": [{"old_string": "foo", "new_string": "bar"}]}
}' "MultiEdit Pre-Tool Use"

send_tool_event "post_tool_use" "MultiEdit" '{
    "file_path": "/Users/test/multi.txt",
    "execution_time_ms": 35,
    "success": true
}' "MultiEdit Post-Tool Use"

# Test Grep tool
echo -e "${BLUE}6. Testing Grep Tool${NC}"
send_tool_event "pre_tool_use" "Grep" '{
    "pattern": "TODO|FIXME",
    "path": "/Users/test/src",
    "tool_input": {"pattern": "TODO|FIXME", "path": "/Users/test/src"}
}' "Grep Pre-Tool Use"

send_tool_event "post_tool_use" "Grep" '{
    "pattern": "TODO|FIXME",
    "tool_response": "5 matches found",
    "execution_time_ms": 89,
    "success": true
}' "Grep Post-Tool Use"

# Test Glob tool
echo -e "${BLUE}7. Testing Glob Tool${NC}"
send_tool_event "pre_tool_use" "Glob" '{
    "pattern": "**/*.kt",
    "path": "/Users/test/project",
    "tool_input": {"pattern": "**/*.kt", "path": "/Users/test/project"}
}' "Glob Pre-Tool Use"

send_tool_event "post_tool_use" "Glob" '{
    "pattern": "**/*.kt",
    "tool_response": "Found 23 Kotlin files",
    "execution_time_ms": 67,
    "success": true
}' "Glob Post-Tool Use"

# Test LS tool
echo -e "${BLUE}8. Testing LS Tool${NC}"
send_tool_event "pre_tool_use" "LS" '{
    "path": "/Users/test/directory",
    "tool_input": {"path": "/Users/test/directory"}
}' "LS Pre-Tool Use"

send_tool_event "post_tool_use" "LS" '{
    "path": "/Users/test/directory",
    "tool_response": "10 files, 3 directories",
    "execution_time_ms": 8,
    "success": true
}' "LS Post-Tool Use"

# Test WebFetch tool
echo -e "${BLUE}9. Testing WebFetch Tool${NC}"
send_tool_event "pre_tool_use" "WebFetch" '{
    "url": "https://api.example.com/data",
    "tool_input": {"url": "https://api.example.com/data", "prompt": "Extract JSON data"}
}' "WebFetch Pre-Tool Use"

send_tool_event "post_tool_use" "WebFetch" '{
    "url": "https://api.example.com/data",
    "tool_response": "JSON data retrieved",
    "execution_time_ms": 234,
    "success": true
}' "WebFetch Post-Tool Use"

# Test WebSearch tool
echo -e "${BLUE}10. Testing WebSearch Tool${NC}"
send_tool_event "pre_tool_use" "WebSearch" '{
    "query": "Claude Code documentation hooks",
    "tool_input": {"query": "Claude Code documentation hooks"}
}' "WebSearch Pre-Tool Use"

send_tool_event "post_tool_use" "WebSearch" '{
    "query": "Claude Code documentation hooks",
    "tool_response": "Found 5 relevant results",
    "execution_time_ms": 456,
    "success": true
}' "WebSearch Post-Tool Use"

# Test Task tool (for subagents)
echo -e "${BLUE}11. Testing Task Tool${NC}"
send_tool_event "pre_tool_use" "Task" '{
    "description": "Analyze code quality",
    "prompt": "Review the codebase for potential improvements",
    "subagent_type": "code-reviewer",
    "tool_input": {"description": "Analyze code quality", "prompt": "Review the codebase"}
}' "Task Pre-Tool Use"

send_tool_event "post_tool_use" "Task" '{
    "description": "Analyze code quality",
    "tool_response": "Analysis complete: 3 issues found",
    "execution_time_ms": 1234,
    "success": true
}' "Task Post-Tool Use"

# Test TodoWrite tool
echo -e "${BLUE}12. Testing TodoWrite Tool${NC}"
send_tool_event "pre_tool_use" "TodoWrite" '{
    "todos": [{"content": "Test task 1", "status": "pending", "id": "1"}],
    "tool_input": {"todos": [{"content": "Test task 1", "status": "pending", "id": "1"}]}
}' "TodoWrite Pre-Tool Use"

send_tool_event "post_tool_use" "TodoWrite" '{
    "todos": [{"content": "Test task 1", "status": "pending", "id": "1"}],
    "execution_time_ms": 15,
    "success": true
}' "TodoWrite Post-Tool Use"

# Test NotebookEdit tool
echo -e "${BLUE}13. Testing NotebookEdit Tool${NC}"
send_tool_event "pre_tool_use" "NotebookEdit" '{
    "notebook_path": "/Users/test/notebook.ipynb",
    "cell_id": "cell-123",
    "new_source": "print(\"Hello\")",
    "tool_input": {"notebook_path": "/Users/test/notebook.ipynb", "cell_id": "cell-123", "new_source": "print(\"Hello\")"}
}' "NotebookEdit Pre-Tool Use"

send_tool_event "post_tool_use" "NotebookEdit" '{
    "notebook_path": "/Users/test/notebook.ipynb",
    "execution_time_ms": 28,
    "success": true
}' "NotebookEdit Post-Tool Use"

# Test with tool_name as "unknown" (should trigger inference)
echo -e "${BLUE}14. Testing Tool Name Inference${NC}"
send_tool_event "pre_tool_use" "unknown" '{
    "command": "git status",
    "tool_input": {"command": "git status"}
}' "Unknown Tool with Command (should infer Bash)"

send_tool_event "pre_tool_use" "unknown" '{
    "file_path": "/test/file.txt",
    "content": "test content",
    "tool_input": {"file_path": "/test/file.txt", "content": "test content"}
}' "Unknown Tool with File+Content (should infer Write)"

# Summary
echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}================================${NC}"
echo -e "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
if [ $TESTS_FAILED -gt 0 ]; then
    echo -e "${RED}Tests Failed: $TESTS_FAILED${NC}"
else
    echo -e "${GREEN}Tests Failed: $TESTS_FAILED${NC}"
fi
echo ""
echo -e "${YELLOW}📱 Check your Android device for:${NC}"
echo "   1. Tool-specific notifications"
echo "   2. Event cards showing tool names in titles"
echo "   3. Tool icons and colors in the cards"
echo ""
echo -e "${YELLOW}📋 Check logs at ~/.claude/logs/hooks.log for:${NC}"
echo "   1. Tool name extraction logging"
echo "   2. Inference logic when tool_name is unknown"
echo "   3. Final tool names set in payloads"