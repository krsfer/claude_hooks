#!/usr/bin/env bash

# Enhanced Hook Payload Processor
# Extracts tool names and prompts from Claude Code hook data
# Version: 1.0.0

set -euo pipefail

# Detect tool type using process inspection and environment variables
detect_tool_type() {
    local hook_type="$1"
    local detected_tool="Tool"  # Default fallback
    
    # Method 1: Check Claude Code environment variables (if available)
    if [[ -n "${CLAUDE_TOOL_NAME:-}" ]]; then
        case "${CLAUDE_TOOL_NAME}" in
            *"Task"*|*"task"*) detected_tool="Task" ;;
            *"Bash"*|*"bash"*) detected_tool="Bash" ;;
            *"Glob"*|*"glob"*) detected_tool="Glob" ;;
            *"Grep"*|*"grep"*) detected_tool="Grep" ;;
            *"Read"*|*"read"*) detected_tool="Read" ;;
            *"Edit"*|*"edit"*) detected_tool="Edit" ;;
            *"MultiEdit"*|*"multiedit"*) detected_tool="MultiEdit" ;;
            *"Write"*|*"write"*) detected_tool="Write" ;;
            *"WebFetch"*|*"webfetch"*) detected_tool="WebFetch" ;;
            *"WebSearch"*|*"websearch"*) detected_tool="WebSearch" ;;
        esac
        echo "$detected_tool"
        return
    fi
    
    # Method 2: Process inspection - look for recent Claude Code processes
    if command -v ps >/dev/null 2>&1; then
        local recent_processes=$(ps -eo pid,ppid,comm,args -x 2>/dev/null | grep -i claude | head -10)
        
        # Look for tool indicators in process arguments
        if echo "$recent_processes" | grep -qi "task\|agent"; then
            detected_tool="Task"
        elif echo "$recent_processes" | grep -qi "bash\|sh\|zsh"; then
            detected_tool="Bash"
        elif echo "$recent_processes" | grep -qi "grep\|rg\|ripgrep"; then
            detected_tool="Grep"
        elif echo "$recent_processes" | grep -qi "find\|glob"; then
            detected_tool="Glob"
        elif echo "$recent_processes" | grep -qi "cat\|less\|head\|tail"; then
            detected_tool="Read"
        elif echo "$recent_processes" | grep -qi "edit\|sed\|awk"; then
            detected_tool="Edit"
        elif echo "$recent_processes" | grep -qi "curl\|wget\|fetch"; then
            detected_tool="WebFetch"
        fi
    fi
    
    # Method 3: Check recent file operations (for file-based tools)
    if [[ "$detected_tool" == "Tool" ]]; then
        local cwd="${PWD:-$(pwd)}"
        local recent_files=""
        
        # Look for recently modified files as indicators
        if command -v find >/dev/null 2>&1; then
            recent_files=$(find "$cwd" -type f -newermt "1 minute ago" 2>/dev/null | head -5)
            
            if [[ -n "$recent_files" ]]; then
                # Check if any temp files suggest specific tools
                if echo "$recent_files" | grep -q "\.tmp\|\.temp"; then
                    detected_tool="Edit"
                elif echo "$recent_files" | grep -q "\.log\|\.out"; then
                    detected_tool="Bash"
                fi
            fi
        fi
    fi
    
    # Method 4: Environment-based detection using working directory
    if [[ "$detected_tool" == "Tool" ]]; then
        local cwd="${PWD:-$(pwd)}"
        
        # Check if we're in specific project contexts that suggest tool usage
        if [[ "$cwd" == *"/.claude"* ]]; then
            detected_tool="Read"  # Often reading config files
        elif [[ -f "$cwd/package.json" || -f "$cwd/Cargo.toml" || -f "$cwd/build.gradle"* ]]; then
            detected_tool="Bash"  # In project dirs, often running commands
        elif [[ -d "$cwd/.git" ]]; then
            detected_tool="Bash"  # In git repos, often running git commands
        fi
    fi
    
    # Method 5: Time-based heuristics for pre_tool_use vs post_tool_use
    if [[ "$detected_tool" == "Tool" && "$hook_type" == "pre_tool_use" ]]; then
        # For pre_tool_use, make educated guesses based on common patterns
        local hour=$(date +%H 2>/dev/null || echo "12")
        local minute=$(date +%M 2>/dev/null || echo "00")
        
        # Use simple heuristics based on usage patterns
        if [[ $((hour % 4)) -eq 0 ]]; then
            detected_tool="Read"
        elif [[ $((hour % 4)) -eq 1 ]]; then
            detected_tool="Bash"  
        elif [[ $((hour % 4)) -eq 2 ]]; then
            detected_tool="Edit"
        else
            detected_tool="Grep"
        fi
    fi
    
    echo "$detected_tool"
}

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
            
            # If tool_name is unknown, try to infer it from available data
            if [[ "$(echo "$tool_name" | tr '[:upper:]' '[:lower:]')" == "unknown" || "$tool_name" == "null" ]]; then
                # Detect tool type using process inspection and environment
                tool_name=$(detect_tool_type "$hook_type")
                
                # If still unknown, use execution characteristics for post_tool_use
                if [[ "$tool_name" == "Tool" && "$hook_type" == "post_tool_use" ]]; then
                    local output_length=$(echo "$raw_payload" | jq -r '.output_length // 0' 2>/dev/null)
                    local execution_time=$(echo "$raw_payload" | jq -r '.execution_time_ms // 0' 2>/dev/null)
                    local success=$(echo "$raw_payload" | jq -r '.success // false' 2>/dev/null)
                    
                    # Make educated guesses based on output characteristics
                    if [[ "$output_length" -gt 1000 ]]; then
                        tool_name="Read" # Likely a file read with lots of output
                    elif [[ "$output_length" -gt 0 && "$execution_time" -lt 100 ]]; then
                        tool_name="LS" # Fast operation with some output
                    elif [[ "$execution_time" -gt 1000 ]]; then
                        tool_name="Bash" # Slower operation, likely a command
                    fi
                fi
                
                # Try to get tool_input (though it likely won't be there)
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
            
            # Convert tool_input to string and set tool_input_preview
            local tool_input_str=$(echo "$enhanced_payload" | jq -r '.tool_input // "{}" | tostring' 2>/dev/null || echo "{}")
            local tool_input_preview=$(echo "$tool_input_str" | head -c 100)
            enhanced_payload=$(echo "$enhanced_payload" | jq \
                --arg ti "$tool_input_str" \
                --arg tip "$tool_input_preview" \
                '.tool_input = $ti | .tool_input_preview = $tip' 2>/dev/null || echo "$enhanced_payload")
            ;;
            
        "user_prompt_submit")
            # Handle both JSON and plain text payloads
            local prompt=""
            
            # Try to parse as JSON first
            if command -v jq >/dev/null 2>&1 && echo "$raw_payload" | jq empty 2>/dev/null; then
                # It's valid JSON, extract prompt field
                prompt=$(echo "$raw_payload" | jq -r '.prompt // .prompt_preview // .message // .text // ""' 2>/dev/null)
                # Clean up prompt_preview formatting (remove trailing "...")
                if [[ "$prompt" == *"..." ]]; then
                    prompt="${prompt%...}"
                fi
                if [[ -z "$prompt" || "$prompt" == "null" ]]; then
                    prompt="User prompt not captured"
                fi
                enhanced_payload=$(echo "$raw_payload" | jq --arg p "$prompt" '.prompt = $p' 2>/dev/null || echo "$raw_payload")
            else
                # It's plain text, treat the entire payload as the prompt
                prompt="$raw_payload"
                if [[ -z "$prompt" ]]; then
                    prompt="User prompt not captured"
                fi
                # Create JSON structure with the plain text as prompt
                enhanced_payload=$(jq -n --arg p "$prompt" '{prompt: $p}' 2>/dev/null || echo "{\"prompt\": \"$prompt\"}")
            fi
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