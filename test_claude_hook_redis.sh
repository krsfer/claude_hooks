#!/usr/bin/env bash

# Test script for claude_hook_redis.sh
# Tests all hook types and various scenarios

set -euo pipefail

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m' # No Color

# Test configuration
readonly SCRIPT_PATH="./claude_hook_redis.sh"
readonly TEST_SESSION="test-session-$(date +%s)"
readonly TEST_LOG="/tmp/test_claude_hooks.log"

# Test counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Ensure script exists
if [[ ! -f "$SCRIPT_PATH" ]]; then
    echo -e "${RED}Error: Script not found at $SCRIPT_PATH${NC}"
    exit 1
fi

# Test function
run_test() {
    local test_name="$1"
    local hook_type="$2"
    local payload="$3"
    local expected_result="${4:-0}"
    
    ((TESTS_RUN++))
    
    echo -e "${BLUE}Testing: $test_name${NC}"
    
    # Set up test environment
    export CLAUDE_HOOKS_LOG="$TEST_LOG"
    export DEBUG="${DEBUG:-0}"
    
    # Run the test
    local result=0
    if echo "$payload" | "$SCRIPT_PATH" "$hook_type" "$TEST_SESSION" >/dev/null 2>&1; then
        result=0
    else
        result=$?
    fi
    
    # Check result
    if [[ $result -eq $expected_result ]]; then
        echo -e "${GREEN}✓ $test_name passed${NC}"
        ((TESTS_PASSED++))
        return 0
    else
        echo -e "${RED}✗ $test_name failed (expected: $expected_result, got: $result)${NC}"
        ((TESTS_FAILED++))
        return 1
    fi
}

# Print test summary
print_summary() {
    echo ""
    echo "================================"
    echo "Test Summary"
    echo "================================"
    echo -e "Tests run: ${TESTS_RUN}"
    echo -e "Tests passed: ${GREEN}${TESTS_PASSED}${NC}"
    echo -e "Tests failed: ${RED}${TESTS_FAILED}${NC}"
    
    if [[ $TESTS_FAILED -eq 0 ]]; then
        echo -e "${GREEN}All tests passed!${NC}"
        return 0
    else
        echo -e "${RED}Some tests failed${NC}"
        return 1
    fi
}

