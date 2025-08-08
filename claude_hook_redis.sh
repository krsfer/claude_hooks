#!/usr/bin/env bash

# Claude Code Hook Redis Publisher
# Sends hook data to Redis channel "hooksdata" with proper JSON structure
# Supports all 8 Claude Code hook types

set -euo pipefail

# Configuration
readonly SCRIPT_NAME="$(basename "$0")"
readonly SCRIPT_VERSION="1.0.0"
readonly REDIS_CHANNEL="hooksdata"
readonly LOG_FILE="${CLAUDE_HOOKS_LOG:-/tmp/claude_hooks.log}"
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
  CLAUDE_HOOKS_LOG        Log file path (default: /tmp/claude_hooks.log)
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
        # Use the same TLS configuration as your redis-tls alias
        local cert_dir="$HOME/.redis/certs"
        if [[ -f "$cert_dir/redis-server.crt" && -f "$cert_dir/redis-server.key" && -f "$cert_dir/ca.crt" ]]; then
            # Use certificates like your redis-tls alias
            redis_cmd=(redis-cli --tls 
                      --cert "$cert_dir/redis-server.crt" 
                      --key "$cert_dir/redis-server.key" 
                      --cacert "$cert_dir/ca.crt"
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
    
    # Get sequence number
    local sequence=$(get_sequence_number "$session_id")
    log_debug "Sequence number: $sequence"
    
    # Build JSON payload
    local json_payload=$(build_json_payload "$hook_type" "$session_id" "$hook_payload" "$sequence")
    
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