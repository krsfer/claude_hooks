# Monitoring and Debugging Guide

Comprehensive guide for monitoring, debugging, and maintaining the Claude Code Redis hook integration system.

## Table of Contents
- [System Monitoring](#system-monitoring)
- [Debug Techniques](#debug-techniques)
- [Log Analysis](#log-analysis)
- [Performance Monitoring](#performance-monitoring)
- [Health Checks](#health-checks)
- [Alerting Setup](#alerting-setup)
- [Troubleshooting Tools](#troubleshooting-tools)

## System Monitoring

### Real-time Hook Monitoring

```bash
#!/bin/bash
# Real-time monitoring script for Claude hooks

monitor_hooks() {
    local redis_host="${REDIS_HOST}"
    local redis_port="${REDIS_PORT:-6380}"
    local redis_pass="${REDIS_PASSWORD}"
    
    echo "Monitoring Claude hooks on Redis channel 'hooksdata'..."
    echo "Press Ctrl+C to stop"
    echo "======================================================"
    
    # Subscribe to Redis channel with formatting
    redis-cli -h "$redis_host" -p "$redis_port" --pass "$redis_pass" \
        ${REDIS_TLS:+--tls} SUBSCRIBE hooksdata | \
    while IFS= read -r line; do
        case "$line" in
            "subscribe")
                read -r channel
                read -r subscriber_count
                echo "[$(date '+%Y-%m-%d %H:%M:%S')] Subscribed to $channel ($subscriber_count subscribers)"
                ;;
            "message")
                read -r channel
                read -r message
                
                # Parse and format message
                local timestamp=$(echo "$message" | jq -r '.timestamp // empty')
                local hook_type=$(echo "$message" | jq -r '.hook_type // empty')
                local session_id=$(echo "$message" | jq -r '.session_id // empty')
                local sequence=$(echo "$message" | jq -r '.sequence // empty')
                local status=$(echo "$message" | jq -r '.core.status // empty')
                
                echo "[$(date '+%H:%M:%S')] $hook_type | $session_id:$sequence | $status"
                
                # Pretty print in debug mode
                if [[ "${MONITOR_DEBUG:-0}" == "1" ]]; then
                    echo "$message" | jq '.' | head -20
                    echo "----------------------------------------"
                fi
                ;;
        esac
    done
}

# Enhanced monitoring with statistics
monitor_with_stats() {
    declare -A hook_counts
    declare -A session_counts
    local start_time=$(date +%s)
    
    monitor_hooks | while IFS= read -r line; do
        echo "$line"
        
        # Extract statistics
        if [[ "$line" =~ \[.*\]\ ([^|]+)\ \|\ ([^:]+):([^|]+)\ \|\ (.+) ]]; then
            local hook_type="${BASH_REMATCH[1]// /}"
            local session_id="${BASH_REMATCH[2]}"
            local status="${BASH_REMATCH[4]// /}"
            
            ((hook_counts[$hook_type]++))
            ((session_counts[$session_id]++))
            
            # Print statistics every 100 hooks
            if (( (${hook_counts[*]/%/+} 0) % 100 == 0 )); then
                local current_time=$(date +%s)
                local elapsed=$((current_time - start_time))
                
                echo ""
                echo "=== STATISTICS (${elapsed}s) ==="
                echo "Hook Types:"
                for type in "${!hook_counts[@]}"; do
                    echo "  $type: ${hook_counts[$type]}"
                done
                echo "Active Sessions: ${#session_counts[@]}"
                echo "================================="
                echo ""
            fi
        fi
    done
}
```

### Dashboard Script

```bash
#!/bin/bash
# Live dashboard for Claude hooks

create_dashboard() {
    local refresh_interval=5
    
    while true; do
        clear
        
        # Header
        echo "================================================================"
        echo "                Claude Hooks Live Dashboard"
        echo "                  $(date '+%Y-%m-%d %H:%M:%S')"
        echo "================================================================"
        echo ""
        
        # Redis connection status
        echo "Redis Connection:"
        if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
            --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} ping >/dev/null 2>&1; then
            echo "  Status: ✅ Connected"
        else
            echo "  Status: ❌ Disconnected"
        fi
        
        # Channel subscribers
        local subscribers=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
            --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} \
            PUBSUB NUMSUB hooksdata 2>/dev/null | tail -n1 || echo "0")
        echo "  Channel Subscribers: $subscribers"
        echo ""
        
        # Recent hooks (if persistence enabled)
        if [[ "${REDIS_PERSIST:-false}" == "true" ]]; then
            echo "Recent Hooks (last 10):"
            local recent_keys=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
                --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} \
                --scan --pattern "hooks:*:*" | tail -10)
            
            if [[ -n "$recent_keys" ]]; then
                echo "$recent_keys" | while IFS= read -r key; do
                    local hook_data=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
                        --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} \
                        HGET "$key" data 2>/dev/null)
                    
                    if [[ -n "$hook_data" ]]; then
                        local hook_type=$(echo "$hook_data" | jq -r '.hook_type // "unknown"')
                        local timestamp=$(echo "$hook_data" | jq -r '.timestamp // ""')
                        local session=$(echo "$hook_data" | jq -r '.session_id // ""')
                        echo "  [$timestamp] $hook_type ($session)"
                    fi
                done
            else
                echo "  No recent hooks found"
            fi
        fi
        echo ""
        
        # System status
        echo "System Status:"
        echo "  Log file: $(ls -lh ~/.claude/logs/hooks.log 2>/dev/null | awk '{print $5}' || echo 'Not found')"
        echo "  Sequence file: $(ls -lh ~/.claude/.sequence 2>/dev/null | awk '{print $5}' || echo 'Not found')"
        
        # Hook scripts status
        local hook_count=$(ls -1 ~/.claude/hooks/*.sh 2>/dev/null | wc -l)
        echo "  Hook scripts: $hook_count installed"
        
        echo ""
        echo "Press Ctrl+C to exit"
        echo "================================================================"
        
        sleep $refresh_interval
    done
}
```

## Debug Techniques

### Debug Mode Configuration

```bash
#!/bin/bash
# Comprehensive debug configuration

enable_debug() {
    # Global debug mode
    export DEBUG=1
    export CLAUDE_HOOKS_DEBUG=1
    
    # Verbose Redis commands
    export REDIS_DEBUG=1
    
    # Trace mode for scripts
    set -x
    
    echo "Debug mode enabled. Additional logging will be generated."
}

# Conditional debug wrapper
debug_wrapper() {
    local command="$1"
    shift
    
    if [[ "${DEBUG:-0}" == "1" ]]; then
        echo "[DEBUG] Executing: $command $*" >&2
        strace -e trace=write -o /tmp/debug_trace.log "$command" "$@" 2>&1 | \
            tee -a /tmp/debug_output.log
    else
        "$command" "$@"
    fi
}

# Function tracing
trace_functions() {
    # Enable function tracing
    set -o functrace
    
    # Trap function calls
    trap 'echo "[TRACE] Entering: $BASH_COMMAND" >&2' DEBUG
}
```

### Hook Testing Framework

```bash
#!/bin/bash
# Test framework for Claude hooks

test_hook() {
    local hook_type="$1"
    local payload="$2"
    local expected_result="${3:-0}"
    local test_name="${4:-$hook_type test}"
    
    echo "Testing: $test_name"
    
    # Capture output and timing
    local start_time=$(date +%s%N)
    local output
    local exit_code
    
    if output=$(echo "$payload" | timeout 10 ./claude_hook_redis.sh "$hook_type" "test-$(date +%s)" 2>&1); then
        exit_code=0
    else
        exit_code=$?
    fi
    
    local end_time=$(date +%s%N)
    local duration_ms=$(( (end_time - start_time) / 1000000 ))
    
    # Evaluate result
    if [[ $exit_code -eq $expected_result ]]; then
        echo "  ✅ PASS (${duration_ms}ms)"
        return 0
    else
        echo "  ❌ FAIL (expected: $expected_result, got: $exit_code)"
        echo "  Output: $output"
        return 1
    fi
}

# Comprehensive test suite
run_debug_tests() {
    echo "Running debug test suite..."
    local tests_passed=0
    local tests_failed=0
    
    # Test valid hook types
    for hook in session_start user_prompt_submit pre_tool_use post_tool_use \
               notification stop_hook sub_agent_stop_hook pre_compact; do
        if test_hook "$hook" '{"test": true}' 0 "Valid $hook"; then
            ((tests_passed++))
        else
            ((tests_failed++))
        fi
    done
    
    # Test invalid hook type
    if test_hook "invalid_hook" '{"test": true}' 1 "Invalid hook type"; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Test empty payload
    if test_hook "notification" '{}' 0 "Empty payload"; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Test malformed JSON
    if test_hook "notification" '{"invalid": json}' 0 "Malformed JSON (should handle gracefully)"; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Summary
    echo ""
    echo "Test Results:"
    echo "  Passed: $tests_passed"
    echo "  Failed: $tests_failed"
    echo "  Total:  $((tests_passed + tests_failed))"
    
    return $tests_failed
}
```

### Network Debugging

```bash
#!/bin/bash
# Network-level debugging tools

debug_redis_connection() {
    local host="$REDIS_HOST"
    local port="$REDIS_PORT"
    
    echo "Debugging Redis connection to $host:$port"
    echo "=========================================="
    
    # DNS resolution
    echo "1. DNS Resolution:"
    nslookup "$host" || echo "  DNS resolution failed"
    
    # Network connectivity
    echo ""
    echo "2. Network Connectivity:"
    if timeout 5 nc -zv "$host" "$port" 2>&1; then
        echo "  Port is reachable"
    else
        echo "  Port is not reachable"
    fi
    
    # SSL/TLS testing
    if [[ "${REDIS_TLS,,}" == "true" ]]; then
        echo ""
        echo "3. TLS Connection:"
        echo | timeout 5 openssl s_client -connect "$host:$port" -servername "$host" 2>&1 | \
            grep -E "(CONNECTED|SSL_connect|verify return code)"
    fi
    
    # Redis ping
    echo ""
    echo "4. Redis Authentication:"
    local ping_result=$(redis-cli -h "$host" -p "$port" \
        --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} ping 2>&1)
    echo "  Result: $ping_result"
    
    # Redis info
    echo ""
    echo "5. Redis Server Info:"
    redis-cli -h "$host" -p "$port" \
        --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} \
        INFO server | head -10
}

# Packet capture for deep debugging
capture_redis_traffic() {
    local interface="${INTERFACE:-any}"
    local host="$REDIS_HOST"
    local port="$REDIS_PORT"
    local output_file="/tmp/redis_capture_$(date +%s).pcap"
    
    echo "Capturing Redis traffic to $output_file"
    echo "Press Ctrl+C to stop capture"
    
    # Capture packets (requires root)
    sudo tcpdump -i "$interface" -w "$output_file" \
        "host $host and port $port" &
    local tcpdump_pid=$!
    
    # Stop capture on interrupt
    trap "sudo kill $tcpdump_pid; echo 'Capture saved to $output_file'" INT
    
    wait $tcpdump_pid
}
```

## Log Analysis

### Log Parser

```bash
#!/bin/bash
# Advanced log analysis tools

parse_logs() {
    local log_file="${CLAUDE_HOOKS_LOG:-~/.claude/logs/hooks.log}"
    local since="${1:-'1 hour ago'}"
    
    if [[ ! -f "$log_file" ]]; then
        echo "Log file not found: $log_file"
        return 1
    fi
    
    # Parse logs since specified time
    local since_timestamp=$(date -d "$since" +%s 2>/dev/null || echo 0)
    
    echo "Analyzing logs since: $(date -d "@$since_timestamp" 2>/dev/null || echo 'beginning')"
    echo "=============================================="
    
    # Extract relevant log entries
    while IFS= read -r line; do
        # Extract timestamp from log line
        local log_timestamp=$(echo "$line" | grep -oP '\[\K[^\]]+' | head -1)
        
        if [[ -n "$log_timestamp" ]]; then
            local log_epoch=$(date -d "$log_timestamp" +%s 2>/dev/null || echo 0)
            if [[ $log_epoch -ge $since_timestamp ]]; then
                echo "$line"
            fi
        fi
    done < "$log_file"
}

analyze_log_patterns() {
    local log_file="${CLAUDE_HOOKS_LOG:-~/.claude/logs/hooks.log}"
    
    echo "Log Analysis Report"
    echo "=================="
    echo ""
    
    # Error analysis
    echo "Error Summary:"
    grep -i error "$log_file" | \
        sed 's/.*ERROR\] //' | \
        sort | uniq -c | sort -nr | head -10
    echo ""
    
    # Hook type distribution
    echo "Hook Type Distribution:"
    grep "Processing hook" "$log_file" | \
        awk '{print $NF}' | \
        sort | uniq -c | sort -nr
    echo ""
    
    # Session analysis
    echo "Top Sessions by Activity:"
    grep "session" "$log_file" | \
        grep -oP 'session [a-zA-Z0-9-]+' | \
        sort | uniq -c | sort -nr | head -10
    echo ""
    
    # Performance analysis
    echo "Performance Metrics:"
    local avg_time=$(grep "execution_time_ms" "$log_file" | \
        grep -oP 'execution_time_ms":\s*\K\d+' | \
        awk '{sum+=$1; count++} END {if(count>0) print sum/count; else print 0}')
    echo "  Average execution time: ${avg_time}ms"
    
    local max_time=$(grep "execution_time_ms" "$log_file" | \
        grep -oP 'execution_time_ms":\s*\K\d+' | \
        sort -n | tail -1)
    echo "  Max execution time: ${max_time}ms"
    
    # Recent activity
    echo ""
    echo "Recent Activity (last 10 entries):"
    tail -10 "$log_file" | while IFS= read -r line; do
        echo "  $line"
    done
}

# Log rotation and cleanup
manage_logs() {
    local log_file="${CLAUDE_HOOKS_LOG:-~/.claude/logs/hooks.log}"
    local max_size_mb="${LOG_MAX_SIZE_MB:-50}"
    local keep_files="${LOG_KEEP_FILES:-5}"
    
    if [[ ! -f "$log_file" ]]; then
        return 0
    fi
    
    # Check file size
    local size_mb=$(du -m "$log_file" | cut -f1)
    
    if [[ $size_mb -gt $max_size_mb ]]; then
        echo "Rotating log file (${size_mb}MB > ${max_size_mb}MB)"
        
        # Rotate logs
        local timestamp=$(date +%Y%m%d_%H%M%S)
        local rotated_log="${log_file}.${timestamp}"
        
        mv "$log_file" "$rotated_log"
        touch "$log_file"
        
        # Compress old log
        gzip "$rotated_log" &
        
        # Clean up old logs
        find "$(dirname "$log_file")" -name "$(basename "$log_file").*.gz" \
            -type f -printf '%T@ %p\n' | \
            sort -n | head -n -$keep_files | cut -d' ' -f2- | \
            xargs -r rm
    fi
}
```

### Real-time Log Analysis

```bash
#!/bin/bash
# Real-time log analysis and alerting

monitor_logs() {
    local log_file="${CLAUDE_HOOKS_LOG:-~/.claude/logs/hooks.log}"
    local alert_threshold=10  # errors per minute
    local error_count=0
    local start_time=$(date +%s)
    
    echo "Monitoring logs for anomalies..."
    
    tail -F "$log_file" | while IFS= read -r line; do
        local current_time=$(date +%s)
        
        # Reset counter every minute
        if [[ $((current_time - start_time)) -gt 60 ]]; then
            if [[ $error_count -gt $alert_threshold ]]; then
                send_alert "High error rate: $error_count errors in last minute"
            fi
            error_count=0
            start_time=$current_time
        fi
        
        # Check for errors
        if echo "$line" | grep -qi error; then
            ((error_count++))
            echo "[ALERT] Error detected: $line"
        fi
        
        # Check for performance issues
        if echo "$line" | grep -oP 'execution_time_ms":\s*\K\d+' | \
           awk '$1 > 5000 {exit 1}'; then
            echo "[ALERT] Slow execution detected: $line"
        fi
        
        # Check for Redis connection issues
        if echo "$line" | grep -qi "redis.*fail\|connection.*error"; then
            echo "[CRITICAL] Redis connection issue: $line"
            send_alert "Redis connection problem detected"
        fi
    done
}

send_alert() {
    local message="$1"
    local timestamp=$(date)
    
    # Log alert
    echo "[$timestamp] ALERT: $message" >> ~/.claude/logs/alerts.log
    
    # Send notification (customize based on your setup)
    case "${ALERT_METHOD:-log}" in
        email)
            echo "$message" | mail -s "Claude Hooks Alert" admin@example.com
            ;;
        slack)
            curl -X POST -H 'Content-type: application/json' \
                --data "{\"text\":\"Claude Hooks Alert: $message\"}" \
                "$SLACK_WEBHOOK_URL"
            ;;
        webhook)
            curl -X POST -H 'Content-type: application/json' \
                --data "{\"alert\":\"$message\",\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}" \
                "$ALERT_WEBHOOK_URL"
            ;;
        *)
            echo "ALERT: $message" >&2
            ;;
    esac
}
```

## Performance Monitoring

### Metrics Collection

```bash
#!/bin/bash
# Performance metrics collection

collect_performance_metrics() {
    local metrics_file="/tmp/claude_hooks_metrics.json"
    local start_time=$(date +%s%N)
    
    # Test hook performance
    echo '{"test": "performance_test"}' | \
        ./claude_hook_redis.sh notification "perf-test-$(date +%s)" >/dev/null 2>&1
    
    local end_time=$(date +%s%N)
    local duration_ns=$((end_time - start_time))
    local duration_ms=$((duration_ns / 1000000))
    
    # System metrics
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//')
    local memory_usage=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}')
    local disk_usage=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
    
    # Redis metrics
    local redis_memory=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
        --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} \
        INFO memory | grep used_memory_human | cut -d: -f2 | tr -d '\r')
    
    local redis_connections=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
        --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} \
        INFO clients | grep connected_clients | cut -d: -f2 | tr -d '\r')
    
    # Create metrics JSON
    cat > "$metrics_file" << EOF
{
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "performance": {
        "hook_duration_ms": $duration_ms,
        "cpu_usage_percent": "${cpu_usage:-0}",
        "memory_usage_percent": "${memory_usage:-0}",
        "disk_usage_percent": "${disk_usage:-0}"
    },
    "redis": {
        "memory_usage": "${redis_memory:-unknown}",
        "connections": ${redis_connections:-0}
    }
}
EOF
    
    cat "$metrics_file"
}

# Performance benchmarking
benchmark_hooks() {
    local iterations="${1:-100}"
    local hook_type="${2:-notification}"
    local results_file="/tmp/benchmark_results.csv"
    
    echo "Running benchmark: $iterations iterations of $hook_type hook"
    echo "timestamp,duration_ms,success" > "$results_file"
    
    for ((i=1; i<=iterations; i++)); do
        local start_time=$(date +%s%N)
        local success=1
        
        if ! echo '{"benchmark": true, "iteration": '$i'}' | \
             ./claude_hook_redis.sh "$hook_type" "benchmark-$i" >/dev/null 2>&1; then
            success=0
        fi
        
        local end_time=$(date +%s%N)
        local duration_ms=$(( (end_time - start_time) / 1000000 ))
        
        echo "$(date +%s),$duration_ms,$success" >> "$results_file"
        
        if [[ $((i % 10)) -eq 0 ]]; then
            echo "  Completed: $i/$iterations"
        fi
        
        sleep 0.1  # Prevent overwhelming
    done
    
    # Calculate statistics
    local avg_time=$(awk -F, 'NR>1 {sum+=$2; count++} END {print sum/count}' "$results_file")
    local success_rate=$(awk -F, 'NR>1 {total++; if($3==1) success++} END {print (success/total)*100}' "$results_file")
    
    echo ""
    echo "Benchmark Results:"
    echo "  Average time: ${avg_time}ms"
    echo "  Success rate: ${success_rate}%"
    echo "  Results file: $results_file"
}
```

### Resource Usage Monitoring

```bash
#!/bin/bash
# Monitor resource usage of hook processes

monitor_resources() {
    local interval="${1:-5}"
    local duration="${2:-300}"  # 5 minutes default
    local end_time=$(($(date +%s) + duration))
    
    echo "Monitoring resource usage (${duration}s, ${interval}s intervals)"
    echo "Time,PID,CPU%,MEM%,RSS_MB,VSZ_MB"
    
    while [[ $(date +%s) -lt $end_time ]]; do
        # Find hook processes
        pgrep -f "claude_hook_redis.sh" | while read -r pid; do
            if [[ -n "$pid" ]]; then
                local stats=$(ps -p "$pid" -o pid,pcpu,pmem,rss,vsz --no-headers 2>/dev/null)
                if [[ -n "$stats" ]]; then
                    local timestamp=$(date '+%H:%M:%S')
                    local rss_mb=$(echo "$stats" | awk '{printf "%.1f", $4/1024}')
                    local vsz_mb=$(echo "$stats" | awk '{printf "%.1f", $5/1024}')
                    echo "$timestamp,$stats,$rss_mb,$vsz_mb" | awk '{gsub(/ +/,","); print}'
                fi
            fi
        done
        
        sleep "$interval"
    done
}

# Memory leak detection
detect_memory_leaks() {
    local threshold_mb="${1:-100}"
    local check_interval=60
    local samples=10
    local baseline=0
    
    echo "Memory leak detection (threshold: ${threshold_mb}MB)"
    
    for ((i=1; i<=samples; i++)); do
        # Get memory usage of all hook processes
        local current_usage=$(pgrep -f "claude_hook_redis.sh" | \
            xargs -r ps -p -o rss= | \
            awk '{sum+=$1} END {printf "%.1f", sum/1024}')
        
        if [[ "$current_usage" != "" ]]; then
            echo "Sample $i: ${current_usage}MB"
            
            if [[ $i -eq 1 ]]; then
                baseline=$(echo "$current_usage" | cut -d. -f1)
            else
                local growth=$(echo "$current_usage $baseline" | \
                    awk '{printf "%.1f", $1-$2}')
                
                if (( $(echo "$growth > $threshold_mb" | bc -l) )); then
                    echo "WARNING: Memory growth detected: ${growth}MB"
                    send_alert "Memory leak suspected: ${growth}MB growth"
                fi
            fi
        else
            echo "Sample $i: No processes found"
        fi
        
        sleep $check_interval
    done
}
```

## Health Checks

### Comprehensive Health Check

```bash
#!/bin/bash
# Comprehensive health check script

health_check() {
    local exit_code=0
    local timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    
    echo "Claude Hooks Health Check - $timestamp"
    echo "======================================="
    
    # Check 1: Configuration
    echo -n "Configuration file... "
    if [[ -f ~/.claude/redis_config.env ]]; then
        source ~/.claude/redis_config.env
        echo "✅ OK"
    else
        echo "❌ MISSING"
        exit_code=1
    fi
    
    # Check 2: Required variables
    echo -n "Environment variables... "
    if [[ -n "$REDIS_HOST" && -n "$REDIS_PASSWORD" ]]; then
        echo "✅ OK"
    else
        echo "❌ MISSING (REDIS_HOST or REDIS_PASSWORD)"
        exit_code=1
    fi
    
    # Check 3: Redis connectivity
    echo -n "Redis connection... "
    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
        --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} \
        ping >/dev/null 2>&1; then
        echo "✅ OK"
    else
        echo "❌ FAILED"
        exit_code=1
    fi
    
    # Check 4: Hook scripts
    echo -n "Hook scripts... "
    local hook_count=$(ls -1 ~/.claude/hooks/*.sh 2>/dev/null | wc -l)
    if [[ $hook_count -eq 8 ]]; then
        echo "✅ OK ($hook_count/8)"
    elif [[ $hook_count -gt 0 ]]; then
        echo "⚠️  PARTIAL ($hook_count/8)"
    else
        echo "❌ MISSING"
        exit_code=1
    fi
    
    # Check 5: Main script
    echo -n "Main script... "
    if [[ -x ./claude_hook_redis.sh ]]; then
        echo "✅ OK"
    else
        echo "❌ MISSING or NOT EXECUTABLE"
        exit_code=1
    fi
    
    # Check 6: Log file
    echo -n "Log file... "
    local log_file="${CLAUDE_HOOKS_LOG:-~/.claude/logs/hooks.log}"
    if [[ -f "$log_file" ]]; then
        local log_size=$(du -h "$log_file" | cut -f1)
        local log_age=$(stat -c %Y "$log_file" 2>/dev/null || stat -f %m "$log_file" 2>/dev/null || echo 0)
        local current_time=$(date +%s)
        local age_hours=$(( (current_time - log_age) / 3600 ))
        
        echo "✅ OK ($log_size, ${age_hours}h old)"
    else
        echo "⚠️  NOT FOUND (will be created)"
    fi
    
    # Check 7: Disk space
    echo -n "Disk space... "
    local disk_usage=$(df -h ~/.claude 2>/dev/null | tail -1 | awk '{print $5}' | sed 's/%//')
    if [[ $disk_usage -lt 90 ]]; then
        echo "✅ OK (${disk_usage}% used)"
    elif [[ $disk_usage -lt 95 ]]; then
        echo "⚠️  HIGH (${disk_usage}% used)"
    else
        echo "❌ CRITICAL (${disk_usage}% used)"
        exit_code=1
    fi
    
    # Check 8: Test hook
    echo -n "Test hook send... "
    if echo '{"health_check": true}' | \
        timeout 10 ./claude_hook_redis.sh notification "health-$(date +%s)" >/dev/null 2>&1; then
        echo "✅ OK"
    else
        echo "❌ FAILED"
        exit_code=1
    fi
    
    # Check 9: Performance
    echo -n "Performance test... "
    local start_time=$(date +%s%N)
    echo '{"perf_test": true}' | \
        ./claude_hook_redis.sh notification "perf-$(date +%s)" >/dev/null 2>&1
    local end_time=$(date +%s%N)
    local duration_ms=$(( (end_time - start_time) / 1000000 ))
    
    if [[ $duration_ms -lt 1000 ]]; then
        echo "✅ OK (${duration_ms}ms)"
    elif [[ $duration_ms -lt 5000 ]]; then
        echo "⚠️  SLOW (${duration_ms}ms)"
    else
        echo "❌ TOO SLOW (${duration_ms}ms)"
        exit_code=1
    fi
    
    # Summary
    echo ""
    if [[ $exit_code -eq 0 ]]; then
        echo "Overall Status: ✅ HEALTHY"
    else
        echo "Overall Status: ❌ ISSUES DETECTED"
    fi
    
    return $exit_code
}

# Automated health monitoring
monitor_health() {
    local check_interval="${1:-300}"  # 5 minutes default
    local alert_file="/tmp/claude_hooks_health_alerts"
    
    while true; do
        if ! health_check >/dev/null 2>&1; then
            local current_time=$(date +%s)
            
            # Check if we've already alerted recently
            if [[ ! -f "$alert_file" ]] || \
               [[ $((current_time - $(cat "$alert_file" 2>/dev/null || echo 0))) -gt 3600 ]]; then
                
                # Send alert
                echo "Health check failed at $(date)" | \
                    send_alert "Claude Hooks health check failure"
                
                echo "$current_time" > "$alert_file"
            fi
        else
            # Clear alert file on successful check
            rm -f "$alert_file"
        fi
        
        sleep "$check_interval"
    done
}
```

## Alerting Setup

### Alert Configuration

```bash
#!/bin/bash
# Alert system configuration

# Alert channels configuration
configure_alerts() {
    cat > ~/.claude/alert_config.sh << 'EOF'
# Alert configuration

# Email alerts
EMAIL_ALERTS_ENABLED="true"
EMAIL_TO="admin@example.com"
EMAIL_FROM="claude-hooks@example.com"
SMTP_SERVER="smtp.example.com"

# Slack alerts  
SLACK_ALERTS_ENABLED="true"
SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
SLACK_CHANNEL="#alerts"

# PagerDuty integration
PAGERDUTY_ENABLED="true"
PAGERDUTY_INTEGRATION_KEY="your-integration-key"

# Custom webhook
WEBHOOK_ENABLED="true"
WEBHOOK_URL="https://your-monitoring-system.com/webhook"
WEBHOOK_SECRET="your-webhook-secret"

# Alert thresholds
ERROR_RATE_THRESHOLD="5"          # errors per minute
LATENCY_THRESHOLD="5000"          # milliseconds
DISK_USAGE_THRESHOLD="90"         # percentage
MEMORY_USAGE_THRESHOLD="80"       # percentage
EOF

    chmod 600 ~/.claude/alert_config.sh
}

# Multi-channel alert sender
send_multi_channel_alert() {
    local severity="$1"  # info, warning, critical
    local message="$2"
    local timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    
    # Load alert configuration
    source ~/.claude/alert_config.sh 2>/dev/null || return 1
    
    # Email alerts
    if [[ "$EMAIL_ALERTS_ENABLED" == "true" ]]; then
        send_email_alert "$severity" "$message" "$timestamp"
    fi
    
    # Slack alerts
    if [[ "$SLACK_ALERTS_ENABLED" == "true" ]]; then
        send_slack_alert "$severity" "$message" "$timestamp"
    fi
    
    # PagerDuty for critical alerts
    if [[ "$PAGERDUTY_ENABLED" == "true" && "$severity" == "critical" ]]; then
        send_pagerduty_alert "$message" "$timestamp"
    fi
    
    # Custom webhook
    if [[ "$WEBHOOK_ENABLED" == "true" ]]; then
        send_webhook_alert "$severity" "$message" "$timestamp"
    fi
}

send_email_alert() {
    local severity="$1"
    local message="$2"
    local timestamp="$3"
    
    local subject="[$severity] Claude Hooks Alert"
    
    cat << EOF | mail -s "$subject" "$EMAIL_TO"
Claude Hooks Alert
=================

Severity: $severity
Time: $timestamp
Message: $message

System Information:
- Hostname: $(hostname)
- User: $(whoami)
- Working Directory: $(pwd)

Please investigate and take appropriate action.
EOF
}

send_slack_alert() {
    local severity="$1"
    local message="$2"
    local timestamp="$3"
    
    local color="good"
    case "$severity" in
        warning) color="warning" ;;
        critical) color="danger" ;;
    esac
    
    local payload=$(cat << EOF
{
    "channel": "$SLACK_CHANNEL",
    "username": "Claude Hooks Monitor",
    "attachments": [
        {
            "color": "$color",
            "title": "Claude Hooks Alert - $severity",
            "text": "$message",
            "fields": [
                {
                    "title": "Timestamp",
                    "value": "$timestamp",
                    "short": true
                },
                {
                    "title": "Host",
                    "value": "$(hostname)",
                    "short": true
                }
            ]
        }
    ]
}
EOF
    )
    
    curl -X POST -H 'Content-type: application/json' \
        --data "$payload" "$SLACK_WEBHOOK_URL" >/dev/null 2>&1
}

send_pagerduty_alert() {
    local message="$1"
    local timestamp="$2"
    
    local payload=$(cat << EOF
{
    "routing_key": "$PAGERDUTY_INTEGRATION_KEY",
    "event_action": "trigger",
    "dedup_key": "claude-hooks-$(date +%Y%m%d)",
    "payload": {
        "summary": "Claude Hooks Critical Alert: $message",
        "timestamp": "$timestamp",
        "severity": "critical",
        "source": "$(hostname)",
        "component": "claude-hooks"
    }
}
EOF
    )
    
    curl -X POST -H 'Content-Type: application/json' \
        --data "$payload" \
        'https://events.pagerduty.com/v2/enqueue' >/dev/null 2>&1
}

send_webhook_alert() {
    local severity="$1"
    local message="$2"
    local timestamp="$3"
    
    local payload=$(cat << EOF
{
    "alert": {
        "severity": "$severity",
        "message": "$message",
        "timestamp": "$timestamp",
        "source": "claude-hooks",
        "hostname": "$(hostname)",
        "version": "1.0.0"
    }
}
EOF
    )
    
    # Sign payload if secret is configured
    if [[ -n "$WEBHOOK_SECRET" ]]; then
        local signature=$(echo -n "$payload" | \
            openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -binary | \
            base64)
        
        curl -X POST \
            -H 'Content-Type: application/json' \
            -H "X-Hook-Signature: $signature" \
            --data "$payload" "$WEBHOOK_URL" >/dev/null 2>&1
    else
        curl -X POST \
            -H 'Content-Type: application/json' \
            --data "$payload" "$WEBHOOK_URL" >/dev/null 2>&1
    fi
}
```

## Troubleshooting Tools

### Interactive Debug Shell

```bash
#!/bin/bash
# Interactive debugging shell for Claude hooks

debug_shell() {
    echo "Claude Hooks Debug Shell"
    echo "======================="
    echo "Available commands:"
    echo "  test <hook_type> [payload]  - Test a hook"
    echo "  monitor                     - Monitor Redis channel"
    echo "  logs [lines]               - Show recent logs"
    echo "  health                     - Run health check"
    echo "  redis <command>            - Execute Redis command"
    echo "  performance               - Run performance test"
    echo "  help                      - Show this help"
    echo "  exit                      - Exit debug shell"
    echo ""
    
    while true; do
        read -p "debug> " -a cmd
        
        case "${cmd[0]}" in
            test)
                local hook_type="${cmd[1]:-notification}"
                local payload="${cmd[2]:-'{\"debug\": true}'}"
                echo "Testing $hook_type with payload: $payload"
                echo "$payload" | ./claude_hook_redis.sh "$hook_type" "debug-$(date +%s)"
                ;;
            monitor)
                echo "Monitoring Redis channel (Ctrl+C to stop)..."
                monitor_hooks
                ;;
            logs)
                local lines="${cmd[1]:-20}"
                tail -n "$lines" "${CLAUDE_HOOKS_LOG:-~/.claude/logs/hooks.log}"
                ;;
            health)
                health_check
                ;;
            redis)
                local redis_cmd="${cmd[*]:1}"
                echo "Executing: $redis_cmd"
                redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
                    --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} $redis_cmd
                ;;
            performance)
                echo "Running performance test..."
                benchmark_hooks 10
                ;;
            help)
                echo "Available commands:"
                echo "  test, monitor, logs, health, redis, performance, help, exit"
                ;;
            exit|quit)
                echo "Goodbye!"
                break
                ;;
            "")
                continue
                ;;
            *)
                echo "Unknown command: ${cmd[0]}. Type 'help' for available commands."
                ;;
        esac
        echo ""
    done
}

# Auto-diagnostic tool
auto_diagnose() {
    echo "Running automatic diagnostics..."
    echo "=============================="
    
    local issues_found=0
    
    # Check for common issues
    if ! health_check >/dev/null 2>&1; then
        echo "❌ Health check failed"
        ((issues_found++))
        
        # Detailed diagnosis
        echo "Running detailed diagnosis..."
        
        # Check Redis connection
        if ! redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
            --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} ping >/dev/null 2>&1; then
            echo "  - Redis connection failed"
            echo "  - Suggested fix: Check REDIS_HOST, REDIS_PASSWORD, and network connectivity"
        fi
        
        # Check file permissions
        if [[ ! -x ./claude_hook_redis.sh ]]; then
            echo "  - Main script not executable"
            echo "  - Suggested fix: chmod +x ./claude_hook_redis.sh"
        fi
        
        # Check disk space
        local disk_usage=$(df -h ~/.claude 2>/dev/null | tail -1 | awk '{print $5}' | sed 's/%//' || echo 100)
        if [[ $disk_usage -gt 95 ]]; then
            echo "  - Low disk space (${disk_usage}%)"
            echo "  - Suggested fix: Clean up old logs and temporary files"
        fi
    else
        echo "✅ All basic checks passed"
    fi
    
    # Performance check
    local start_time=$(date +%s%N)
    echo '{"diagnostic": true}' | \
        ./claude_hook_redis.sh notification "diag-$(date +%s)" >/dev/null 2>&1
    local end_time=$(date +%s%N)
    local duration_ms=$(( (end_time - start_time) / 1000000 ))
    
    if [[ $duration_ms -gt 5000 ]]; then
        echo "⚠️  Performance issue detected (${duration_ms}ms)"
        echo "  - Suggested fix: Check network latency to Redis server"
        ((issues_found++))
    fi
    
    echo ""
    if [[ $issues_found -eq 0 ]]; then
        echo "✅ No issues detected. System appears healthy."
    else
        echo "❌ $issues_found issue(s) found. Please review the suggestions above."
    fi
    
    return $issues_found
}
```

## Best Practices

### Monitoring Checklist

- [ ] Set up real-time log monitoring
- [ ] Configure automated health checks
- [ ] Implement performance benchmarking
- [ ] Set up multi-channel alerting
- [ ] Monitor Redis server metrics
- [ ] Track hook success/failure rates
- [ ] Monitor system resources
- [ ] Set up log rotation
- [ ] Implement debug tools
- [ ] Create diagnostic procedures

### Debug Workflow

1. **Immediate Issues**: Check logs, Redis connectivity
2. **Performance Issues**: Run benchmarks, monitor resources
3. **Intermittent Issues**: Enable debug mode, long-term monitoring
4. **Complex Issues**: Use debug shell, packet capture
5. **Resolution**: Document findings, update monitoring

### Emergency Response

1. **Service Down**: Run health check, check Redis connectivity
2. **High Error Rate**: Check logs, investigate patterns
3. **Performance Degradation**: Monitor resources, check network
4. **Security Incident**: Review audit logs, check access patterns