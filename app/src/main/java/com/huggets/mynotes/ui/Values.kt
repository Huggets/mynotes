package com.huggets.mynotes.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

object Values {
    val smallPadding = 12.dp
    val smallSpacing = 8.dp

    object Limit {
        val minWidthRequiredExtendedFab = 400.dp
        val minWidthRequiredFabToLeft = 800.dp
    }

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
}