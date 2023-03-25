package com.huggets.mynotes

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

fun shortened(
    string: String,
    maxSize: Float,
    density: Density,
    contentWidth: Dp,
    fontSize: TextUnit
): String {
    val widthPx = with(density) { contentWidth.toPx() }
    val fontPx = with(density) { fontSize.toDp() }

    val max = (widthPx / fontPx.value * (0.75f * (maxSize))).toInt()

    val oneLine = string.lines().let {
        val stringBuilder = StringBuilder()
        var index = 0
        while (index != it.size) {
            stringBuilder.append(it[index])
            stringBuilder.append(' ')
            index++
        }

        stringBuilder.toString()
    }

    return if (oneLine.length < max) oneLine
    else oneLine.substring(0, max) + "..."
}