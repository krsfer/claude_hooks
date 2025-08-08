#!/usr/bin/env bash

# Enhanced Hook Payload Processor
# Extracts tool names and prompts from Claude Code hook data
# Version: 1.0.0

set -euo pipefail

# Process and enhance the payload
process_payload() {
    local hook_type="$1"
    local raw_payload="$2"
    
    # Default to raw payload
    local enhanced_payload="$raw_payload"
    
    # Check if jq is available
    if ! command -v jq >/dev/null 2>&1; then
        echo "$enhanced_payload"
        return
    fi
    
    case "$hook_type" in
        "pre_tool_use"|"post_tool_use")
            # Extract existing tool name
            local tool_name=$(echo "$raw_payload" | jq -r '.tool_name // "unknown"' 2>/dev/null)
            
            # If tool_name is unknown, try to infer it
            if [[ "$tool_name" == "unknown" || "$tool_name" == "null" ]]; then
                # Try to get tool_input
                local tool_input=$(echo "$raw_payload" | jq -r '.tool_input // "{}"' 2>/dev/null)
                
                # Infer tool name from tool_input patterns
                if echo "$tool_input" | grep -q '"file_path"'; then
                    tool_name="Read"
                elif echo "$tool_input" | grep -q '"old_string"'; then
                    tool_name="Edit"
                elif echo "$tool_input" | grep -q '"new_string"'; then
                    tool_name="Edit"
                elif echo "$tool_input" | grep -q '"content"'; then
                    tool_name="Write"
                elif echo "$tool_input" | grep -q '"command"'; then
                    # Extract command name
                    local cmd=$(echo "$tool_input" | jq -r '.command // ""' 2>/dev/null)
                    if [[ -n "$cmd" && "$cmd" != "null" ]]; then
                        local cmd_name=$(echo "$cmd" | awk '{print $1}')
                        tool_name="${cmd_name:-Bash}"
                    else
                        tool_name="Bash"
                    fi
                elif echo "$tool_input" | grep -q '"pattern"'; then
                    tool_name="Grep"
                elif echo "$tool_input" | grep -q '"glob"'; then
                    tool_name="Glob"
                elif echo "$tool_input" | grep -q '"path"'; then
                    tool_name="LS"
                elif echo "$tool_input" | grep -q '"url"'; then
                    tool_name="WebFetch"
                elif echo "$tool_input" | grep -q '"query"'; then
                    tool_name="WebSearch"
                elif echo "$tool_input" | grep -q '"description"'; then
                    tool_name="Task"
                elif echo "$tool_input" | grep -q '"todos"'; then
                    tool_name="TodoWrite"
                elif echo "$tool_input" | grep -q '"prompt"'; then
                    tool_name="Task"
                fi
                
                # Update the payload with the inferred tool name
                enhanced_payload=$(echo "$raw_payload" | jq --arg tn "$tool_name" '.tool_name = $tn' 2>/dev/null || echo "$raw_payload")
            fi
            
            # Ensure tool_input_preview is set
            local tool_input_preview=$(echo "$enhanced_payload" | jq -r '.tool_input // "{}" | tostring | .[0:100]' 2>/dev/null || echo "...")
            enhanced_payload=$(echo "$enhanced_payload" | jq --arg tip "$tool_input_preview" '.tool_input_preview = $tip' 2>/dev/null || echo "$enhanced_payload")
            ;;
            
        "user_prompt_submit")
            # Ensure prompt field exists
            local prompt=$(echo "$raw_payload" | jq -r '.prompt // .message // .text // ""' 2>/dev/null)
            if [[ -z "$prompt" || "$prompt" == "null" ]]; then
                prompt="User prompt not captured"
            fi
            enhanced_payload=$(echo "$raw_payload" | jq --arg p "$prompt" '.prompt = $p' 2>/dev/null || echo "$raw_payload")
            ;;
            
        "notification")
            # Ensure notification fields exist
            local notification_type=$(echo "$raw_payload" | jq -r '.notification_type // "System"' 2>/dev/null)
            local message=$(echo "$raw_payload" | jq -r '.message // .text // "Notification"' 2>/dev/null)
            
            enhanced_payload=$(echo "$raw_payload" | jq \
                --arg nt "$notification_type" \
                --arg msg "$message" \
                '.notification_type = $nt | .message = $msg' 2>/dev/null || echo "$raw_payload")
            ;;
            
        *)
            # Pass through for other hook types
            enhanced_payload="$raw_payload"
            ;;
    esac
    
    echo "$enhanced_payload"
}

# Main execution
if [[ $# -lt 1 ]]; then
    echo "Usage: $0 <hook_type> [payload]" >&2
    echo "Reads payload from stdin if not provided as argument" >&2
    exit 1
fi

hook_type="$1"
payload="${2:-}"

# Read from stdin if no payload provided
if [[ -z "$payload" ]] && [[ ! -t 0 ]]; then
    payload=$(cat)
fi

# Default to empty JSON if still no payload
if [[ -z "$payload" ]]; then
    payload="{}"
fi

# Process and output the enhanced payload
process_payload "$hook_type" "$payload"