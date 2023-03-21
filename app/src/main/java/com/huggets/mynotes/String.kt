package com.huggets.mynotes

fun shortened(string: String, maxSize: Int): String =
    if (string.length < maxSize) string
    else string.substring(0, maxSize) + "..."