package com.huggets.mynotes.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

object Value {
    object Limit {
        val minWidthRequiredExtendedFab = 400.dp
        val minWidthRequiredFabToLeft = 800.dp
    }

    object Animation {
        private val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        private val emphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
        private val emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        fun <T> enterScreen() = tween<T>(
            durationMillis = 500,
            easing = emphasizedDecelerate
        )

        fun <T> exitScreenPermanently() = tween<T>(
            durationMillis = 200,
            easing = emphasizedAccelerate
        )

        fun <T> exitScreen() = tween<T>(
            durationMillis = 200,
            easing = emphasized
        )
    }

    val smallPadding = 12.dp
}