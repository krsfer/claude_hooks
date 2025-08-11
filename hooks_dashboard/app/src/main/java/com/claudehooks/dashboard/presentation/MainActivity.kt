package com.claudehooks.dashboard.presentation

import android.os.Build
import android.os.Bundle
import android.view.DisplayCutout
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.claudehooks.dashboard.data.DataProvider
import com.claudehooks.dashboard.presentation.screens.DashboardScreen
import com.claudehooks.dashboard.presentation.screens.SettingsScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.claudehooks.dashboard.presentation.theme.HooksDashboardTheme

class MainActivity : ComponentActivity() {
    private var repository: com.claudehooks.dashboard.data.repository.HookDataRepository? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure full-screen immersive mode
        setupFullScreenMode()
        enableEdgeToEdge()
        
        // Initialize repository
        repository = DataProvider.getRepository(this)
        
        setContent {
            HooksDashboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HooksDashboardApp(repository)
                }
            }
        }
    }
    
    private fun setupFullScreenMode() {
        // Enhanced display cutout handling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use ALWAYS mode for dashboard apps that can benefit from full screen real estate
            window.attributes.layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        
        // Configure window for immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set up system bars behavior
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Hide system bars by default
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        // Configure behavior: swipe to show, auto-hide after delay
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Keep screen on for monitoring
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Log cutout information for debugging
        logCutoutInformation()
    }
    
    private fun logCutoutInformation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                val cutout = insets.displayCutout
                if (cutout != null) {
                    android.util.Log.d("MainActivity", """
                        Display Cutout detected:
                        - Safe inset top: ${cutout.safeInsetTop}
                        - Safe inset bottom: ${cutout.safeInsetBottom}
                        - Safe inset left: ${cutout.safeInsetLeft}
                        - Safe inset right: ${cutout.safeInsetRight}
                        - Bounding rects: ${cutout.boundingRects.size} areas
                    """.trimIndent())
                } else {
                    android.util.Log.d("MainActivity", "No display cutout detected")
                }
                insets
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Re-hide system bars when activity resumes
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        // Performance monitoring is now handled by DashboardScreen's lifecycle observer
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up repository resources including performance monitoring
        repository?.cleanup()
        repository = null
        android.util.Log.d("MainActivity", "Repository cleanup completed in onDestroy")
    }
}

@Composable
private fun HooksDashboardApp(repository: com.claudehooks.dashboard.data.repository.HookDataRepository? = null) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var backPressCount by remember { mutableStateOf(0) }
    var lastBackPressTime by remember { mutableStateOf(0L) }
    
    // Handle back button press - double tap to exit
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            // Double tap detected - exit the app
            (context as? ComponentActivity)?.finishAndRemoveTask()
        } else {
            // First tap - reset counter
            backPressCount = 1
            lastBackPressTime = currentTime
            // You could show a toast here: "Press back again to exit"
        }
    }
    
    if (showSettings) {
        SettingsScreen(
            onNavigateBack = { showSettings = false }
        )
    } else {
        DashboardScreen(
            repository = if (repository != null) {
                com.claudehooks.dashboard.data.repository.BackgroundServiceRepositoryImpl(repository)
            } else null,
            onQuitRequested = {
                (context as? ComponentActivity)?.finishAndRemoveTask()
            },
            onNavigateToSettings = { showSettings = true }
        )
    }
}