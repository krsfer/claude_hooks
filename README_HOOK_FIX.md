# Claude Code Hook Enhancement Solution

## Problem Summary
The Claude Hooks Dashboard Android app displays "unknown" for tool names and "null" for user prompts because the Claude Code hook system is not properly extracting this information.

## Solution Components

### 1. Enhanced Payload Processor (`enhance_hook_payload.sh`)
This script intelligently extracts tool names from hook payloads by:
- Checking the `tool_name` field first
- Inferring tool names from `tool_input` patterns
- Extracting command names from Bash tool usage
- Properly handling user prompts and notifications

### 2. Hook Wrapper (`claude_hook_redis_wrapper.sh`)
This wrapper script:
- Intercepts hook payloads from Claude Code
- Enhances them using the payload processor
- Forwards enhanced payloads to the original Redis hook script

## Installation Instructions

### Option 1: Replace the Claude Code Hook (Recommended)

1. **Backup your current hook script:**
   ```bash
   cp /path/to/your/claude_hook_redis.sh /path/to/your/claude_hook_redis.sh.backup
   ```

2. **Configure Claude Code to use the wrapper:**
   
   In your Claude Code settings (`.claude/settings.local.json` or global settings), configure hooks to use the wrapper:
   
   ```json
   {
     "hooks": {
       "pre_tool_use": "/Users/chris/dev/src/android/claude_hooks/claude_hook_redis_wrapper.sh pre_tool_use claude-session-$(date +%s)",
       "post_tool_use": "/Users/chris/dev/src/android/claude_hooks/claude_hook_redis_wrapper.sh post_tool_use claude-session-$(date +%s)",
       "user_prompt_submit": "/Users/chris/dev/src/android/claude_hooks/claude_hook_redis_wrapper.sh user_prompt_submit claude-session-$(date +%s)",
       "notification": "/Users/chris/dev/src/android/claude_hooks/claude_hook_redis_wrapper.sh notification claude-session-$(date +%s)"
     }
   }
   ```

### Option 2: Create an Alias

Add to your shell configuration (`~/.bashrc` or `~/.zshrc`):

```bash
alias claude-hook-enhanced='/Users/chris/dev/src/android/claude_hooks/claude_hook_redis_wrapper.sh'
```

Then use `claude-hook-enhanced` instead of the original hook script.

### Option 3: Symlink Replacement

```bash
# Backup original
mv /path/to/claude_hook_redis.sh /path/to/claude_hook_redis_original.sh

# Create symlink to wrapper
ln -s /Users/chris/dev/src/android/claude_hooks/claude_hook_redis_wrapper.sh /path/to/claude_hook_redis.sh
```

## Testing the Enhancement

### Test Individual Tool Detection

```bash
# Test Bash command detection
echo '{"tool_input": {"command": "git status"}}' | \
  ./enhance_hook_payload.sh pre_tool_use

# Test Read file detection
echo '{"tool_input": {"file_path": "/path/to/file"}}' | \
  ./enhance_hook_payload.sh pre_tool_use

# Test Edit detection
echo '{"tool_input": {"old_string": "foo", "new_string": "bar"}}' | \
  ./enhance_hook_payload.sh pre_tool_use

# Test Grep detection
echo '{"tool_input": {"pattern": "search.*pattern"}}' | \
  ./enhance_hook_payload.sh pre_tool_use
```

### Test Complete Pipeline

```bash
# Set Redis environment
source ~/.claude/redis_config.env

# Test with enhanced wrapper
echo '{"tool_input": {"command": "ls -la"}}' | \
  DEBUG=1 ./claude_hook_redis_wrapper.sh pre_tool_use test-session-001
```

## What This Fixes

✅ **Tool Names**: Properly extracts and displays actual tool names (Read, Edit, Bash, Grep, etc.) instead of "unknown"

✅ **Command Names**: For Bash tools, extracts the actual command (e.g., "git", "ls", "cat")

✅ **User Prompts**: Captures and forwards user prompt text for user_prompt_submit hooks

✅ **Notifications**: Properly handles notification types and messages

✅ **Tool Input Preview**: Provides meaningful preview of tool input parameters

## Verification in Android App

After implementing the wrapper, the Android Hooks Dashboard should display:
- **"Tool Use: Edit"** instead of "Tool Use: unknown"
- **"Tool Use: git"** instead of "Tool Use: unknown" for git commands
- **Actual user prompt text** instead of "null"
- **Proper notification messages** with correct types

## Debugging

Enable debug mode to see enhanced payloads:
```bash
export DEBUG=1
./claude_hook_redis_wrapper.sh pre_tool_use session-001 < payload.json
```

## Files Created

1. **`enhance_hook_payload.sh`** - Core payload enhancement logic
2. **`claude_hook_redis_wrapper.sh`** - Wrapper that integrates enhancement with Redis hook
3. **`claude_hook_redis_enhanced.sh`** - Alternative standalone enhanced version (optional)

## Notes

- The wrapper is backward compatible with the original hook script
- No changes needed to the Android app - it will automatically display enhanced data
- The enhancement works by pattern matching on tool_input fields
- Falls back gracefully if patterns don't match

## Support

If tool names are still showing as "unknown":
1. Check that the wrapper has execute permissions: `chmod +x *.sh`
2. Verify Redis environment variables are set: `source ~/.claude/redis_config.env`
3. Enable DEBUG mode to see what's being processed
4. Check Android app logs: `adb logcat | grep -E "tool_name|HookDataMapper"`