package com.huggets.mynotes.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Values {
    val smallPadding = 12.dp
    val smallSpacing = 8.dp

    val normalFontSize = 16.sp
    val bigFontSize = 20.sp

    /**
     * An emphasized animation with a float value.
     */
    val emphasizedFloat = Animation.emphasized<Float>()

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

    object Modifier {
        val smallPadding = androidx.compose.ui.Modifier.padding(Values.smallPadding)
        val maxWidth = androidx.compose.ui.Modifier.fillMaxWidth()
        val maxSize = androidx.compose.ui.Modifier.fillMaxSize()
        val paddingMaxSize = androidx.compose.ui.Modifier.padding(Values.smallPadding).fillMaxSize()
    }
}