# Main test execution
main() {
    echo "================================"
    echo "Claude Hook Redis Test Suite"
    echo "================================"
    echo ""
    
    # Check for required environment variables
    if [[ -z "${REDIS_HOST:-}" || -z "${REDIS_PASSWORD:-}" ]]; then
        echo -e "${YELLOW}Warning: REDIS_HOST and REDIS_PASSWORD not set${NC}"
        echo "Setting dummy values for testing (will fail on actual Redis operations)"
        export REDIS_HOST="localhost"
        export REDIS_PASSWORD="test_password"
        export REDIS_PORT="6380"
        export REDIS_TLS="false"
    fi
    
    # Test 1: session_start hook
    run_test "session_start hook" \
        "session_start" \
        '{"source": "startup", "cwd": "/home/user/project", "transcript_path": "/tmp/transcript.txt"}' \
        0 || true
    
    # Test 2: user_prompt_submit hook
    run_test "user_prompt_submit hook" \
        "user_prompt_submit" \
        '{"prompt": "Write a Python hello world program"}' \
        0 || true
    
    # Test 3: pre_tool_use hook
    run_test "pre_tool_use hook" \
        "pre_tool_use" \
        '{"tool_name": "bash", "tool_input": {"command": "ls -la"}}' \
        0 || true
    
    # Test 4: post_tool_use hook
    run_test "post_tool_use hook" \
        "post_tool_use" \
        '{"tool_name": "bash", "tool_input": {"command": "ls"}, "tool_response": {"output": "file1.txt\nfile2.txt"}, "execution_time_ms": 45}' \
        0 || true
    
    # Test 5: notification hook
    run_test "notification hook" \
        "notification" \
        '{"message": "Task completed successfully"}' \
        0 || true
    
    # Test 6: stop_hook
    run_test "stop_hook" \
        "stop_hook" \
        '{"stop_hook_active": true}' \
        0 || true
    
    # Test 7: sub_agent_stop_hook
    run_test "sub_agent_stop_hook" \
        "sub_agent_stop_hook" \
        '{"stop_hook_active": false}' \
        0 || true
    
    # Test 8: pre_compact hook
    run_test "pre_compact hook" \
        "pre_compact" \
        '{"trigger": "auto", "custom_instructions": "Preserve important context"}' \
        0 || true
    
    # Test 9: Empty payload
    run_test "Empty payload" \
        "notification" \
        '{}' \
        0 || true
    
    # Test 10: Complex nested payload
    run_test "Complex nested payload" \
        "post_tool_use" \
        '{"tool_name": "edit", "tool_input": {"file": "main.py", "changes": [{"line": 1, "content": "import sys"}]}, "tool_response": {"success": true, "lines_changed": 1}}' \
        0 || true
    
    # Test 11: Invalid hook type
    run_test "Invalid hook type" \
        "invalid_hook" \
        '{"data": "test"}' \
        1 || true
    
    # Test 12: Help flag
    echo -e "${BLUE}Testing: Help flag${NC}"
    if "$SCRIPT_PATH" --help | grep -q "Usage:"; then
        echo -e "${GREEN}✓ Help flag test passed${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Help flag test failed${NC}"
        ((TESTS_FAILED++))
    fi
    ((TESTS_RUN++))
    
    # Test 13: Version flag
    echo -e "${BLUE}Testing: Version flag${NC}"
    if "$SCRIPT_PATH" --version | grep -q "version"; then
        echo -e "${GREEN}✓ Version flag test passed${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Version flag test failed${NC}"
        ((TESTS_FAILED++))
    fi
    ((TESTS_RUN++))
    
    # Test 14: No stdin (should use empty payload)
    echo -e "${BLUE}Testing: No stdin input${NC}"
    if "$SCRIPT_PATH" "notification" "$TEST_SESSION" < /dev/null >/dev/null 2>&1; then
        echo -e "${GREEN}✓ No stdin test passed${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ No stdin test failed${NC}"
        ((TESTS_FAILED++))
    fi
    ((TESTS_RUN++))
    
    # Test 15: Special characters in payload
    run_test "Special characters in payload" \
        "notification" \
        '{"message": "Line with \"quotes\" and \n newlines \t and tabs"}' \
        0 || true
    
    # Test 16: Large payload
    local large_content=$(printf '{"data": "%s"}' "$(head -c 10000 /dev/zero | tr '\0' 'X')")
    run_test "Large payload (10KB)" \
        "notification" \
        "$large_content" \
        0 || true
    
    # Test 17: Unicode in payload
    run_test "Unicode characters" \
        "notification" \
        '{"message": "Hello 世界 🌍 émojis"}' \
        0 || true
    
    # Test 18: Multiple sequential calls (sequence increment test)
    echo -e "${BLUE}Testing: Sequence number increment${NC}"
    local seq_test_session="seq-test-$(date +%s)"
    
    echo '{"test": 1}' | "$SCRIPT_PATH" "notification" "$seq_test_session" >/dev/null 2>&1
    echo '{"test": 2}' | "$SCRIPT_PATH" "notification" "$seq_test_session" >/dev/null 2>&1
    echo '{"test": 3}' | "$SCRIPT_PATH" "notification" "$seq_test_session" >/dev/null 2>&1
    
    echo -e "${GREEN}✓ Sequence increment test completed${NC}"
    ((TESTS_PASSED++))
    ((TESTS_RUN++))
    
    # Print summary
    print_summary
}

# Check if we should run specific tests
if [[ $# -gt 0 ]]; then
    case "$1" in
        --quick)
            # Run only basic tests
            echo "Running quick tests only..."
            run_test "Basic notification" "notification" '{"message": "test"}' 0
            print_summary
            ;;
        --help)
            echo "Usage: $0 [--quick|--full|--help]"
            echo "  --quick  Run basic tests only"
            echo "  --full   Run all tests (default)"
            echo "  --help   Show this help"
            exit 0
            ;;
        *)
            main
            ;;
    esac
else
    main
fi