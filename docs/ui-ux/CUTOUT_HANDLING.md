# Camera Cutout Handling in Claude Hooks Dashboard

## Overview
The Claude Hooks Dashboard now includes comprehensive camera cutout handling to ensure optimal display on devices with notches, punch holes, and other display cutouts.

## Implementation Levels

### 1. MainActivity Configuration
- **`LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`**: Allows content to extend into cutout areas
- **Cutout Detection**: Logs cutout information for debugging
- **Edge-to-edge**: Window extends behind system bars
- **Screen Awake**: Keeps screen on for monitoring sessions

### 2. Theme Configuration
- **Transparent System Bars**: Status/navigation bars are transparent
- **Always Cutout Mode**: Aggressive cutout utilization in theme
- **No Action Bar**: Prevents conflicts with cutout areas

### 3. Compose Layout Handling
- **CutoutUtils**: Utility functions for cutout-aware layouts
- **Safe Area Padding**: Automatic padding to avoid cutout areas
- **Cutout Detection**: Runtime cutout presence detection
- **Adaptive Layout**: UI adapts based on cutout information

## Cutout Strategies

### Strategy 1: Avoid Cutouts (Recommended for Critical UI)
```kotlin
// Use for important content like buttons, text, form inputs
@Composable
fun CriticalContent() {
    Box(
        modifier = Modifier.cutoutAware()
    ) {
        // Content that must not be obscured
        Text("Important information")
    }
}
```

### Strategy 2: Embrace Cutouts (For Background/Decorative Content)
```kotlin
// Use for backgrounds, status indicators, decorative elements
@Composable
fun BackgroundContent() {
    Box(
        modifier = Modifier.fillMaxSize() // No cutout padding
    ) {
        // Content that can be partially obscured
        Image(painter = backgroundPainter, ...)
    }
}
```

### Strategy 3: Adaptive Layout (Smart Adjustment)
```kotlin
@Composable
fun AdaptiveContent() {
    val hasDisplayCutout = hasCutout()
    val cutoutTop = getCutoutTopInset()
    
    Column {
        if (hasDisplayCutout) {
            Spacer(modifier = Modifier.height(cutoutTop))
        }
        // Content with conditional spacing
    }
}
```

## Device Types Supported

### Notch Devices
- iPhone X-style notches at top center
- **Strategy**: Content flows around notch
- **Example**: Essential content avoids notch area

### Punch Hole Devices  
- Small circular cutouts (usually top corner)
- **Strategy**: Minimal layout adjustment needed
- **Example**: Status indicators can coexist

### Edge-to-Edge Cutouts
- Cutouts that extend to screen edges
- **Strategy**: More aggressive padding required
- **Example**: Full-width content needs careful positioning

### Dual Cutouts
- Multiple cutout areas (rare)
- **Strategy**: Complex layout calculations
- **Example**: Content in safe central area

## Dashboard-Specific Optimizations

### Cutout-Aware Toolbar (New Implementation)
- **Dual-Layout System**: Automatically switches between cutout and standard layouts
- **Icon Distribution**: Icons positioned strategically around cutout areas
- **Title Repositioning**: App title moved below cutout in optimized layout
- **Space Utilization**: Maximum use of available screen real estate

#### Cutout-Optimized Layout Features:
- **Top Row**: Icons distributed on left/right sides of cutout
- **Bottom Row**: Centered title with "Cutout-optimized layout" indicator
- **Smart Grouping**: Connection indicators on left, action buttons on right
- **Vertical Space**: Better use of height by stacking elements

#### Standard Layout (Non-Cutout Devices):
- **Traditional AppBar**: Uses Material 3 CenterAlignedTopAppBar
- **Single Row**: All elements in standard horizontal arrangement
- **Consistent Behavior**: Same functionality with conventional layout

### Implementation Architecture

#### CutoutAwareToolbar Component
```kotlin
@Composable
fun CutoutAwareToolbar(
    // Connection status and data
    connectionStatus: ConnectionStatus,
    lastUpdateTime: Instant?,
    useTestData: Boolean,
    
    // Action callbacks
    onTestDataToggle: () -> Unit,
    onReconnect: () -> Unit,
    onPerformanceToggle: () -> Unit,
    // ... other callbacks
    
    // UI state management
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
)
```

#### Layout Decision Logic
1. **Cutout Detection**: Uses `getCutoutInfo()` to detect device capabilities
2. **Layout Selection**: Automatically chooses between optimized and standard layouts
3. **Dynamic Adaptation**: Responds to cutout presence without user intervention

### Content Areas
- **Stats Cards**: Horizontal scroll avoids cutout interference
- **Event List**: Proper padding prevents content hiding
- **Filter Chips**: Safe positioning in cutout-free zones

### Monitoring Benefits
- **Maximum Screen Usage**: Every pixel utilized for data display
- **Distraction-Free**: Content never hidden behind cutouts
- **Professional Appearance**: Polished layout on all devices

## Testing Recommendations

### Emulator Testing
1. Use Android Studio's cutout simulation
2. Test different cutout types (notch, punch hole, corner)
3. Verify content positioning in all orientations

### Physical Device Testing
1. Test on devices with actual cutouts
2. Verify edge cases (rotation, keyboard, system UI)
3. Check accessibility with screen readers

### Debug Information
- **Logcat**: Check for cutout detection logs
- **Visual Indicators**: Cutout status shown in title bar
- **Layout Inspector**: Verify padding calculations

## Future Enhancements

### Planned Improvements
- **Landscape Optimization**: Better handling for landscape cutouts
- **Foldable Support**: Adaptive layout for foldable devices
- **Dynamic Adjustment**: Real-time cutout detection updates
- **User Preferences**: Allow users to choose cutout handling strategy

This implementation ensures the Claude Hooks Dashboard provides an optimal viewing experience across all Android devices with display cutouts.