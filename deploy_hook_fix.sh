#!/usr/bin/env bash

# Claude Hook Enhancement Deployment Script
# Configures Claude Code to use the enhanced hook wrapper
# Version: 1.0.0

set -euo pipefail

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly WRAPPER_SCRIPT="${SCRIPT_DIR}/claude_hook_redis_wrapper.sh"
readonly CLAUDE_SETTINGS_FILE="/Users/chris/.claude/settings.local.json"
readonly BACKUP_SUFFIX=".backup.$(date +%Y%m%d-%H%M%S)"

# Colors for output
readonly RED='\033[0;31m'
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

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if wrapper script exists and is executable
    if [[ ! -x "$WRAPPER_SCRIPT" ]]; then
        log_error "Wrapper script not found or not executable: $WRAPPER_SCRIPT"
        exit 1
    fi
    
    # Check if jq is available
    if ! command -v jq >/dev/null 2>&1; then
        log_warning "jq not found. Installing with brew..."
        if command -v brew >/dev/null 2>&1; then
            brew install jq
        else
            log_error "jq is required but not installed. Please install jq manually."
            exit 1
        fi
    fi
    
    # Check if Claude settings directory exists
    local claude_dir="$(dirname "$CLAUDE_SETTINGS_FILE")"
    if [[ ! -d "$claude_dir" ]]; then
        log_info "Creating Claude settings directory: $claude_dir"
        mkdir -p "$claude_dir"
    fi
    
    log_success "Prerequisites check completed"
}

# Backup existing settings
backup_settings() {
    if [[ -f "$CLAUDE_SETTINGS_FILE" ]]; then
        local backup_file="${CLAUDE_SETTINGS_FILE}${BACKUP_SUFFIX}"
        log_info "Backing up existing settings to: $backup_file"
        cp "$CLAUDE_SETTINGS_FILE" "$backup_file"
        log_success "Settings backed up"
    else
        log_info "No existing settings file found, creating new one"
    fi
}

# Generate hook configuration
generate_hook_config() {
    log_info "Generating hook configuration..."
    
    # Hook commands using the wrapper
    local hook_cmd="${WRAPPER_SCRIPT}"
    
    # Create configuration directly
    cat > /tmp/claude_settings.json << EOF
{
  "\$schema": "https://json.schemastore.org/claude-code-settings.json",
  "permissions": {
    "allow": [
      "Bash(chmod:*)",
      "Bash(launchctl load:*)",
      "Bash(brew install:*)",
      "Bash(launchctl unload:*)",
      "Bash(echo:*)",
      "Bash(find:*)",
      "Bash(ls:*)",
      "Bash(crontab:*)",
      "Bash(sudo crontab:*)",
      "Bash(launchctl:*)",
      "Bash(mkdir:*)",
      "Bash(open /Users/chris/.logs/bluetooth/wf1000xm4_connection.md)",
      "Bash(rm:*)",
      "Bash(ioreg:*)",
      "Bash(blueutil:*)"
    ],
    "deny": []
  },
  "hooks": {
    "pre_tool_use": "${hook_cmd} pre_tool_use claude-session",
    "post_tool_use": "${hook_cmd} post_tool_use claude-session",
    "user_prompt_submit": "${hook_cmd} user_prompt_submit claude-session",
    "notification": "${hook_cmd} notification claude-session",
    "session_start": "${hook_cmd} session_start claude-session",
    "stop_hook": "${hook_cmd} stop_hook claude-session"
  }
}
EOF
    
    cat /tmp/claude_settings.json
}

# Deploy configuration
deploy_configuration() {
    log_info "Deploying hook configuration..."
    
    generate_hook_config > "$CLAUDE_SETTINGS_FILE"
    
    log_success "Configuration deployed to: $CLAUDE_SETTINGS_FILE"
}

# Test the deployment
test_deployment() {
    log_info "Testing deployment..."
    
    # Test wrapper script directly
    local test_payload='{"tool_input": {"command": "echo test"}}'
    local result
    
    if result=$(echo "$test_payload" | "$WRAPPER_SCRIPT" pre_tool_use test-session 2>/dev/null); then
        log_success "Wrapper script test passed"
    else
        log_error "Wrapper script test failed"
        return 1
    fi
    
    # Validate settings file
    if jq empty "$CLAUDE_SETTINGS_FILE" 2>/dev/null; then
        log_success "Settings file is valid JSON"
    else
        log_error "Settings file contains invalid JSON"
        return 1
    fi
    
    # Check if hooks are configured
    local hooks_count=$(jq '.hooks | length' "$CLAUDE_SETTINGS_FILE" 2>/dev/null || echo 0)
    if [[ "$hooks_count" -gt 0 ]]; then
        log_success "Hooks configuration found ($hooks_count hooks)"
    else
        log_warning "No hooks configuration found"
    fi
    
    log_success "Deployment test completed"
}

# Show status
show_status() {
    echo
    echo "========================================="
    echo "Claude Hook Enhancement Deployment Status"
    echo "========================================="
    echo
    echo "✅ Enhanced hook scripts:"
    echo "   - Wrapper: $WRAPPER_SCRIPT"
    echo "   - Enhancer: ${SCRIPT_DIR}/enhance_hook_payload.sh"
    echo
    echo "✅ Claude settings:"
    echo "   - File: $CLAUDE_SETTINGS_FILE"
    echo "   - Hooks configured: $(jq '.hooks | length' "$CLAUDE_SETTINGS_FILE" 2>/dev/null || echo 0)"
    echo
    echo "🔧 Next steps:"
    echo "   1. Restart any running Claude Code sessions"
    echo "   2. Start the Android Hooks Dashboard app"
    echo "   3. Use Claude Code normally - tool names should now display correctly"
    echo
    echo "📋 To verify:"
    echo "   - Check Android app for proper tool names (not 'unknown')"
    echo "   - Look for actual command names in Bash tools"
    echo "   - User prompts should display correctly"
    echo
    echo "🔄 To rollback:"
    echo "   - Restore from backup: ${CLAUDE_SETTINGS_FILE}${BACKUP_SUFFIX}"
    echo
}

# Main execution
main() {
    echo "==========================================="
    echo "Claude Hook Enhancement Deployment"
    echo "==========================================="
    echo
    
    check_prerequisites
    backup_settings
    deploy_configuration
    test_deployment
    show_status
    
    log_success "Deployment completed successfully!"
    echo
    echo "🚀 The enhanced Claude Code hook system is now active!"
    echo "   Tool names and user prompts will be properly extracted and displayed."
}

# Run if executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi