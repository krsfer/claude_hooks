# Usage Examples

Comprehensive examples for using the Claude Code Redis hook integration with all 8 hook types.

## Table of Contents
- [Basic Usage](#basic-usage)
- [Hook Type Examples](#hook-type-examples)
- [Advanced Scenarios](#advanced-scenarios)
- [Integration Patterns](#integration-patterns)
- [Automation Examples](#automation-examples)

## Basic Usage

### Simple Command Structure

```bash
# Basic syntax
echo '<JSON_PAYLOAD>' | ./claude_hook_redis.sh <HOOK_TYPE> <SESSION_ID>

# With environment variables
export REDIS_HOST="redis.example.com"
export REDIS_PASSWORD="secret"
echo '{"key": "value"}' | ./claude_hook_redis.sh notification session-123

# With debug output
DEBUG=1 echo '{"test": true}' | ./claude_hook_redis.sh notification test-session
```

### Using Configuration File

```bash
# Source configuration
source ~/.claude/redis_config.env

# Send hook
echo '{"message": "Hello"}' | ./claude_hook_redis.sh notification my-session
```

## Hook Type Examples

### 1. session_start

Triggered when a new Claude Code session begins.

```bash
# Basic session start
echo '{
  "source": "startup"
}' | ./claude_hook_redis.sh session_start sess-$(date +%s)

# With full context
echo '{
  "source": "startup",
  "cwd": "'$(pwd)'",
  "transcript_path": "/tmp/claude_transcript.txt",
  "user": "'$USER'",
  "hostname": "'$(hostname)'",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
}' | ./claude_hook_redis.sh session_start sess-001

# From a script
#!/bin/bash
SESSION_ID="claude-$(date +%Y%m%d-%H%M%S)"
cat <<EOF | ./claude_hook_redis.sh session_start "$SESSION_ID"
{
  "source": "automated",
  "cwd": "$(pwd)",
  "git_branch": "$(git branch --show-current 2>/dev/null || echo 'none')",
  "project": "$(basename $(pwd))",
  "environment": "development"
}
EOF
```

### 2. user_prompt_submit

Triggered when user submits a prompt to Claude.

```bash
# Simple prompt
echo '{
  "prompt": "Write a hello world program in Python"
}' | ./claude_hook_redis.sh user_prompt_submit sess-001

# Complex prompt with context
echo '{
  "prompt": "Refactor the authentication module",
  "context": {
    "files_open": ["auth.py", "models.py", "tests.py"],
    "previous_prompt": "Add user authentication",
    "session_duration": 1800
  },
  "metadata": {
    "prompt_length": 35,
    "has_code": false,
    "language_hint": "python"
  }
}' | ./claude_hook_redis.sh user_prompt_submit sess-001

# Multi-line prompt
PROMPT="Please help me with the following:
1. Review the code for security issues
2. Suggest performance improvements
3. Add comprehensive error handling"

jq -n --arg prompt "$PROMPT" '{prompt: $prompt}' | \
  ./claude_hook_redis.sh user_prompt_submit sess-001
```

### 3. pre_tool_use

Triggered before Claude executes a tool.

```bash
# Bash command execution
echo '{
  "tool_name": "bash",
  "tool_input": {
    "command": "ls -la /tmp"
  }
}' | ./claude_hook_redis.sh pre_tool_use sess-001

# File read operation
echo '{
  "tool_name": "read_file",
  "tool_input": {
    "path": "/home/user/project/main.py",
    "encoding": "utf-8"
  }
}' | ./claude_hook_redis.sh pre_tool_use sess-001

# File write operation
echo '{
  "tool_name": "write_file",
  "tool_input": {
    "path": "/tmp/output.txt",
    "content": "Hello, World!",
    "mode": "w"
  }
}' | ./claude_hook_redis.sh pre_tool_use sess-001

# Complex tool with multiple parameters
echo '{
  "tool_name": "edit_file",
  "tool_input": {
    "file": "app.py",
    "changes": [
      {
        "line": 10,
        "operation": "replace",
        "content": "import logging"
      },
      {
        "line": 25,
        "operation": "insert",
        "content": "    logger.info(\"Processing started\")"
      }
    ]
  },
  "context": {
    "reason": "Add logging support",
    "risk_level": "low"
  }
}' | ./claude_hook_redis.sh pre_tool_use sess-001
```

### 4. post_tool_use

Triggered after Claude executes a tool.

```bash
# Successful command execution
echo '{
  "tool_name": "bash",
  "tool_input": {
    "command": "echo \"Hello\""
  },
  "tool_response": {
    "output": "Hello\n",
    "exit_code": 0,
    "success": true
  },
  "execution_time_ms": 15
}' | ./claude_hook_redis.sh post_tool_use sess-001

# Failed command
echo '{
  "tool_name": "bash",
  "tool_input": {
    "command": "false"
  },
  "tool_response": {
    "output": "",
    "exit_code": 1,
    "success": false,
    "error": "Command failed with exit code 1"
  },
  "execution_time_ms": 8
}' | ./claude_hook_redis.sh post_tool_use sess-001

# File operation result
echo '{
  "tool_name": "write_file",
  "tool_input": {
    "path": "/tmp/data.json",
    "content": "{\"key\": \"value\"}"
  },
  "tool_response": {
    "success": true,
    "bytes_written": 16,
    "file_created": true
  },
  "execution_time_ms": 23
}' | ./claude_hook_redis.sh post_tool_use sess-001

# Complex response with metrics
echo '{
  "tool_name": "run_tests",
  "tool_input": {
    "test_file": "test_app.py"
  },
  "tool_response": {
    "success": true,
    "tests_run": 42,
    "tests_passed": 40,
    "tests_failed": 2,
    "coverage": 87.5,
    "duration_ms": 1234
  },
  "execution_time_ms": 1250,
  "metadata": {
    "framework": "pytest",
    "parallel": true
  }
}' | ./claude_hook_redis.sh post_tool_use sess-001
```

### 5. notification

General notifications from Claude Code.

```bash
# Simple notification
echo '{
  "message": "Task completed successfully"
}' | ./claude_hook_redis.sh notification sess-001

# Warning notification
echo '{
  "level": "warning",
  "message": "Large file detected, processing may be slow",
  "details": {
    "file": "database_dump.sql",
    "size_mb": 523
  }
}' | ./claude_hook_redis.sh notification sess-001

# Error notification
echo '{
  "level": "error",
  "message": "Failed to connect to API",
  "error": {
    "code": "CONNECTION_TIMEOUT",
    "details": "Timeout after 30 seconds",
    "retry_count": 3
  }
}' | ./claude_hook_redis.sh notification sess-001

# Progress notification
echo '{
  "type": "progress",
  "message": "Processing files",
  "progress": {
    "current": 15,
    "total": 100,
    "percentage": 15
  }
}' | ./claude_hook_redis.sh notification sess-001
```

### 6. stop_hook

Triggered when stop hook is activated.

```bash
# Stop hook activated
echo '{
  "stop_hook_active": true,
  "reason": "User requested pause",
  "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
}' | ./claude_hook_redis.sh stop_hook sess-001

# Stop hook with context
echo '{
  "stop_hook_active": true,
  "trigger": "error_threshold",
  "context": {
    "error_count": 5,
    "last_error": "Memory limit exceeded",
    "duration_ms": 45000
  }
}' | ./claude_hook_redis.sh stop_hook sess-001
```

### 7. sub_agent_stop_hook

Triggered for sub-agent stop hooks.

```bash
# Sub-agent stop
echo '{
  "stop_hook_active": false,
  "agent_id": "sub-agent-001",
  "parent_session": "main-session-001"
}' | ./claude_hook_redis.sh sub_agent_stop_hook sess-001

# With sub-agent details
echo '{
  "stop_hook_active": true,
  "agent_id": "analyzer-002",
  "task": "code_review",
  "status": "completed",
  "results": {
    "issues_found": 3,
    "suggestions": 7
  }
}' | ./claude_hook_redis.sh sub_agent_stop_hook sess-001
```

### 8. pre_compact

Triggered before context compaction.

```bash
# Auto compaction
echo '{
  "trigger": "auto",
  "context_size": 8192,
  "threshold": 8000
}' | ./claude_hook_redis.sh pre_compact sess-001

# Manual compaction with instructions
echo '{
  "trigger": "manual",
  "custom_instructions": "Keep all error messages and test results",
  "preserve": ["errors", "tests", "user_prompts"],
  "context_before": 8500,
  "target_size": 4000
}' | ./claude_hook_redis.sh pre_compact sess-001

# Scheduled compaction
echo '{
  "trigger": "scheduled",
  "schedule": "every_1000_tokens",
  "strategy": "sliding_window",
  "window_size": 5000
}' | ./claude_hook_redis.sh pre_compact sess-001
```

## Advanced Scenarios

### Batch Processing

```bash
#!/bin/bash
# Process multiple hooks in sequence

SESSION_ID="batch-$(date +%s)"

# Start session
echo '{"source": "batch_processor"}' | \
  ./claude_hook_redis.sh session_start "$SESSION_ID"

# Process prompts from file
while IFS= read -r prompt; do
  echo "{\"prompt\": \"$prompt\"}" | \
    ./claude_hook_redis.sh user_prompt_submit "$SESSION_ID"
  sleep 0.1  # Rate limiting
done < prompts.txt

# End session
echo '{"message": "Batch processing complete"}' | \
  ./claude_hook_redis.sh notification "$SESSION_ID"
```

### Error Handling

```bash
#!/bin/bash
# Robust hook sending with error handling

send_hook() {
  local hook_type=$1
  local session_id=$2
  local payload=$3
  local max_retries=3
  local retry_count=0
  
  while [ $retry_count -lt $max_retries ]; do
    if echo "$payload" | ./claude_hook_redis.sh "$hook_type" "$session_id"; then
      echo "Hook sent successfully"
      return 0
    else
      retry_count=$((retry_count + 1))
      echo "Attempt $retry_count failed, retrying..." >&2
      sleep $((retry_count * 2))  # Exponential backoff
    fi
  done
  
  echo "Failed to send hook after $max_retries attempts" >&2
  return 1
}

# Usage
send_hook "notification" "sess-001" '{"message": "Test"}'
```

### Pipeline Integration

```bash
#!/bin/bash
# CI/CD pipeline integration

# GitHub Actions example
- name: Send deployment notification
  env:
    REDIS_HOST: ${{ secrets.REDIS_HOST }}
    REDIS_PASSWORD: ${{ secrets.REDIS_PASSWORD }}
  run: |
    echo '{
      "event": "deployment",
      "status": "success",
      "environment": "production",
      "version": "'${{ github.sha }}'",
      "deployed_by": "'${{ github.actor }}'"
    }' | ./claude_hook_redis.sh notification "deploy-${{ github.run_id }}"
```

### Monitoring Script

```bash
#!/bin/bash
# Monitor Claude Code activity

monitor_session() {
  local session_id="monitor-$(date +%Y%m%d)"
  
  # Send heartbeat every 60 seconds
  while true; do
    echo '{
      "type": "heartbeat",
      "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
      "metrics": {
        "memory_usage": "'$(free -m | awk 'NR==2{printf "%s", $3}')'",
        "cpu_load": "'$(uptime | awk -F'load average:' '{print $2}')'",
        "disk_usage": "'$(df -h / | awk 'NR==2{print $5}')''"
      }
    }' | ./claude_hook_redis.sh notification "$session_id"
    
    sleep 60
  done
}

monitor_session &
```

### Data Transformation

```bash
#!/bin/bash
# Transform and enrich hook data

transform_hook() {
  local input=$(cat)
  local timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  local hostname=$(hostname)
  
  # Add metadata using jq
  echo "$input" | jq --arg ts "$timestamp" --arg host "$hostname" '. + {
    metadata: {
      timestamp: $ts,
      hostname: $host,
      environment: env.ENVIRONMENT // "development"
    }
  }'
}

# Usage
echo '{"prompt": "Hello"}' | transform_hook | \
  ./claude_hook_redis.sh user_prompt_submit sess-001
```

## Integration Patterns

### Claude Code Wrapper

```bash
#!/bin/bash
# Wrapper script for Claude Code with automatic hook integration

claude_with_hooks() {
  local session_id="claude-$(date +%Y%m%d-%H%M%S)"
  
  # Start session
  echo '{"source": "wrapper", "command": "'"$*"'"}' | \
    ./claude_hook_redis.sh session_start "$session_id"
  
  # Export session ID for hooks
  export CLAUDE_SESSION_ID="$session_id"
  
  # Run Claude Code
  claude "$@"
  local exit_code=$?
  
  # End session
  echo '{"exit_code": '$exit_code'}' | \
    ./claude_hook_redis.sh notification "$session_id"
  
  return $exit_code
}

# Usage
claude_with_hooks "Write a Python script"
```

### Docker Integration

```dockerfile
# Dockerfile with hooks
FROM ubuntu:latest

# Install dependencies
RUN apt-get update && apt-get install -y \
    redis-tools \
    jq \
    bash

# Copy hook scripts
COPY claude_hook_redis.sh /usr/local/bin/
COPY hooks/ /root/.claude/hooks/

# Set environment
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379

# Entry point with hooks
ENTRYPOINT ["/usr/local/bin/claude_with_hooks.sh"]
```

### Kubernetes Job

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: claude-hook-processor
spec:
  template:
    spec:
      containers:
      - name: hook-sender
        image: claude-hooks:latest
        env:
        - name: REDIS_HOST
          value: redis-service
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: redis-secret
              key: password
        command:
        - /bin/bash
        - -c
        - |
          echo '{"job": "kubernetes", "status": "started"}' | \
            /app/claude_hook_redis.sh notification job-$HOSTNAME
```

## Automation Examples

### Automated Testing

```bash
#!/bin/bash
# Run tests and send results to Redis

run_tests_with_hooks() {
  local session_id="test-$(date +%s)"
  local test_output
  local test_status
  
  # Pre-test hook
  echo '{"phase": "pre_test", "suite": "unit_tests"}' | \
    ./claude_hook_redis.sh notification "$session_id"
  
  # Run tests
  test_output=$(pytest --json-report --json-report-file=/tmp/report.json 2>&1)
  test_status=$?
  
  # Parse and send results
  if [ -f /tmp/report.json ]; then
    jq '{
      tool_name: "pytest",
      tool_response: {
        success: (.exitcode == 0),
        tests_run: .summary.total,
        tests_passed: .summary.passed,
        tests_failed: .summary.failed,
        duration: .duration
      }
    }' /tmp/report.json | \
      ./claude_hook_redis.sh post_tool_use "$session_id"
  fi
  
  return $test_status
}
```

### Log Processing

```bash
#!/bin/bash
# Process Claude Code logs and send to Redis

tail -F ~/.claude/logs/claude.log | while IFS= read -r line; do
  # Parse log line
  if [[ "$line" =~ ERROR ]]; then
    echo "{
      \"level\": \"error\",
      \"message\": \"$(echo "$line" | sed 's/"/\\"/g')\"
    }" | ./claude_hook_redis.sh notification "log-monitor"
  fi
done
```

### Scheduled Tasks

```bash
#!/bin/bash
# Cron job for regular hook sending

# Add to crontab:
# */5 * * * * /path/to/scheduled_hook.sh

send_scheduled_hook() {
  local session_id="scheduled-$(date +%Y%m%d)"
  
  echo '{
    "type": "scheduled",
    "interval": "5_minutes",
    "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
    "stats": {
      "sessions_today": '$(ls ~/.claude/sessions/ 2>/dev/null | wc -l)',
      "hooks_sent": '$(grep -c "Successfully published" ~/.claude/logs/hooks.log 2>/dev/null || echo 0)'
    }
  }' | ./claude_hook_redis.sh notification "$session_id"
}

send_scheduled_hook
```

## Best Practices

1. **Session Management**
   - Use consistent session IDs across related hooks
   - Include timestamp in session ID for uniqueness
   - Store session ID in environment variable for reuse

2. **Error Handling**
   - Always check return codes
   - Implement retry logic for network failures
   - Log errors for debugging

3. **Performance**
   - Batch similar hooks when possible
   - Use async processing for non-critical hooks
   - Implement rate limiting to avoid overwhelming Redis

4. **Security**
   - Never include sensitive data in hooks
   - Use environment variables for credentials
   - Validate and sanitize all input data

5. **Monitoring**
   - Track hook success/failure rates
   - Monitor Redis channel for anomalies
   - Set up alerts for critical failures