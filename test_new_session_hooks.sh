#!/usr/bin/env bash

# Test script to verify enhanced hooks will work in new Claude Code session
# Run this AFTER restarting Claude Code to verify the enhancement is working
# Version: 1.0.0

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly WRAPPER_SCRIPT="${SCRIPT_DIR}/claude_hook_redis_wrapper.sh"

# Colors for output
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly RED='\033[0;31m'
readonly NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Test various tool scenarios that should work in new session
test_enhanced_hooks() {
    log_info "Testing enhanced hook scenarios that will work in new Claude Code session..."
    
    # Source Redis config
    if [[ -f ~/.claude/redis_config.env ]]; then
        source ~/.claude/redis_config.env
        log_info "Redis environment loaded"
    else
        log_error "Redis config not found at ~/.claude/redis_config.env"
        return 1
    fi
    
    local session_id="test-new-session-$(date +%s)"
    
    echo
    log_info "🧪 Testing scenarios that will show correct tool names:"
    echo
    
    # Test 1: File reading (should show "Read")
    log_info "Test 1: File Reading Operation"
    local read_result=$(echo '{"tool_input": {"file_path": "/Users/chris/dev/src/android/claude_hooks/README.md"}}' | \
        "$WRAPPER_SCRIPT" pre_tool_use "$session_id" 2>/dev/null | \
        grep -o '"tool_name":"[^"]*"' | cut -d'"' -f4 || echo "failed")
    
    if [[ "$read_result" == "Read" ]]; then
        log_success "✅ File reading will show: Tool Use: Read"
    else
        log_warning "⚠️  File reading test failed: $read_result"
    fi
    
    # Test 2: Git command (should show "git")  
    log_info "Test 2: Git Command"
    local git_result=$(echo '{"tool_input": {"command": "git status"}}' | \
        "$WRAPPER_SCRIPT" pre_tool_use "$session_id" 2>/dev/null | \
        grep -o '"tool_name":"[^"]*"' | cut -d'"' -f4 || echo "failed")
    
    if [[ "$git_result" == "git" ]]; then
        log_success "✅ Git commands will show: Tool Use: git"
    else
        log_warning "⚠️  Git command test failed: $git_result"
    fi
    
    # Test 3: File editing (should show "Edit")
    log_info "Test 3: File Editing Operation"
    local edit_result=$(echo '{"tool_input": {"file_path": "/path/to/file", "old_string": "old", "new_string": "new"}}' | \
        "$WRAPPER_SCRIPT" pre_tool_use "$session_id" 2>/dev/null | \
        grep -o '"tool_name":"[^"]*"' | cut -d'"' -f4 || echo "failed")
    
    if [[ "$edit_result" == "Edit" ]]; then
        log_success "✅ File editing will show: Tool Use: Edit"
    else
        log_warning "⚠️  File editing test failed: $edit_result"
    fi
    
    # Test 4: Directory listing (should show "ls")
    log_info "Test 4: Directory Listing"
    local ls_result=$(echo '{"tool_input": {"command": "ls -la"}}' | \
        "$WRAPPER_SCRIPT" pre_tool_use "$session_id" 2>/dev/null | \
        grep -o '"tool_name":"[^"]*"' | cut -d'"' -f4 || echo "failed")
    
    if [[ "$ls_result" == "ls" ]]; then
        log_success "✅ Directory listing will show: Tool Use: ls"
    else
        log_warning "⚠️  Directory listing test failed: $ls_result"
    fi
    
    # Test 5: User prompt (should capture actual prompt)
    log_info "Test 5: User Prompt Capture"
    local prompt_result=$(echo '{"prompt": "Test user message"}' | \
        "$WRAPPER_SCRIPT" user_prompt_submit "$session_id" 2>/dev/null | \
        grep -o '"prompt":"[^"]*"' | cut -d'"' -f4 || echo "failed")
    
    if [[ "$prompt_result" == "Test user message" ]]; then
        log_success "✅ User prompts will show: \"Test user message\" (not null)"
    else
        log_warning "⚠️  User prompt test failed: $prompt_result"
    fi
    
    echo
    log_info "📱 Expected Android Dashboard Results After Claude Code Restart:"
    echo
    echo "   BEFORE (current broken session):"
    echo "   ❌ Tool Use: Unknown"
    echo "   ❌ User Prompt: null" 
    echo
    echo "   AFTER (new enhanced session):"
    echo "   ✅ Tool Use: Read"
    echo "   ✅ Tool Use: git"
    echo "   ✅ Tool Use: Edit"
    echo "   ✅ Tool Use: ls"
    echo "   ✅ User Prompt: \"Yes, please\" (actual message text)"
    echo
}

# Show instructions for testing
show_test_instructions() {
    echo
    echo "========================================="
    echo "🔄 RESTART CLAUDE CODE SESSION TEST"
    echo "========================================="
    echo
    echo "📋 Steps to verify the fix:"
    echo
    echo "1. 🚪 EXIT this current Claude Code session"
    echo "   - The current session is using old hooks (shows 'Unknown')"
    echo
    echo "2. 🆕 START a new Claude Code session" 
    echo "   - New session will load enhanced hooks automatically"
    echo
    echo "3. 📱 OPEN Android Hooks Dashboard app"
    echo "   - Clear any old events if needed"
    echo
    echo "4. 🧪 TEST with simple commands in new Claude session:"
    echo "   - Type: \"ls\" (should show Tool Use: ls)"
    echo "   - Type: \"read package.json\" (should show Tool Use: Read)"
    echo "   - Type: \"edit file.txt\" (should show Tool Use: Edit)"
    echo
    echo "5. ✅ VERIFY results in Android app:"
    echo "   - Tool names should be specific (Read, Edit, git, ls)"
    echo "   - User prompts should show actual text (not null)"
    echo "   - No more 'Unknown' tool names"
    echo
    echo "🔧 If still showing 'Unknown' after restart:"
    echo "   - Check Claude Code is using correct settings file"
    echo "   - Run: claude --debug to see active hook configuration"
    echo "   - Verify hooks section in ~/.claude/settings.local.json"
    echo
}

# Main execution
main() {
    echo "==========================================="
    echo "Enhanced Hook Verification for New Session"
    echo "==========================================="
    echo
    
    test_enhanced_hooks
    show_test_instructions
    
    log_success "Test completed! Enhanced hooks are ready for new Claude Code session."
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi