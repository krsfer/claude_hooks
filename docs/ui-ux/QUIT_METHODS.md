# Ways to Quit the Claude Hooks Dashboard App

The full-screen Claude Hooks Dashboard provides multiple ways to exit the application completely:

## 1. 🔴 **Quit Button (Always Visible)**
- **Location**: Top-right corner of the app bar
- **Icon**: Red exit door icon
- **Action**: Single tap to quit immediately
- **Visibility**: Always present, regardless of connection status

## 2. ⬅️ **Double Back Button**
- **Gesture**: Press the device's back button twice within 2 seconds
- **First Press**: Prepares for exit (resets timer)
- **Second Press**: Exits the app completely
- **Timeout**: 2 seconds between presses

## 3. 📱 **Long-Press Menu**
- **Gesture**: Long-press on the app title "Claude Hooks Dashboard"
- **Menu Options**:
  - "Quit Application" - Exits the app
  - "About" - Shows version information
- **Visual Hint**: "Long-press for menu" text appears below the title

## 4. 🎯 **System Methods (Android Standard)**
- **Recent Apps**: Swipe up from home button area, then swipe the app away
- **App Switcher**: Use the recent apps button and swipe to dismiss
- **Force Stop**: Settings → Apps → Claude Hooks Dashboard → Force Stop

## Technical Implementation

### Exit Method Used
All quit methods call `finishAndRemoveTask()` which:
- Completely terminates the activity
- Removes the app from the recent apps list
- Cleans up all resources
- Ensures complete shutdown

### Full-Screen Considerations
Even in full-screen immersive mode:
- The quit button remains accessible
- Back button gestures work normally
- Long-press interactions function properly
- System bars can be swiped to reveal for standard Android navigation

## Troubleshooting

### If Quit Button Not Visible
1. **Swipe down** from the top to temporarily show system bars
2. **Look for the red exit icon** in the top-right corner
3. **Use alternative methods** like double-back or long-press

### If App Won't Quit
1. Use Android's force stop in Settings
2. Clear the app from recent apps
3. Restart the device if necessary

## User Experience Notes

- **No Confirmation Dialog**: All quit methods exit immediately for quick workflow
- **State Not Saved**: Exiting loses current filter/view state
- **Background Service**: If using background service mode, service continues after app exit
- **Quick Restart**: App can be immediately relaunched from launcher

This multi-method approach ensures users can always exit the app conveniently, even in full-screen immersive mode.