# Icon Gradient Options - Claude Hooks Dashboard

The Claude Hooks Dashboard app icon has been reverted to the original hook design with modern gradient background options.

## Current Configuration

**Icon Design**: Original fishing hook with eye, shaft, barb, and fishing line
**Colors**: Orange/gold hook (`#FF6B35`, `#FFD700`) with shadow (`#E55B2B`)
**Background**: Modern gradient with glassmorphism overlay effects

## Available Gradient Options

### 1. Cosmic Purple-Blue (Currently Active) ⭐ Recommended
- **Colors**: Soft Blue (#667eea) → Deep Purple (#764ba2)
- **Style**: Professional tech aesthetic
- **Best for**: Developer tools, professional apps
- **Contrast**: Excellent with orange hook elements

### 2. Sunset Gradient
- **Colors**: Soft Pink (#ff9a9e) → Pale Rose (#fecfef)
- **Style**: Warm and approachable
- **Best for**: Consumer apps, friendly tools
- **Contrast**: Good with orange elements

### 3. Ocean Wave
- **Colors**: Deep Blue (#2193b0) → Light Cyan (#6dd5ed)
- **Style**: Cool and modern
- **Best for**: Data visualization, analytics
- **Contrast**: Excellent with orange hook

### 4. Forest Mist
- **Colors**: Deep Teal (#134e5e) → Fresh Green (#71b280)
- **Style**: Natural and professional
- **Best for**: Monitoring tools, system apps
- **Contrast**: Good with orange elements

### 5. Aurora
- **Colors**: Rose Pink (#ee9ca7) → Pale Rose (#ffdde1)
- **Style**: Vibrant and modern
- **Best for**: Creative apps, modern tools
- **Contrast**: Good with gold highlights

### 6. Tech Gradient
- **Colors**: Electric Blue (#4facfe) → Cyan (#00f2fe)
- **Style**: High-tech, futuristic
- **Best for**: Developer tools, tech products
- **Contrast**: Excellent with orange elements

## How to Switch Gradients

1. **Edit Colors File**: Open `/app/src/main/res/values/colors.xml`

2. **Update Active Colors**: Change these two lines (around line 43-44):
   ```xml
   <color name="background_gradient_start">@color/bg_[option]_start</color>
   <color name="background_gradient_end">@color/bg_[option]_end</color>
   ```

3. **Replace `[option]` with one of**:
   - `cosmic` (current/recommended)
   - `sunset`
   - `ocean`
   - `forest`
   - `aurora`
   - `tech`

4. **Rebuild and Install**:
   ```bash
   ./gradlew clean assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Example: Switching to Ocean Wave

```xml
<!-- Change these lines in colors.xml -->
<color name="background_gradient_start">@color/bg_ocean_start</color>
<color name="background_gradient_end">@color/bg_ocean_end</color>
```

## Technical Details

- **Gradient Direction**: Diagonal (20,20) to (88,88) for modern appeal
- **Overlay Effects**: 
  - Flowing wave pattern for organic depth (8% white opacity)
  - Radial highlight for premium feel (12% white opacity)
  - Top linear highlight for glossy effect (8% white opacity)
- **Adaptive Icon Support**: Full compatibility with all Android launcher shapes
- **Monochrome Version**: Available for Android 13+ themed icons

## Color Psychology

- **Blue gradients** (Cosmic, Ocean, Tech): Trust, technology, professionalism
- **Purple gradients** (Cosmic): Creativity, innovation, premium feel
- **Green gradients** (Forest): Growth, stability, monitoring
- **Pink/Rose gradients** (Sunset, Aurora): Approachability, modern, creative
- **Cyan gradients** (Tech, Ocean): Digital, modern, high-tech

The current **Cosmic Purple-Blue** gradient is recommended as it provides the best balance of professionalism, modern appeal, and excellent contrast with the orange hook design.