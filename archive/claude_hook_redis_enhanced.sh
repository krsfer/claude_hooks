#!/usr/bin/env bash

# Enhanced Claude Code Hook Redis Publisher
# Properly extracts tool names, prompts, and tool inputs from Claude Code hooks
# Version: 2.0.0

set -euo pipefail

# Configuration
readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_VERSION="2.0.0"
readonly REDIS_CHANNEL="hooksdata"
readonly LOG_FILE="${CLAUDE_HOOKS_LOG:-/tmp/claude_hooks.log}"
readonly SEQUENCE_FILE="${CLAUDE_HOOKS_SEQ:-/tmp/.claude_hooks_seq}"

# Exit codes
readonly EXIT_SUCCESS=0
readonly EXIT_INVALID_ARGS=1
readonly EXIT_REDIS_ERROR=2
readonly EXIT_JSON_ERROR=3
readonly EXIT_ENV_ERROR=4

# Source the original script for core functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/claude_hook_redis.sh" 2>/dev/null || true

# Enhanced payload processing function
process_enhanced_payload() {
    local hook_type="$1"
    local raw_payload="$2"
    
    local enhanced_payload="{}"
    
    case "$hook_type" in
        "pre_tool_use"|"post_tool_use")
            # Extract tool name from environment or payload
            local tool_name="${CLAUDE_TOOL_NAME:-}"
            local tool_input="${CLAUDE_TOOL_INPUT:-}"
            
            # If not in environment, try to extract from payload
            if [[ -z "$tool_name" ]] && command -v jq >/dev/null 2>&1; then
                # Try various possible field names
                tool_name=$(echo "$raw_payload" | jq -r '.tool_name // .name // .tool // .command // .action // "unknown"' 2>/dev/null)
                
                # If still unknown, try to infer from tool_input
                if [[ "$tool_name" == "unknown" || "$tool_name" == "null" ]]; then
                    local tool_input_data=$(echo "$raw_payload" | jq -r '.tool_input // .input // .parameters // "{}"' 2>/dev/null)
                    
                    # Check for specific patterns in tool_input
                    if echo "$tool_input_data" | grep -q '"file_path"'; then
                        tool_name="Read"
                    elif echo "$tool_input_data" | grep -q '"old_string"'; then
                        tool_name="Edit"
                    elif echo "$tool_input_data" | grep -q '"content"'; then
                        tool_name="Write"
                    elif echo "$tool_input_data" | grep -q '"command"'; then
                        local cmd=$(echo "$tool_input_data" | jq -r '.command' 2>/dev/null)
                        tool_name="Bash"
                        # Extract the actual command name if available
                        if [[ -n "$cmd" && "$cmd" != "null" ]]; then
                            local cmd_name=$(echo "$cmd" | awk '{print $1}')
                            [[ -n "$cmd_name" ]] && tool_name="Bash:$cmd_name"
                        fi
                    elif echo "$tool_input_data" | grep -q '"pattern"'; then
                        tool_name="Grep"
                    elif echo "$tool_input_data" | grep -q '"path"'; then
                        tool_name="LS"
                    elif echo "$tool_input_data" | grep -q '"url"'; then
                        tool_name="WebFetch"
                    elif echo "$tool_input_data" | grep -q '"query"'; then
                        tool_name="WebSearch"
                    elif echo "$tool_input_data" | grep -q '"description"'; then
                        tool_name="Task"
                    elif echo "$tool_input_data" | grep -q '"todos"'; then
                        tool_name="TodoWrite"
                    fi
                fi
            fi
            
            # Build enhanced payload with extracted tool name
            if command -v jq >/dev/null 2>&1; then
                enhanced_payload=$(echo "$raw_payload" | jq --arg tn "$tool_name" '. + {tool_name: $tn}' 2>/dev/null || echo "$raw_payload")
                
                # Add tool_input if available
                if [[ -n "$tool_input" ]]; then
                    enhanced_payload=$(echo "$enhanced_payload" | jq --arg ti "$tool_input" '. + {tool_input: $ti}' 2>/dev/null || echo "$enhanced_payload")
                fi
                
                # Add timestamp
                local timestamp=$(TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S.%3N%z' 2>/dev/null || TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S%z')
                enhanced_payload=$(echo "$enhanced_payload" | jq --arg ts "$timestamp" '. + {timestamp: $ts}' 2>/dev/null || echo "$enhanced_payload")
                
                # Add current working directory
                local cwd=$(pwd)
                enhanced_payload=$(echo "$enhanced_payload" | jq --arg cwd "$cwd" '. + {cwd: $cwd}' 2>/dev/null || echo "$enhanced_payload")
            else
                # Fallback without jq
                enhanced_payload="{\"tool_name\":\"$tool_name\",\"tool_input_preview\":\"...\",\"timestamp\":\"$(TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S%z')\",\"cwd\":\"$(pwd)\"}"
            fi
            ;;
            
        "user_prompt_submit")
            # Extract prompt from payload or environment
            local prompt="${CLAUDE_USER_PROMPT:-}"
            
            if [[ -z "$prompt" ]] && command -v jq >/dev/null 2>&1; then
                prompt=$(echo "$raw_payload" | jq -r '.prompt // .message // .text // .input // ""' 2>/dev/null)
            fi
            
            # Build enhanced payload with prompt
            if command -v jq >/dev/null 2>&1; then
                if [[ -n "$prompt" ]]; then
                    enhanced_payload=$(echo "$raw_payload" | jq --arg p "$prompt" '. + {prompt: $p}' 2>/dev/null || echo "$raw_payload")
                else
                    enhanced_payload="$raw_payload"
                fi
            else
                enhanced_payload="{\"prompt\":\"${prompt:-No prompt captured}\"}"
            fi
            ;;
            
        "notification")
            # Extract notification details
            local notification_type="${CLAUDE_NOTIFICATION_TYPE:-System}"
            local message="${CLAUDE_NOTIFICATION_MESSAGE:-}"
            
            if command -v jq >/dev/null 2>&1; then
                if [[ -z "$message" ]]; then
                    message=$(echo "$raw_payload" | jq -r '.message // .text // .notification // ""' 2>/dev/null)
                fi
                
                enhanced_payload=$(echo "$raw_payload" | jq \
                    --arg nt "$notification_type" \
                    --arg msg "$message" \
                    '. + {notification_type: $nt, message: $msg}' 2>/dev/null || echo "$raw_payload")
            else
                enhanced_payload="{\"notification_type\":\"$notification_type\",\"message\":\"$message\"}"
            fi
            ;;
            
        *)
            # For other hook types, pass through the original payload
            enhanced_payload="$raw_payload"
            ;;
    esac
    
    echo "$enhanced_payload"
}

