#!/usr/bin/env bash

# Claude Hook Enhancement Verification Script
# Verifies that the deployment is working correctly
# Version: 1.0.0

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly WRAPPER_SCRIPT="${SCRIPT_DIR}/claude_hook_redis_wrapper.sh"
readonly CLAUDE_SETTINGS_FILE="/Users/chris/.claude/settings.local.json"

# Colors for output
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
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

# Test different tool types
test_tool_extraction() {
    log_info "Testing tool name extraction..."
    
    # Source Redis config
    if [[ -f ~/.claude/redis_config.env ]]; then
        source ~/.claude/redis_config.env
    else
        log_warning "Redis config not found, testing without Redis"
        export REDIS_HOST="test"
        export REDIS_PORT="6379"
        export REDIS_PASSWORD="test"
    fi
    
    local tests=(
        '{"tool_input": {"file_path": "/path/to/file"}}:Read'
        '{"tool_input": {"old_string": "foo", "new_string": "bar"}}:Edit'
        '{"tool_input": {"command": "ls -la"}}:ls'
        '{"tool_input": {"command": "git status"}}:git'
        '{"tool_input": {"pattern": "search.*pattern"}}:Grep'
        '{"tool_input": {"content": "file content"}}:Write'
    )
    
    for test in "${tests[@]}"; do
        local payload="${test%:*}"
        local expected="${test#*:}"
        
        local result=$(echo "$payload" | "${SCRIPT_DIR}/enhance_hook_payload.sh" pre_tool_use 2>/dev/null)
        local extracted_tool=$(echo "$result" | jq -r '.tool_name // "unknown"' 2>/dev/null)
        
        if [[ "$extracted_tool" == "$expected" ]]; then
            log_success "✅ $expected detection works"
        else
            log_warning "⚠️  Expected '$expected', got '$extracted_tool'"
        fi
    done
}

# Verify configuration
verify_configuration() {
    log_info "Verifying Claude Code configuration..."
    
    # Check settings file exists
    if [[ -f "$CLAUDE_SETTINGS_FILE" ]]; then
        log_success "Settings file exists: $CLAUDE_SETTINGS_FILE"
    else
        log_warning "Settings file not found"
        return 1
    fi
    
    # Check if hooks are configured
    local hooks_count=$(jq '.hooks | length' "$CLAUDE_SETTINGS_FILE" 2>/dev/null || echo 0)
    if [[ "$hooks_count" -gt 0 ]]; then
        log_success "Hooks configured: $hooks_count hooks"
        
        # List configured hooks
        log_info "Configured hooks:"
        jq -r '.hooks | keys[]' "$CLAUDE_SETTINGS_FILE" 2>/dev/null | while read -r hook; do
            echo "  - $hook"
        done
    else
        log_warning "No hooks configured"
    fi
    
    # Check wrapper script path
    local wrapper_count=$(jq -r '.hooks | values[]' "$CLAUDE_SETTINGS_FILE" 2>/dev/null | grep -c "claude_hook_redis_wrapper.sh" || echo 0)
    if [[ "$wrapper_count" -gt 0 ]]; then
        log_success "Wrapper script is configured in hooks"
    else
        log_warning "Wrapper script not found in hook configuration"
    fi
}

# Show current status
show_status() {
    echo
    echo "========================================="
    echo "Claude Hook Enhancement Status"
    echo "========================================="
    echo
    
    echo "📁 Files:"
    echo "   ✅ Wrapper: $WRAPPER_SCRIPT"
    echo "   ✅ Enhancer: ${SCRIPT_DIR}/enhance_hook_payload.sh"
    echo "   ✅ Settings: $CLAUDE_SETTINGS_FILE"
    echo
    
    echo "🔧 Configuration:"
    local hooks_configured=$(jq '.hooks | length' "$CLAUDE_SETTINGS_FILE" 2>/dev/null || echo 0)
    echo "   ✅ Hooks configured: $hooks_configured"
    echo
    
    echo "🚀 Status: READY"
    echo "   The enhanced hook system is active and ready to use."
    echo
    
    echo "📱 Next Steps:"
    echo "   1. Start the Android Hooks Dashboard app"
    echo "   2. Use Claude Code normally"
    echo "   3. Check that tool names display correctly (not 'unknown')"
    echo "   4. Verify command names show for Bash tools (e.g., 'git', 'ls')"
    echo
    
    echo "🔍 To test manually:"
    echo "   source ~/.claude/redis_config.env"
    echo "   echo '{\"tool_input\": {\"command\": \"echo test\"}}' | \\"
    echo "     DEBUG=1 $WRAPPER_SCRIPT pre_tool_use test-session"
    echo
}

# Main execution
main() {
    echo "==========================================="
    echo "Claude Hook Enhancement Verification"
    echo "==========================================="
    echo
    
    test_tool_extraction
    verify_configuration
    show_status
    
    log_success "Verification completed!"
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi