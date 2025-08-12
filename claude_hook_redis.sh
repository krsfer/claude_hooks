#!/usr/bin/env bash

# Claude Code Hook Redis Publisher
# Sends hook data to Redis channel "hooksdata" with proper JSON structure
# Supports all 8 Claude Code hook types

set -euo pipefail

# Source Redis configuration
if [[ -f ~/.claude/redis_config.env ]]; then
    source ~/.claude/redis_config.env
fi

# Configuration
readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_VERSION="1.0.0"
readonly REDIS_CHANNEL="hooksdata"
readonly LOG_FILE="${CLAUDE_HOOKS_LOG:-$HOME/.claude/logs/hooks.log}"
readonly SEQUENCE_FILE="${CLAUDE_HOOKS_SEQ:-/tmp/.claude_hooks_seq}"

# Exit codes
readonly EXIT_SUCCESS=0
readonly EXIT_INVALID_ARGS=1
readonly EXIT_REDIS_ERROR=2
readonly EXIT_JSON_ERROR=3
readonly EXIT_ENV_ERROR=4

# Color codes for logging (disabled in non-terminal)
if [[ -t 2 ]]; then
    readonly RED='\033[0;31m'
    readonly GREEN='\033[0;32m'
    readonly YELLOW='\033[1;33m'
    readonly NC='\033[0m' # No Color
else
    readonly RED=''
    readonly GREEN=''
    readonly YELLOW=''
    readonly NC=''
fi

# Logging functions
log_error() {
    local timestamp=$(TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S.%3N%z' 2>/dev/null || TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S%z')
    echo -e "${RED}[ERROR]${NC} $*" >&2
    echo "[$timestamp] [ERROR] $*" >> "$LOG_FILE"
}

log_info() {
    local timestamp=$(TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S.%3N%z' 2>/dev/null || TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S%z')
    echo "[$timestamp] [INFO] $*" >> "$LOG_FILE"
}

log_debug() {
    if [[ "${DEBUG:-0}" == "1" ]]; then
        local timestamp=$(TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S.%3N%z' 2>/dev/null || TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S%z')
        echo -e "${YELLOW}[DEBUG]${NC} $*" >&2
        echo "[$timestamp] [DEBUG] $*" >> "$LOG_FILE"
    fi
}

# Log full payload for debugging
log_payload() {
    local hook_type="$1"
    local session_id="$2"
    local payload="$3"
    
    # Create logs directory if it doesn't exist
    mkdir -p "$(dirname "$LOG_FILE")"
    
    local timestamp=$(TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S.%3N%z' 2>/dev/null || TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S%z')
    
    # Log the full payload
    {
        echo "=== HOOK PAYLOAD ==="
        echo "[$timestamp] Type: $hook_type | Session: $session_id"
        echo "--- Raw Payload ---"
        echo "$payload"
        if command -v jq >/dev/null 2>&1; then
            echo "--- Formatted Payload ---"
            echo "$payload" | jq '.' 2>/dev/null || echo "$payload"
        fi
        echo "=================="
        echo ""
    } >> "$LOG_FILE" 2>/dev/null
}

# Cleanup function
cleanup() {
    local exit_code=$?
    if [[ -n "${temp_file:-}" && -f "$temp_file" ]]; then
        rm -f "$temp_file"
    fi
    exit $exit_code
}

trap cleanup EXIT INT TERM

# Usage function
usage() {
    cat << EOF
Usage: $SCRIPT_NAME <hook_type> <session_id> [options]

Sends Claude Code hook data to Redis channel "$REDIS_CHANNEL"

Arguments:
  hook_type     One of: session_start, user_prompt_submit, pre_tool_use,
                post_tool_use, notification, stop_hook, sub_agent_stop_hook,
                pre_compact
  session_id    Unique session identifier (UUID format recommended)

Options:
  -h, --help    Show this help message
  -v, --version Show script version
  -d, --debug   Enable debug logging

Environment Variables:
  REDIS_HOST              Redis host (required)
  REDIS_PASSWORD          Redis password (required)
  REDIS_PORT              Redis port (default: 6380)
  REDIS_TLS               Use TLS connection (default: true)
  CLAUDE_HOOKS_LOG        Log file path (default: ~/.claude/logs/hooks.log)
  CLAUDE_HOOKS_SEQ        Sequence file path (default: /tmp/.claude_hooks_seq)
  DEBUG                   Enable debug mode (0 or 1)

Input:
  Hook payload is read from stdin as JSON

Examples:
  echo '{"prompt": "Hello"}' | $SCRIPT_NAME user_prompt_submit sess-123
  echo '{"tool_name": "bash", "tool_input": {"command": "ls"}}' | $SCRIPT_NAME pre_tool_use sess-456

EOF
}

# Version function
version() {
    echo "$SCRIPT_NAME version $SCRIPT_VERSION"
}

# Validate environment variables
validate_env() {
    if [[ -z "${REDIS_HOST:-}" ]]; then
        log_error "REDIS_HOST environment variable is required"
        exit $EXIT_ENV_ERROR
    fi
    
    if [[ -z "${REDIS_PASSWORD:-}" ]]; then
        log_error "REDIS_PASSWORD environment variable is required"
        exit $EXIT_ENV_ERROR
    fi
    
    # Set defaults
    : "${REDIS_PORT:=6380}"
    : "${REDIS_TLS:=true}"
    
    log_debug "Redis config: host=$REDIS_HOST, port=$REDIS_PORT, tls=$REDIS_TLS"
}

# Validate hook type
validate_hook_type() {
    local hook_type="$1"
    local valid_types=(
        "session_start"
        "user_prompt_submit"
        "pre_tool_use"
        "post_tool_use"
        "notification"
        "stop_hook"
        "sub_agent_stop_hook"
        "pre_compact"
    )
    
    for valid in "${valid_types[@]}"; do
        if [[ "$hook_type" == "$valid" ]]; then
            return 0
        fi
    done
    
    log_error "Invalid hook type: $hook_type"
    log_error "Valid types: ${valid_types[*]}"
    return 1
}

# Validate session ID (basic UUID format check)
validate_session_id() {
    local session_id="$1"
    
    # Allow any non-empty string, but warn if not UUID-like
    if [[ -z "$session_id" ]]; then
        log_error "Session ID cannot be empty"
        return 1
    fi
    
    # Optional: warn if not UUID format
    if ! [[ "$session_id" =~ ^[a-fA-F0-9-]{8,}$ ]]; then
        log_debug "Warning: Session ID '$session_id' doesn't look like a UUID"
    fi
    
    return 0
}

# Generate UUID v4
generate_uuid() {
    if command -v uuidgen >/dev/null 2>&1; then
        uuidgen | tr '[:upper:]' '[:lower:]'
    else
        # Fallback to /proc/sys/kernel/random/uuid on Linux
        if [[ -r /proc/sys/kernel/random/uuid ]]; then
            cat /proc/sys/kernel/random/uuid
        else
            # Last resort: generate pseudo-random UUID
            printf '%04x%04x-%04x-%04x-%04x-%04x%04x%04x\n' \
                $RANDOM $RANDOM $RANDOM \
                $((RANDOM & 0x0fff | 0x4000)) \
                $((RANDOM & 0x3fff | 0x8000)) \
                $RANDOM $RANDOM $RANDOM
        fi
    fi
}

# Generate ISO 8601 timestamp with milliseconds in Central European Time
generate_timestamp() {
    if date --version >/dev/null 2>&1; then
        # GNU date
        TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S.%3N%z'
    else
        # BSD/macOS date (no milliseconds)
        TZ='Europe/Paris' date '+%Y-%m-%dT%H:%M:%S%z'
    fi
}

# Get and increment sequence number for session
get_sequence_number() {
    local session_id="$1"
    local sequence=1
    
    # Create sequence file if it doesn't exist
    if [[ ! -f "$SEQUENCE_FILE" ]]; then
        touch "$SEQUENCE_FILE"
    fi
    
    # Lock file for atomic operations
    local lock_file="${SEQUENCE_FILE}.lock"
    local lock_acquired=0
    local max_wait=5
    local wait_count=0
    
    # Try to acquire lock
    while [[ $wait_count -lt $max_wait ]]; do
        if mkdir "$lock_file" 2>/dev/null; then
            lock_acquired=1
            break
        fi
        sleep 0.1
        ((wait_count++))
    done
    
    if [[ $lock_acquired -eq 1 ]]; then
        # Read current sequence
        if grep -q "^${session_id}:" "$SEQUENCE_FILE" 2>/dev/null; then
            sequence=$(grep "^${session_id}:" "$SEQUENCE_FILE" | cut -d: -f2)
            ((sequence++))
            # Update sequence
            sed -i.bak "/^${session_id}:/d" "$SEQUENCE_FILE" 2>/dev/null || \
                sed -i '' "/^${session_id}:/d" "$SEQUENCE_FILE" 2>/dev/null || true
        fi
        
        # Write new sequence
        echo "${session_id}:${sequence}" >> "$SEQUENCE_FILE"
        
        # Release lock
        rmdir "$lock_file" 2>/dev/null || true
    else
        log_debug "Could not acquire lock for sequence file, using timestamp-based sequence"
        sequence=$(date +%s%N | cut -c1-13)
    fi
    
    echo "$sequence"
}

# Get system context
get_system_context() {
    local context='{}'
    
    # Platform
    local platform="unknown"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        platform="darwin"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        platform="linux"
    fi
    
    # Current working directory
    local cwd="$(pwd)"
    
    # Git info (if in git repo)
    local git_branch=""
    local git_status="unknown"
    if command -v git >/dev/null 2>&1 && git rev-parse --git-dir >/dev/null 2>&1; then
        git_branch=$(git branch --show-current 2>/dev/null || echo "")
        if git diff-index --quiet HEAD -- 2>/dev/null; then
            git_status="clean"
        else
            git_status="dirty"
        fi
    fi
    
    # User agent
    local user_agent="claude-hook-redis/${SCRIPT_VERSION}"
    
    # Build context JSON
    context=$(cat <<EOF
{
    "platform": "$platform",
    "cwd": "$cwd",
    "git_branch": "$git_branch",
    "git_status": "$git_status",
    "user_agent": "$user_agent"
}
EOF
    )
    
    echo "$context"
}

# Escape JSON string
escape_json_string() {
    local input="$1"
    # Escape backslashes first, then quotes, then control characters
    echo "$input" | sed 's/\\/\\\\/g; s/"/\\"/g; s/	/\\t/g; s/
/\\n/g; s//\\r/g'
}

# Merge JSON objects using jq if available, otherwise use simple merge
merge_json() {
    local base="$1"
    local overlay="$2"
    
    if command -v jq >/dev/null 2>&1; then
        echo "$base" | jq -s ".[0] * $overlay" 2>/dev/null || echo "$base"
    else
        # Simple merge - just return overlay if it's not empty
        if [[ "$overlay" != "{}" && -n "$overlay" ]]; then
            echo "$overlay"
        else
            echo "$base"
        fi
    fi
}

# Build the complete JSON payload
build_json_payload() {
    local hook_type="$1"
    local session_id="$2"
    local hook_payload="$3"
    local sequence="$4"
    
    local id=$(generate_uuid)
    local timestamp=$(generate_timestamp)
    local context=$(get_system_context)
    
    # Start time for execution measurement
    local start_time=$(date +%s%N 2>/dev/null || date +%s)
    
    # Build core section
    local core_json='{
        "status": "success",
        "execution_time_ms": 0
    }'
    
    # Parse hook payload if it's valid JSON
    local payload_json="{}"
    if [[ -n "$hook_payload" ]]; then
        if command -v jq >/dev/null 2>&1; then
            # Validate and format with jq
            payload_json=$(echo "$hook_payload" | jq -c '.' 2>/dev/null || echo '{}')
        else
            # Use the payload as-is if no jq available
            payload_json="$hook_payload"
        fi
    fi
    
    # Calculate execution time
    local end_time=$(date +%s%N 2>/dev/null || date +%s)
    local exec_time_ms=0
    if [[ ${#end_time} -gt 10 ]]; then
        exec_time_ms=$(( (end_time - start_time) / 1000000 ))
    fi
    
    # Extract tool name from the environment or payload
    local extracted_tool_name="unknown"
    
    # Log tool extraction attempt for debugging
    if [[ "$hook_type" == "pre_tool_use" || "$hook_type" == "post_tool_use" ]]; then
        log_info "Attempting to extract tool name for $hook_type hook"
        log_debug "Raw payload for tool extraction: ${hook_payload:0:500}"
    fi
    
    # Initialize pattern detection flags
    local detected_patterns=""
    local special_metadata=""
    
    # Check for special patterns in payload and add notifications  
    # Skip notification generation if we're already processing a notification to avoid recursion
    if [[ "$hook_type" != "notification" ]] && command -v jq >/dev/null 2>&1 && [[ -n "$hook_payload" ]]; then
        # Check for "User Prompt" pattern in prompt content
        if echo "$hook_payload" | jq -r '.prompt // empty' 2>/dev/null | grep -qi "user prompt"; then
            log_info "Detected 'User Prompt' pattern in payload"
            detected_patterns="${detected_patterns}user_prompt,"
            special_metadata="${special_metadata}\"user_prompt_detected\":true,"
        fi
        
        # Check for "Session Started" pattern in prompt content
        if echo "$hook_payload" | jq -r '.prompt // empty' 2>/dev/null | grep -qi "session start"; then
            log_info "Detected 'Session Started' pattern in payload"
            detected_patterns="${detected_patterns}session_started,"
            special_metadata="${special_metadata}\"session_started_detected\":true,"
        fi
        
        # Check for session_start hook type
        if [[ "$hook_type" == "session_start" ]]; then
            log_info "Session start hook detected - Claude Code session beginning"
            detected_patterns="${detected_patterns}session_start_hook,"
            special_metadata="${special_metadata}\"is_session_start\":true,"
        fi
        
        # Check for user_prompt_submit hook type  
        if [[ "$hook_type" == "user_prompt_submit" ]]; then
            log_info "User prompt submit hook detected - User submitted prompt to Claude"
            detected_patterns="${detected_patterns}user_prompt_submit_hook,"
            special_metadata="${special_metadata}\"is_user_prompt_submit\":true,"
        fi
        
        # Remove trailing comma from detected patterns
        detected_patterns="${detected_patterns%,}"
        special_metadata="${special_metadata%,}"
    fi
    
    # Try to get tool name from Claude Code environment variables
    if [[ -n "${CLAUDE_TOOL_NAME:-}" ]]; then
        extracted_tool_name="$CLAUDE_TOOL_NAME"
        log_info "Using tool name from CLAUDE_TOOL_NAME: $extracted_tool_name"
        
        # Check if this is an MCP tool from environment variable
        if [[ "$CLAUDE_TOOL_NAME" =~ ^mcp__([^_]+)__(.+)$ ]]; then
            local mcp_server="${BASH_REMATCH[1]}"
            local mcp_tool="${BASH_REMATCH[2]}"
            log_info "Detected MCP tool from env: server='$mcp_server', tool='$mcp_tool'"
            extracted_tool_name="MCP: ${mcp_server}/${mcp_tool}"
            # Add MCP metadata to special metadata
            special_metadata="${special_metadata}\"mcp_server\":\"$mcp_server\",\"mcp_tool\":\"$mcp_tool\",\"is_mcp_tool\":true,"
        fi
    elif [[ -n "${CLAUDE_HOOK_MATCHER:-}" ]]; then
        # Extract tool name from Claude hook matcher (e.g., "BashTool" -> "Bash")
        extracted_tool_name="${CLAUDE_HOOK_MATCHER//Tool/}"
        log_info "Using tool name from CLAUDE_HOOK_MATCHER: $extracted_tool_name"
    else
        # Try to extract from payload using multiple methods
        if command -v jq >/dev/null 2>&1; then
            # First try explicit tool_name field
            local payload_tool_name=$(echo "$payload_json" | jq -r '.tool_name // "unknown"' 2>/dev/null)
            log_debug "Extracted tool_name from JSON: '$payload_tool_name'"
            local payload_tool_lower="$(echo "$payload_tool_name" | tr '[:upper:]' '[:lower:]')"
            log_debug "tool_name lowercase: '$payload_tool_lower', checking against 'unknown'"
            # Accept tool names that are not "unknown", "null", empty, or "Tool" (generic)
            if [[ "$payload_tool_lower" != "unknown" && "$payload_tool_name" != "null" && -n "$payload_tool_name" && "$payload_tool_name" != "Tool" ]]; then
                extracted_tool_name="$payload_tool_name"
                log_info "Found tool_name in payload: $extracted_tool_name"
                
                # Check if this is an MCP tool and extract MCP metadata
                if [[ "$payload_tool_name" =~ ^mcp__([^_]+)__(.+)$ ]]; then
                    local mcp_server="${BASH_REMATCH[1]}"
                    local mcp_tool="${BASH_REMATCH[2]}"
                    log_info "Detected MCP tool: server='$mcp_server', tool='$mcp_tool'"
                    extracted_tool_name="MCP: ${mcp_server}/${mcp_tool}"
                    # Add MCP metadata to special metadata
                    special_metadata="${special_metadata}\"mcp_server\":\"$mcp_server\",\"mcp_tool\":\"$mcp_tool\",\"is_mcp_tool\":true,"
                fi
            else
                log_debug "No valid tool_name in payload, attempting inference..."
                # Try to infer from tool input patterns
                local command_field=$(echo "$payload_json" | jq -r '.command // empty' 2>/dev/null)
                local file_path_field=$(echo "$payload_json" | jq -r '.file_path // empty' 2>/dev/null)
                local pattern_field=$(echo "$payload_json" | jq -r '.pattern // empty' 2>/dev/null)
                local path_field=$(echo "$payload_json" | jq -r '.path // empty' 2>/dev/null)
                local url_field=$(echo "$payload_json" | jq -r '.url // empty' 2>/dev/null)
                local query_field=$(echo "$payload_json" | jq -r '.query // empty' 2>/dev/null)
                local content_field=$(echo "$payload_json" | jq -r '.content // empty' 2>/dev/null)
                local old_string_field=$(echo "$payload_json" | jq -r '.old_string // empty' 2>/dev/null)
                
                # Infer tool name from payload structure
                if [[ -n "$command_field" ]]; then
                    extracted_tool_name="Bash"
                    log_debug "Inferred tool: Bash (from command field)"
                elif [[ -n "$file_path_field" && -n "$content_field" ]]; then
                    extracted_tool_name="Write"
                    log_debug "Inferred tool: Write (from file_path + content)"
                elif [[ -n "$file_path_field" && -n "$old_string_field" ]]; then
                    extracted_tool_name="Edit"
                    log_debug "Inferred tool: Edit (from file_path + old_string)"
                elif [[ -n "$file_path_field" ]]; then
                    extracted_tool_name="Read"
                    log_debug "Inferred tool: Read (from file_path only)"
                elif [[ -n "$pattern_field" ]]; then
                    extracted_tool_name="Grep"
                    log_debug "Inferred tool: Grep (from pattern field)"
                elif [[ -n "$path_field" ]]; then
                    extracted_tool_name="LS"
                    log_debug "Inferred tool: LS (from path field)"
                elif [[ -n "$url_field" ]]; then
                    extracted_tool_name="WebFetch"
                    log_debug "Inferred tool: WebFetch (from url field)"
                elif [[ -n "$query_field" ]]; then
                    extracted_tool_name="WebSearch"
                    log_debug "Inferred tool: WebSearch (from query field)"
                else
                    log_debug "Could not infer tool from standard fields"
                    # Try to extract from nested structure
                    local nested_tool=$(echo "$payload_json" | jq -r '.tool_input.command // .tool_input.file_path // .tool_input.pattern // .tool_input.path // empty' 2>/dev/null)
                    if [[ -n "$nested_tool" ]]; then
                        if echo "$nested_tool" | grep -q '^/'; then
                            extracted_tool_name="LS"
                        elif echo "$nested_tool" | grep -qE '^[a-zA-Z_]+$'; then
                            extracted_tool_name="Bash"
                        fi
                    fi
                fi
            fi
        fi
    fi
    
    # Update payload with extracted tool name if it's a tool use hook
    # Only override if the current tool_name is unknown or if we detected a better one
    if [[ "$hook_type" == "pre_tool_use" || "$hook_type" == "post_tool_use" ]] && command -v jq >/dev/null 2>&1; then
        local current_tool_name=$(echo "$payload_json" | jq -r '.tool_name // "unknown"' 2>/dev/null)
        local current_lower="$(echo "$current_tool_name" | tr '[:upper:]' '[:lower:]')"
        
        # Only override if current tool_name is unknown/generic or if extracted is more specific
        if [[ "$current_lower" == "unknown" || "$current_tool_name" == "Tool" || "$extracted_tool_name" != "unknown" ]]; then
            local final_tool_name="$extracted_tool_name"
            # If enhance script already detected something better, keep it
            if [[ "$current_lower" != "unknown" && "$current_tool_name" != "Tool" && "$extracted_tool_name" == "unknown" ]]; then
                final_tool_name="$current_tool_name"
            fi
            log_info "Setting tool_name to '$final_tool_name' in payload for $hook_type (was: '$current_tool_name')"
            payload_json=$(echo "$payload_json" | jq --arg tn "$final_tool_name" '.tool_name = $tn' 2>/dev/null || echo "$payload_json")
        else
            log_info "Keeping existing tool_name '$current_tool_name' in payload for $hook_type"
        fi
    fi
    
    # Add special metadata if patterns were detected
    if [[ -n "$special_metadata" ]] && command -v jq >/dev/null 2>&1; then
        # Add pattern detection metadata to payload
        local pattern_metadata="{$special_metadata}"
        if [[ -n "$detected_patterns" ]]; then
            pattern_metadata=$(echo "$pattern_metadata" | jq --arg dp "$detected_patterns" '. + {"detected_patterns": $dp}' 2>/dev/null || echo "$pattern_metadata")
        fi
        payload_json=$(echo "$payload_json" | jq --argjson pm "$pattern_metadata" '. + {"pattern_detection": $pm}' 2>/dev/null || echo "$payload_json")
        
        log_debug "Added pattern detection metadata: $pattern_metadata"
    fi

    # Build the complete JSON
    local json_output=$(cat <<EOF
{
    "id": "$id",
    "hook_type": "$hook_type",
    "timestamp": "$timestamp",
    "session_id": "$session_id",
    "sequence": $sequence,
    "core": {
        "status": "success",
        "execution_time_ms": $exec_time_ms
    },
    "payload": $payload_json,
    "context": $context,
    "metrics": {
        "script_version": "$SCRIPT_VERSION"
    }
}
EOF
    )
    
    echo "$json_output"
}

# Send data to Redis
send_to_redis() {
    local json_data="$1"
    local hook_type="$2"
    local session_id="$3"
    local sequence="$4"
    
    # Prepare Redis commands
    local redis_host="$REDIS_HOST"
    local redis_port="${REDIS_PORT:-6380}"
    local redis_pass="$REDIS_PASSWORD"
    
    # Build Redis command with proper TLS support
    local redis_cmd
    if [[ "$(echo "${REDIS_TLS}" | tr '[:upper:]' '[:lower:]')" == "true" || "${REDIS_TLS}" == "1" ]]; then
        # Use certificates from environment variables if available
        if [[ -n "${REDIS_CERT_PATH:-}" && -n "${REDIS_KEY_PATH:-}" && -n "${REDIS_CA_PATH:-}" && 
              -f "${REDIS_CERT_PATH}" && -f "${REDIS_KEY_PATH}" && -f "${REDIS_CA_PATH}" ]]; then
            # Use certificates from environment variables
            redis_cmd=(redis-cli --tls 
                      --cert "$REDIS_CERT_PATH" 
                      --key "$REDIS_KEY_PATH" 
                      --cacert "$REDIS_CA_PATH"
                      -p "$redis_port" 
                      -h "$redis_host" 
                      --pass "$redis_pass")
        else
            # Fallback to basic TLS
            redis_cmd=(redis-cli -h "$redis_host" -p "$redis_port" --pass "$redis_pass" --tls)
            if [[ "${REDIS_TLS_SKIP_VERIFY:-false}" == "true" ]]; then
                redis_cmd+=(--insecure)
            fi
        fi
    else
        # Standard Redis connection
        redis_cmd=(redis-cli -h "$redis_host" -p "$redis_port")
        if [[ -n "$redis_pass" ]]; then
            redis_cmd+=(--pass "$redis_pass")
        fi
    fi
    
    # Escape JSON for Redis command
    local escaped_json=$(echo "$json_data" | sed "s/'/'\\\\''/g")
    
    log_debug "Sending to Redis channel: $REDIS_CHANNEL"
    log_debug "Hook type: $hook_type, Session: $session_id, Sequence: $sequence"
    
    # Send PUBLISH command to Redis
    local result
    if result=$("${redis_cmd[@]}" PUBLISH "$REDIS_CHANNEL" "$escaped_json" 2>&1); then
        log_info "Successfully published hook $hook_type for session $session_id (seq: $sequence)"
        log_debug "Redis response: $result"
        
        # Also store in Redis for persistence (optional)
        if [[ "${REDIS_PERSIST:-false}" == "true" ]]; then
            local key="hooks:${session_id}:${sequence}"
            local timestamp=$(date +%s)
            
            # Store hook data
            "${redis_cmd[@]}" HSET "$key" data "$escaped_json" >/dev/null 2>&1 || true
            
            # Add to indexes
            "${redis_cmd[@]}" ZADD "hooks:index:${hook_type}" "$timestamp" "${session_id}:${sequence}" >/dev/null 2>&1 || true
            "${redis_cmd[@]}" ZADD "hooks:session:${session_id}" "$timestamp" "$sequence" >/dev/null 2>&1 || true
            
            # Update stats
            local date_key=$(TZ='Europe/Paris' date '+%Y-%m-%d')
            "${redis_cmd[@]}" HINCRBY "hooks:stats:${date_key}" "${hook_type}_count" 1 >/dev/null 2>&1 || true
            
            log_debug "Persisted hook data to Redis keys"
        fi
        
        return 0
    else
        log_error "Failed to send to Redis: $result"
        return 1
    fi
}

# Main function
main() {
    local hook_type=""
    local session_id=""
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                usage
                exit $EXIT_SUCCESS
                ;;
            -v|--version)
                version
                exit $EXIT_SUCCESS
                ;;
            -d|--debug)
                export DEBUG=1
                shift
                ;;
            -*)
                log_error "Unknown option: $1"
                usage
                exit $EXIT_INVALID_ARGS
                ;;
            *)
                if [[ -z "$hook_type" ]]; then
                    hook_type="$1"
                elif [[ -z "$session_id" ]]; then
                    session_id="$1"
                else
                    log_error "Too many arguments"
                    usage
                    exit $EXIT_INVALID_ARGS
                fi
                shift
                ;;
        esac
    done
    
    # Validate required arguments
    if [[ -z "$hook_type" || -z "$session_id" ]]; then
        log_error "Missing required arguments"
        usage
        exit $EXIT_INVALID_ARGS
    fi
    
    # Validate inputs
    validate_hook_type "$hook_type" || exit $EXIT_INVALID_ARGS
    validate_session_id "$session_id" || exit $EXIT_INVALID_ARGS
    validate_env
    
    log_info "Processing hook: type=$hook_type, session=$session_id"
    
    # Read payload from stdin
    local hook_payload=""
    if [[ ! -t 0 ]]; then
        hook_payload=$(cat)
        log_debug "Read payload from stdin: ${#hook_payload} bytes"
    else
        log_debug "No stdin data, using empty payload"
        hook_payload="{}"
    fi
    
    # Log the raw payload to ~/.claude/logs/hooks.log
    log_payload "$hook_type" "$session_id" "$hook_payload"
    
    # Get sequence number
    local sequence=$(get_sequence_number "$session_id")
    log_debug "Sequence number: $sequence"
    
    # Build JSON payload
    local json_payload=$(build_json_payload "$hook_type" "$session_id" "$hook_payload" "$sequence")
    
    # Log the final JSON payload that will be sent to Redis
    log_payload "${hook_type}_final" "$session_id" "$json_payload"
    
    if [[ "${DEBUG:-0}" == "1" ]]; then
        log_debug "JSON payload:"
        echo "$json_payload" | (command -v jq >/dev/null 2>&1 && jq '.' || cat) >&2
    fi
    
    # Send to Redis
    if send_to_redis "$json_payload" "$hook_type" "$session_id" "$sequence"; then
        log_info "Hook processed successfully"
        exit $EXIT_SUCCESS
    else
        log_error "Failed to process hook"
        exit $EXIT_REDIS_ERROR
    fi
}

# Run main function if not sourced
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi