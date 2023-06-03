package com.huggets.mynotes.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * An object containing useful values for the UI.
 */
object Values {
    val smallPadding = 12.dp
    val smallSpacing = 8.dp

    val normalFontSize = 16.sp
    val bigFontSize = 20.sp

    /**
     * The minimum width required for the extended FAB to be displayed. If the width of the screen
     * is less than this value, the extended FAB will be displayed as a normal FAB.
     */
    val minWidthRequiredExtendedFab = 400.dp

    /**
     * The minimum width required for the FAB to be displayed on the left side of the screen. If
     * the width of the screen is less than this value, the FAB will be displayed in the center of
     * the screen.
     */
    val minWidthRequiredFabToLeft = 800.dp

    /**
     * An emphasized animation with a float value.
     */
    val emphasizedFloat = Animation.emphasized<Float>()

    /**
     * Contains animation values.
     */
    object Animation {
        const val slideOffset = 0.2f

        fun <T> emphasizedDecelerate(durationMillis: Int = 500) = tween<T>(
            durationMillis,
            easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        )

        fun <T> emphasizedAccelerate(durationMillis: Int = 200) = tween<T>(
            durationMillis,
            easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
        )

        fun <T> emphasized(durationMillis: Int = 200) = tween<T>(
            durationMillis,
            easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        )
    }

    /**
     * Contains modifier values used for the Compose UI.
     */
    object Modifier {
        val smallPadding = androidx.compose.ui.Modifier.padding(Values.smallPadding)

        val maxWidth = androidx.compose.ui.Modifier.fillMaxWidth()

        val maxSize = androidx.compose.ui.Modifier.fillMaxSize()

        val paddingMaxSize = androidx.compose.ui.Modifier
            .padding(Values.smallPadding)
            .fillMaxSize()
    }
}