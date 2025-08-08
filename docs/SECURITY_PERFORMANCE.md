# Security and Performance Guide

Best practices for securing and optimizing the Claude Code Redis hook integration system.

## Table of Contents
- [Security Best Practices](#security-best-practices)
- [Authentication and Authorization](#authentication-and-authorization)
- [Data Protection](#data-protection)
- [Network Security](#network-security)
- [Performance Optimization](#performance-optimization)
- [Scaling Strategies](#scaling-strategies)
- [Resource Management](#resource-management)

## Security Best Practices

### Credential Management

#### Never Store Credentials in Code

```bash
# BAD - Never do this
REDIS_PASSWORD="my-secret-password"  # DON'T DO THIS

# GOOD - Use environment variables
export REDIS_PASSWORD="${REDIS_PASSWORD}"  # From environment

# BETTER - Use secret management
export REDIS_PASSWORD="$(vault kv get -field=password secret/redis)"

# BEST - Use cloud provider secrets
export REDIS_PASSWORD="$(aws secretsmanager get-secret-value --secret-id redis-password --query SecretString --output text)"
```

#### Secure Configuration Files

```bash
# Set proper permissions
chmod 600 ~/.claude/redis_config.env  # Owner read/write only
chmod 700 ~/.claude                   # Owner only directory access

# Encrypt sensitive files at rest
# Encrypt
openssl enc -aes-256-cbc -salt -in redis_config.env -out redis_config.env.enc

# Decrypt when needed
openssl enc -aes-256-cbc -d -in redis_config.env.enc -out redis_config.env
```

#### Rotate Credentials Regularly

```bash
#!/bin/bash
# Credential rotation script

rotate_redis_password() {
    local new_password=$(openssl rand -base64 32)
    
    # Update Redis
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
        --pass "$REDIS_PASSWORD" \
        CONFIG SET requirepass "$new_password"
    
    # Update configuration
    sed -i.bak "s/REDIS_PASSWORD=.*/REDIS_PASSWORD=\"$new_password\"/" ~/.claude/redis_config.env
    
    # Notify services
    echo "Password rotated at $(date)" >> ~/.claude/logs/rotation.log
}
```

### Input Validation and Sanitization

#### Validate Hook Types

```bash
#!/bin/bash
# Strict hook type validation

validate_hook_type_strict() {
    local hook_type="$1"
    
    # Whitelist approach
    case "$hook_type" in
        session_start|user_prompt_submit|pre_tool_use|post_tool_use|\
        notification|stop_hook|sub_agent_stop_hook|pre_compact)
            return 0
            ;;
        *)
            log_security_event "Invalid hook type attempted: $hook_type"
            return 1
            ;;
    esac
}
```

#### Sanitize JSON Input

```bash
#!/bin/bash
# JSON input sanitization

sanitize_json() {
    local input=$(cat)
    
    # Remove potentially dangerous content
    local sanitized=$(echo "$input" | \
        sed -E 's/<script[^>]*>.*<\/script>//gi' | \
        sed -E 's/javascript://gi' | \
        sed -E 's/on[a-z]+\s*=\s*"[^"]*"//gi')
    
    # Validate JSON structure
    if echo "$sanitized" | jq empty 2>/dev/null; then
        echo "$sanitized"
    else
        log_security_event "Invalid JSON structure detected"
        echo "{}"
    fi
}
```

#### Payload Size Limits

```bash
#!/bin/bash
# Enforce payload size limits

check_payload_size() {
    local max_size=${MAX_PAYLOAD_SIZE:-1048576}  # 1MB default
    local payload=$(cat)
    local size=${#payload}
    
    if [ $size -gt $max_size ]; then
        log_security_event "Payload too large: $size bytes"
        return 1
    fi
    
    echo "$payload"
}
```

### Secure Communication

#### TLS Configuration

```bash
# Production TLS settings
export REDIS_TLS="true"
export REDIS_TLS_CERT="/path/to/client.crt"
export REDIS_TLS_KEY="/path/to/client.key"
export REDIS_TLS_CA="/path/to/ca.crt"
export REDIS_TLS_SKIP_VERIFY="false"  # Never skip in production

# Test TLS connection
redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
    --tls \
    --cert "$REDIS_TLS_CERT" \
    --key "$REDIS_TLS_KEY" \
    --cacert "$REDIS_TLS_CA" \
    ping
```

#### SSH Tunneling

```bash
#!/bin/bash
# Create secure SSH tunnel to Redis

create_redis_tunnel() {
    local jump_host="bastion.example.com"
    local redis_host="redis.internal.example.com"
    local local_port=6380
    local remote_port=6379
    
    # Create tunnel
    ssh -f -N -L ${local_port}:${redis_host}:${remote_port} ${jump_host}
    
    # Update configuration to use localhost
    export REDIS_HOST="localhost"
    export REDIS_PORT="$local_port"
    export REDIS_TLS="false"  # Already encrypted via SSH
}
```

## Authentication and Authorization

### Redis ACL Configuration

```redis
# Redis ACL configuration for Claude hooks

# Create read-write user for hook publisher
ACL SETUSER claude_publisher \
    on \
    +ping +publish +hset +zadd +hincrby \
    ~hooks:* ~hooksdata \
    >strong_password_here

# Create read-only user for monitoring
ACL SETUSER claude_monitor \
    on \
    +ping +subscribe +hget +zrange \
    ~hooks:* ~hooksdata \
    >monitor_password_here

# Save ACL configuration
ACL SAVE
```

### Multi-Factor Authentication

```bash
#!/bin/bash
# MFA for sensitive operations

require_mfa() {
    local operation="$1"
    
    echo "MFA required for: $operation"
    read -s -p "Enter MFA token: " mfa_token
    echo
    
    # Verify MFA token (example using Google Authenticator)
    if oathtool --base32 --totp "$MFA_SECRET" | grep -q "$mfa_token"; then
        return 0
    else
        log_security_event "MFA failed for operation: $operation"
        return 1
    fi
}

# Use before sensitive operations
if require_mfa "redis_password_rotation"; then
    rotate_redis_password
fi
```

## Data Protection

### Sensitive Data Filtering

```bash
#!/bin/bash
# Filter sensitive data from hooks

filter_sensitive_data() {
    local input=$(cat)
    
    # List of sensitive patterns
    local patterns=(
        's/"password":\s*"[^"]*"/"password": "[REDACTED]"/g'
        's/"api_key":\s*"[^"]*"/"api_key": "[REDACTED]"/g'
        's/"token":\s*"[^"]*"/"token": "[REDACTED]"/g'
        's/"secret":\s*"[^"]*"/"secret": "[REDACTED]"/g'
        's/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/[EMAIL]/g'
        's/\b(?:\d{4}[-\s]?){3}\d{4}\b/[CREDIT_CARD]/g'
        's/\b\d{3}-\d{2}-\d{4}\b/[SSN]/g'
    )
    
    local filtered="$input"
    for pattern in "${patterns[@]}"; do
        filtered=$(echo "$filtered" | sed -E "$pattern")
    done
    
    echo "$filtered"
}
```

### Data Encryption

```bash
#!/bin/bash
# Encrypt sensitive payloads

encrypt_payload() {
    local payload="$1"
    local key="${ENCRYPTION_KEY:-}"
    
    if [ -z "$key" ]; then
        echo "Warning: No encryption key set" >&2
        echo "$payload"
        return
    fi
    
    # Encrypt using OpenSSL
    local encrypted=$(echo "$payload" | \
        openssl enc -aes-256-cbc -a -salt -pass pass:"$key")
    
    # Return encrypted payload with metadata
    jq -n --arg data "$encrypted" '{
        encrypted: true,
        algorithm: "AES-256-CBC",
        data: $data
    }'
}

decrypt_payload() {
    local encrypted="$1"
    local key="${ENCRYPTION_KEY:-}"
    
    echo "$encrypted" | \
        openssl enc -aes-256-cbc -d -a -pass pass:"$key"
}
```

### Data Retention Policies

```bash
#!/bin/bash
# Implement data retention policies

cleanup_old_data() {
    local retention_days="${RETENTION_DAYS:-30}"
    local cutoff_timestamp=$(date -d "$retention_days days ago" +%s)
    
    # Clean old logs
    find ~/.claude/logs -name "*.log" -mtime +$retention_days -delete
    
    # Clean old Redis data
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" --pass "$REDIS_PASSWORD" eval "
        local keys = redis.call('keys', 'hooks:*')
        local deleted = 0
        for i=1,#keys do
            local timestamp = redis.call('hget', keys[i], 'timestamp')
            if timestamp and tonumber(timestamp) < $cutoff_timestamp then
                redis.call('del', keys[i])
                deleted = deleted + 1
            end
        end
        return deleted
    " 0
}

# Schedule cleanup
# Add to crontab: 0 2 * * * /path/to/cleanup_old_data.sh
```

## Network Security

### Firewall Configuration

```bash
#!/bin/bash
# Configure firewall for Redis

setup_firewall() {
    # Allow Redis from specific IPs only
    sudo ufw allow from 10.0.0.0/24 to any port 6379 comment "Redis from internal network"
    
    # Rate limiting
    sudo iptables -A INPUT -p tcp --dport 6379 -m state --state NEW -m recent --set
    sudo iptables -A INPUT -p tcp --dport 6379 -m state --state NEW -m recent --update --seconds 60 --hitcount 10 -j DROP
    
    # Save rules
    sudo netfilter-persistent save
}
```

### VPN Configuration

```bash
#!/bin/bash
# Require VPN for Redis access

check_vpn_connection() {
    # Check if VPN is connected
    if ! ip addr show | grep -q "tun0"; then
        echo "Error: VPN not connected. Redis access requires VPN connection." >&2
        exit 1
    fi
    
    # Verify VPN IP range
    local vpn_ip=$(ip addr show tun0 | grep inet | awk '{print $2}' | cut -d/ -f1)
    if [[ ! "$vpn_ip" =~ ^10\.8\. ]]; then
        echo "Error: Invalid VPN connection" >&2
        exit 1
    fi
}

# Check before Redis operations
check_vpn_connection
```

## Performance Optimization

### Connection Pooling

```bash
#!/bin/bash
# Redis connection pooling implementation

# Global connection pool
declare -A REDIS_CONNECTIONS

get_redis_connection() {
    local pool_key="${REDIS_HOST}:${REDIS_PORT}"
    
    if [[ -z "${REDIS_CONNECTIONS[$pool_key]}" ]]; then
        # Create new connection
        REDIS_CONNECTIONS[$pool_key]=$(mktemp -u /tmp/redis.XXXXXX)
        mkfifo "${REDIS_CONNECTIONS[$pool_key]}"
        
        # Start persistent connection
        redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
            --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls} \
            < "${REDIS_CONNECTIONS[$pool_key]}" &
    fi
    
    echo "${REDIS_CONNECTIONS[$pool_key]}"
}

# Use pooled connection
send_with_pool() {
    local command="$1"
    local connection=$(get_redis_connection)
    echo "$command" > "$connection"
}
```

### Batch Processing

```bash
#!/bin/bash
# Batch multiple hooks for efficiency

batch_hooks() {
    local batch_size="${BATCH_SIZE:-10}"
    local batch_timeout="${BATCH_TIMEOUT:-5}"
    local batch_file="/tmp/hook_batch_$$"
    local count=0
    
    > "$batch_file"
    
    # Collect hooks
    while IFS= read -r -t "$batch_timeout" line || [ $count -gt 0 ]; do
        if [ -n "$line" ]; then
            echo "$line" >> "$batch_file"
            ((count++))
        fi
        
        if [ $count -ge $batch_size ] || [ -z "$line" ]; then
            # Process batch
            if [ $count -gt 0 ]; then
                process_batch "$batch_file"
                > "$batch_file"
                count=0
            fi
        fi
    done
    
    rm -f "$batch_file"
}

process_batch() {
    local batch_file="$1"
    local session_id="batch-$(date +%s)"
    
    # Send all hooks in a single Redis transaction
    {
        echo "MULTI"
        while IFS= read -r hook; do
            echo "PUBLISH hooksdata '$hook'"
        done < "$batch_file"
        echo "EXEC"
    } | redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" \
        --pass "$REDIS_PASSWORD" ${REDIS_TLS:+--tls}
}
```

### Caching Strategy

```bash
#!/bin/bash
# Implement caching for frequently used data

declare -A CACHE
declare -A CACHE_TIMESTAMPS

cache_get() {
    local key="$1"
    local ttl="${2:-300}"  # 5 minutes default
    local now=$(date +%s)
    
    if [[ -n "${CACHE[$key]}" ]]; then
        local cached_time="${CACHE_TIMESTAMPS[$key]:-0}"
        if [ $((now - cached_time)) -lt $ttl ]; then
            echo "${CACHE[$key]}"
            return 0
        fi
    fi
    
    return 1
}

cache_set() {
    local key="$1"
    local value="$2"
    
    CACHE[$key]="$value"
    CACHE_TIMESTAMPS[$key]=$(date +%s)
}

# Use cache for expensive operations
get_system_context_cached() {
    local cache_key="system_context"
    
    if cached=$(cache_get "$cache_key" 600); then
        echo "$cached"
    else
        local context=$(get_system_context)
        cache_set "$cache_key" "$context"
        echo "$context"
    fi
}
```

### Async Processing

```bash
#!/bin/bash
# Asynchronous hook processing

# Queue for async processing
ASYNC_QUEUE="/tmp/claude_hooks_queue"
mkfifo "$ASYNC_QUEUE" 2>/dev/null || true

# Worker process
start_async_worker() {
    while true; do
        if read -r hook_data < "$ASYNC_QUEUE"; then
            # Process hook in background
            (
                echo "$hook_data" | ./claude_hook_redis.sh notification async-session
            ) &
        fi
    done &
    
    echo $! > /tmp/claude_hooks_worker.pid
}

# Send hook asynchronously
send_async_hook() {
    local hook_data="$1"
    echo "$hook_data" > "$ASYNC_QUEUE" &
}

# Stop worker
stop_async_worker() {
    if [ -f /tmp/claude_hooks_worker.pid ]; then
        kill $(cat /tmp/claude_hooks_worker.pid) 2>/dev/null
        rm -f /tmp/claude_hooks_worker.pid
    fi
}
```

## Scaling Strategies

### Horizontal Scaling

```yaml
# Docker Swarm configuration for scaling
version: '3.8'

services:
  claude-hooks:
    image: claude-hooks:latest
    deploy:
      replicas: 5
      update_config:
        parallelism: 2
        delay: 10s
      restart_policy:
        condition: on-failure
    environment:
      - REDIS_HOST=redis
      - REDIS_PASSWORD_FILE=/run/secrets/redis_password
    secrets:
      - redis_password
    networks:
      - claude-network

  redis:
    image: redis:alpine
    deploy:
      replicas: 1
      placement:
        constraints:
          - node.role == manager
    networks:
      - claude-network

secrets:
  redis_password:
    external: true

networks:
  claude-network:
    driver: overlay
```

### Load Balancing

```nginx
# Nginx load balancer configuration
upstream claude_hooks {
    least_conn;
    server hooks1.example.com:5000 weight=3;
    server hooks2.example.com:5000 weight=2;
    server hooks3.example.com:5000 weight=1;
    
    # Health checks
    keepalive 32;
}

server {
    listen 443 ssl;
    server_name hooks.example.com;
    
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    
    location /hook {
        proxy_pass http://claude_hooks;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        
        # Rate limiting
        limit_req zone=hooks_limit burst=10 nodelay;
        limit_req_status 429;
    }
}

# Rate limiting zone
limit_req_zone $binary_remote_addr zone=hooks_limit:10m rate=10r/s;
```

### Redis Cluster

```bash
#!/bin/bash
# Redis Cluster configuration for high availability

setup_redis_cluster() {
    # Create cluster
    redis-cli --cluster create \
        redis1.example.com:6379 \
        redis2.example.com:6379 \
        redis3.example.com:6379 \
        redis4.example.com:6379 \
        redis5.example.com:6379 \
        redis6.example.com:6379 \
        --cluster-replicas 1 \
        --cluster-yes
    
    # Configure Claude hooks for cluster
    export REDIS_CLUSTER_NODES="redis1.example.com:6379,redis2.example.com:6379,redis3.example.com:6379"
    export REDIS_CLUSTER_ENABLED="true"
}

# Modified Redis connection for cluster
connect_redis_cluster() {
    redis-cli -c \
        -h "${REDIS_CLUSTER_NODES%%,*}" \
        --pass "$REDIS_PASSWORD" \
        ${REDIS_TLS:+--tls}
}
```

## Resource Management

### Memory Management

```bash
#!/bin/bash
# Monitor and manage memory usage

monitor_memory() {
    local max_memory="${MAX_MEMORY_MB:-100}"
    
    while true; do
        # Check script memory usage
        local pid=$$
        local mem_usage=$(ps -o rss= -p $pid | awk '{print int($1/1024)}')
        
        if [ $mem_usage -gt $max_memory ]; then
            log_warning "High memory usage: ${mem_usage}MB"
            
            # Clear caches
            unset CACHE
            unset CACHE_TIMESTAMPS
            declare -A CACHE
            declare -A CACHE_TIMESTAMPS
            
            # Force garbage collection (if applicable)
            sync && echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true
        fi
        
        sleep 60
    done &
}
```

### CPU Optimization

```bash
#!/bin/bash
# CPU usage optimization

# Process priority
renice -n 10 $$ 2>/dev/null || true

# CPU affinity (bind to specific cores)
taskset -cp 0,1 $$ 2>/dev/null || true

# Limit concurrent operations
MAX_CONCURRENT="${MAX_CONCURRENT:-5}"
JOB_COUNT=0

wait_for_slot() {
    while [ $(jobs -r | wc -l) -ge $MAX_CONCURRENT ]; do
        sleep 0.1
    done
}

# Use before spawning background jobs
wait_for_slot
process_hook &
```

### Disk I/O Optimization

```bash
#!/bin/bash
# Optimize disk I/O

# Use tmpfs for temporary files
TEMP_DIR="/dev/shm/claude_hooks"
mkdir -p "$TEMP_DIR"

# Batch write operations
batch_write() {
    local buffer=""
    local buffer_size=0
    local max_buffer=4096
    
    while IFS= read -r line; do
        buffer="${buffer}${line}\n"
        buffer_size=$((buffer_size + ${#line}))
        
        if [ $buffer_size -ge $max_buffer ]; then
            echo -e "$buffer" >> "$CLAUDE_HOOKS_LOG"
            buffer=""
            buffer_size=0
        fi
    done
    
    # Flush remaining buffer
    if [ -n "$buffer" ]; then
        echo -e "$buffer" >> "$CLAUDE_HOOKS_LOG"
    fi
}

# Use O_DIRECT for large files (Linux)
dd if=/dev/zero of=largefile bs=1M count=100 oflag=direct 2>/dev/null || true
```

## Monitoring and Alerting

### Performance Metrics

```bash
#!/bin/bash
# Collect and report performance metrics

collect_metrics() {
    local start_time=$(date +%s%N)
    
    # Your operation here
    echo '{"test": true}' | ./claude_hook_redis.sh notification test-metrics
    
    local end_time=$(date +%s%N)
    local duration_ms=$(( (end_time - start_time) / 1000000 ))
    
    # Report metrics
    echo "{
        \"metric\": \"hook_duration\",
        \"value\": $duration_ms,
        \"unit\": \"ms\",
        \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
    }" | send_to_monitoring_system
}

send_to_monitoring_system() {
    # Send to your monitoring system (e.g., Prometheus, DataDog, CloudWatch)
    curl -X POST http://metrics.example.com/api/v1/metrics \
        -H "Content-Type: application/json" \
        -d @-
}
```

### Alert Rules

```yaml
# Prometheus alert rules
groups:
  - name: claude_hooks_alerts
    interval: 30s
    rules:
      - alert: HighHookLatency
        expr: histogram_quantile(0.95, rate(claude_hook_duration_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High hook processing latency"
          description: "95th percentile latency is {{ $value }} seconds"
      
      - alert: HookProcessingErrors
        expr: rate(claude_hooks_errors_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate in hook processing"
          description: "Error rate is {{ $value }} per second"
      
      - alert: RedisConnectionFailure
        expr: up{job="redis"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Redis connection lost"
          description: "Cannot connect to Redis server"
```

## Best Practices Summary

### Security Checklist
- [ ] Use environment variables for credentials
- [ ] Enable TLS for Redis connections
- [ ] Implement input validation and sanitization
- [ ] Set up proper file permissions
- [ ] Enable audit logging
- [ ] Implement rate limiting
- [ ] Use VPN or SSH tunneling for remote access
- [ ] Regular credential rotation
- [ ] Data encryption at rest and in transit

### Performance Checklist
- [ ] Implement connection pooling
- [ ] Use batch processing for multiple hooks
- [ ] Enable async processing for non-critical hooks
- [ ] Implement caching for expensive operations
- [ ] Monitor resource usage
- [ ] Set up horizontal scaling
- [ ] Configure load balancing
- [ ] Optimize disk I/O
- [ ] Regular performance testing

### Operational Checklist
- [ ] Set up monitoring and alerting
- [ ] Implement data retention policies
- [ ] Regular backup procedures
- [ ] Disaster recovery plan
- [ ] Documentation up to date
- [ ] Team training completed
- [ ] Incident response procedures
- [ ] Regular security audits