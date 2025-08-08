#!/bin/bash
# Export Redis environment variables
# Source this file in your shell: source export_redis_vars.sh

# Update these with your actual Redis credentials
export REDIS_HOST="your-redis-host.com"  # Replace with actual host
export REDIS_PORT="18773"                 # Your Redis port
export REDIS_PASSWORD="your-password"     # Replace with actual password
export REDIS_TLS="true"                   # Set to true if using TLS

echo "Redis environment variables set:"
echo "  REDIS_HOST: $REDIS_HOST"
echo "  REDIS_PORT: $REDIS_PORT"  
echo "  REDIS_PASSWORD: [HIDDEN]"
echo "  REDIS_TLS: $REDIS_TLS"