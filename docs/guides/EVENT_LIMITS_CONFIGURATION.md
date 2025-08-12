# Event Limits Configuration Guide

## Overview

The Claude Hooks Dashboard provides configurable event limits to optimize performance based on your device capabilities and monitoring needs. This guide explains how to configure these limits and understand their impact.

## Event Limit Types

### 1. **Memory Cache Limit**
- **Purpose**: Controls how many events are kept in the app's memory
- **Default**: 1,000 events
- **Range**: 50 - 5,000 events
- **Impact**: Higher values use more RAM but provide more historical data

### 2. **Display Limit**
- **Purpose**: Controls how many events are shown in the UI at once
- **Default**: 100 events
- **Range**: 25 - 500 events
- **Impact**: Lower values improve scrolling performance

### 3. **Redis Storage TTL**
- **Purpose**: How long events are stored in Redis
- **Default**: 24 hours (86,400 seconds)
- **Configurable**: Via `REDIS_KEY_TTL` environment variable

## Configuration Methods

### Method 1: Environment Variables

Set these before launching the app:

```bash
# Set maximum events in memory cache
export MAX_MEMORY_EVENTS=2000

# Set maximum events displayed in UI
export MAX_DISPLAY_EVENTS=150

# Set Redis storage TTL (in seconds)
export REDIS_KEY_TTL=43200  # 12 hours
```

### Method 2: In-App Settings (Coming Soon)

Navigate to Settings → Event Limits to adjust:
- Memory Cache Size slider
- Display Limit slider
- View suggested limits based on device RAM

### Method 3: SharedPreferences (Programmatic)

The app stores user preferences that persist across launches:
- `max_memory_events`: Memory cache size
- `max_display_events`: UI display limit

## Recommended Configurations

### Light Monitoring (Low-end Devices)
```bash
export MAX_MEMORY_EVENTS=500
export MAX_DISPLAY_EVENTS=50
```
- **RAM Usage**: ~5MB
- **Best for**: Devices with < 2GB RAM
- **Use case**: Occasional monitoring, battery savings

### Standard Monitoring (Default)
```bash
export MAX_MEMORY_EVENTS=1000  # Default
export MAX_DISPLAY_EVENTS=100   # Default
```
- **RAM Usage**: ~10MB
- **Best for**: Most modern devices (2-4GB RAM)
- **Use case**: Regular monitoring, balanced performance

### Heavy Monitoring (High-end Devices)
```bash
export MAX_MEMORY_EVENTS=2000
export MAX_DISPLAY_EVENTS=200
```
- **RAM Usage**: ~20MB
- **Best for**: Devices with > 4GB RAM
- **Use case**: Intensive debugging, detailed analysis

### Long-term Analysis
```bash
export MAX_MEMORY_EVENTS=3000
export MAX_DISPLAY_EVENTS=100
export REDIS_KEY_TTL=604800  # 7 days
```
- **RAM Usage**: ~30MB
- **Best for**: Development/debugging environments
- **Use case**: Historical analysis, pattern detection

## Memory Management Features

### Automatic Memory Pressure Handling

The app automatically adjusts cache size when memory pressure is detected:

| Memory Usage | Action | Cache Adjustment |
|-------------|--------|------------------|
| > 85% | Critical | Reduce to 50% of current |
| > 70% | Moderate | Reduce to 75% of current |
| < 60% | Normal | Gradually restore original |

### Auto-cleanup Mechanisms

1. **30-minute cleanup**: Events older than 30 minutes are automatically removed
2. **FIFO queue**: When limit reached, oldest events are removed first
3. **Redis TTL**: Events expire from Redis after configured TTL

## Performance Impact

### Memory Cache Size Impact

| Size | RAM Usage | Scroll Performance | History Available |
|------|-----------|-------------------|-------------------|
| 50-250 | 1-3MB | Excellent | 5-30 minutes |
| 250-500 | 3-5MB | Excellent | 30-60 minutes |
| 500-1000 | 5-10MB | Good | 1-2 hours |
| 1000-2000 | 10-20MB | Good | 2-4 hours |
| 2000-5000 | 20-50MB | Moderate | 4-10 hours |

### Display Limit Impact

| Limit | UI Responsiveness | Initial Load Time | Scroll Smoothness |
|-------|------------------|-------------------|-------------------|
| 25-50 | Excellent | < 100ms | Very smooth |
| 50-100 | Excellent | 100-200ms | Smooth |
| 100-200 | Good | 200-400ms | Good |
| 200-500 | Moderate | 400-1000ms | May lag on older devices |

## Monitoring Memory Usage

### Check Current Configuration

The app logs current limits on startup:
```
D/HookDataRepository: Max memory events: 1000, Display limit: 100
```

### Memory Pressure Indicators

Watch for these log messages:
```
W/HookDataRepository: High memory pressure detected (87%), reducing event cache
D/HookDataRepository: Moderate memory pressure detected (72%), adjusting cache
D/HookDataRepository: Memory pressure relieved (58%), restoring cache size
```

### Visual Indicators (Future)

- Settings screen shows current memory usage
- Dashboard shows cache status indicator
- Warning when memory pressure active

## Best Practices

1. **Start with defaults**: The 1000/100 configuration works well for most users
2. **Monitor performance**: Check logs for memory pressure warnings
3. **Adjust gradually**: Change limits by 25-50% at a time
4. **Consider use case**:
   - Quick checks: Lower limits (500/50)
   - Active debugging: Higher limits (2000/150)
   - Long sessions: Moderate limits with longer Redis TTL
5. **Device-specific**:
   - Older devices: Prioritize lower limits
   - Tablets: Can handle higher display limits
   - Development devices: Maximum limits acceptable

## Troubleshooting

### App Crashes or Freezes
- Reduce `MAX_MEMORY_EVENTS` to 500
- Reduce `MAX_DISPLAY_EVENTS` to 50
- Check device available RAM

### Missing Recent Events
- Increase `MAX_MEMORY_EVENTS`
- Check 30-minute auto-cleanup isn't too aggressive
- Verify Redis connection is stable

### Slow Scrolling
- Reduce `MAX_DISPLAY_EVENTS`
- Check if memory pressure is active
- Consider device capabilities

### Events Disappearing Too Quickly
- Increase `REDIS_KEY_TTL` for longer storage
- Increase `MAX_MEMORY_EVENTS` for more in-app history
- Disable or adjust auto-cleanup if needed

## Future Enhancements

- [ ] Visual memory usage indicator in UI
- [ ] Automatic device profiling for optimal limits
- [ ] Per-session limit configuration
- [ ] Export settings profiles
- [ ] Cloud sync for settings across devices

This configuration system ensures optimal performance across all device types while maintaining flexibility for different monitoring needs.