# Enhanced main function
main_enhanced() {
    # Process command line arguments
    local hook_type="${1:-}"
    local session_id="${2:-}"
    
    # Validate arguments
    if [[ -z "$hook_type" ]] || [[ -z "$session_id" ]]; then
        echo "Error: Missing required arguments" >&2
        echo "Usage: $SCRIPT_NAME <hook_type> <session_id>" >&2
        exit $EXIT_INVALID_ARGS
    fi
    
    # Read payload from stdin
    local raw_payload=""
    if [[ ! -t 0 ]]; then
        raw_payload=$(cat)
        log_info "Read ${#raw_payload} bytes from stdin"
    else
        raw_payload="{}"
        log_info "No stdin data, using empty payload"
    fi
    
    # Log debug information
    log_debug "Hook type: $hook_type"
    log_debug "Session ID: $session_id"
    log_debug "Raw payload: $raw_payload"
    
    # Log environment variables for debugging
    if [[ "${DEBUG:-0}" == "1" ]]; then
        log_debug "Environment variables:"
        env | grep -E "CLAUDE|HOOK" | while read -r line; do
            log_debug "  $line"
        done
    fi
    
    # Process and enhance the payload
    local enhanced_payload=$(process_enhanced_payload "$hook_type" "$raw_payload")
    log_debug "Enhanced payload: $enhanced_payload"
    
    # Call the original script's main function with enhanced payload
    echo "$enhanced_payload" | "${SCRIPT_DIR}/claude_hook_redis.sh" "$hook_type" "$session_id"
}

# Run enhanced main if not sourced
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main_enhanced "$@"
fi