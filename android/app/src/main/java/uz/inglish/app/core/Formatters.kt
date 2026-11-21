package uz.inglish.app.core

fun formatBand(value: Double?): String {
    if (value == null) return "—"
    val rounded = kotlin.math.round(value * 2) / 2
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

fun criterionLabel(name: String): String = when (name) {
    "task_response" -> "Task Response"
    "coherence_cohesion" -> "Coherence & Cohesion"
    "lexical_resource" -> "Lexical Resource"
    "grammatical_range_accuracy" -> "Grammar Range & Accuracy"
    "fluency_coherence" -> "Fluency & Coherence"
    "pronunciation" -> "Pronunciation"
    else -> name.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

