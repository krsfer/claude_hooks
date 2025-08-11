package com.claudehooks.dashboard.presentation.util

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat

/**
 * Composable that provides safe padding to avoid display cutouts (notches, punch holes)
 */
@Composable
fun cutoutAwarePadding(): PaddingValues {
    val density = LocalDensity.current
    val view = LocalView.current
    
    var paddingValues by remember { mutableStateOf(PaddingValues(0.dp)) }
    
    LaunchedEffect(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowInsets = ViewCompat.getRootWindowInsets(view)
            val cutout = windowInsets?.displayCutout
            
            paddingValues = if (cutout != null) {
                with(density) {
                    PaddingValues(
                        top = cutout.safeInsetTop.toDp(),
                        bottom = cutout.safeInsetBottom.toDp(),
                        start = cutout.safeInsetLeft.toDp(),
                        end = cutout.safeInsetRight.toDp()
                    )
                }
            } else {
                PaddingValues(0.dp)
            }
        }
    }
    
    return paddingValues
}

/**
 * Modifier that adds cutout-aware padding
 */
@Composable
fun Modifier.cutoutAware(): Modifier {
    val cutoutPadding = cutoutAwarePadding()
    return this.then(Modifier.padding(cutoutPadding))
}

/**
 * Get the top cutout inset as Dp for manual adjustments
 */
@Composable
fun getCutoutTopInset(): Dp {
    val density = LocalDensity.current
    val view = LocalView.current
    
    var topInset by remember { mutableStateOf(0.dp) }
    
    LaunchedEffect(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowInsets = ViewCompat.getRootWindowInsets(view)
            val cutout = windowInsets?.displayCutout
            
            topInset = if (cutout != null) {
                with(density) { cutout.safeInsetTop.toDp() }
            } else {
                0.dp
            }
        }
    }
    
    return topInset
}

/**
 * Detect if the device has a display cutout
 */
@Composable
fun hasCutout(): Boolean {
    val view = LocalView.current
    
    return remember(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowInsets = ViewCompat.getRootWindowInsets(view)
            windowInsets?.displayCutout != null
        } else {
            false
        }
    }
}

/**
 * Get cutout information for debugging or adaptive layouts
 */
@Composable
fun getCutoutInfo(): CutoutInfo {
    val density = LocalDensity.current
    val view = LocalView.current
    
    return remember(view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowInsets = ViewCompat.getRootWindowInsets(view)
            val cutout = windowInsets?.displayCutout
            
            if (cutout != null) {
                with(density) {
                    CutoutInfo(
                        hasCutout = true,
                        topInset = cutout.safeInsetTop.toDp(),
                        bottomInset = cutout.safeInsetBottom.toDp(),
                        leftInset = cutout.safeInsetLeft.toDp(),
                        rightInset = cutout.safeInsetRight.toDp(),
                        cutoutAreas = cutout.boundingRects.size
                    )
                }
            } else {
                CutoutInfo.NoCutout
            }
        } else {
            CutoutInfo.NoCutout
        }
    }
}

/**
 * Data class containing cutout information
 */
data class CutoutInfo(
    val hasCutout: Boolean,
    val topInset: Dp,
    val bottomInset: Dp, 
    val leftInset: Dp,
    val rightInset: Dp,
    val cutoutAreas: Int
) {
    companion object {
        val NoCutout = CutoutInfo(
            hasCutout = false,
            topInset = 0.dp,
            bottomInset = 0.dp,
            leftInset = 0.dp,
            rightInset = 0.dp,
            cutoutAreas = 0
        )
    }
}