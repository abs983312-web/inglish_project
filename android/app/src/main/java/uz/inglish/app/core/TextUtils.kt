package uz.inglish.app.core

fun countWords(text: String): Int =
    text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
