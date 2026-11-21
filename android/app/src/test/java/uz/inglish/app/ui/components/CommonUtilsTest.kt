package uz.inglish.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CommonUtilsTest {

    @Test
    fun formatBand_nullValue_returnsDash() {
        assertEquals("—", formatBand(null))
    }

    @Test
    fun formatBand_wholeNumbers_returnsIntegerString() {
        assertEquals("6", formatBand(6.0))
        assertEquals("7", formatBand(7.0))
        assertEquals("9", formatBand(9.0))
    }

    @Test
    fun formatBand_halfBands_returnsDecimalString() {
        assertEquals("6.5", formatBand(6.5))
        assertEquals("7.5", formatBand(7.5))
        assertEquals("5.5", formatBand(5.5))
    }

    @Test
    fun formatBand_fractionalRounding_roundsToNearestHalf() {
        // IELTS rounding rules: 6.25 rounds to 6.5, 6.75 rounds to 7.0
        assertEquals("6.5", formatBand(6.3))
        assertEquals("7", formatBand(6.8))
    }

    @Test
    fun criterionLabel_knownKeys_returnsReadableLabels() {
        assertEquals("Task Response", criterionLabel("task_response"))
        assertEquals("Coherence & Cohesion", criterionLabel("coherence_cohesion"))
        assertEquals("Lexical Resource", criterionLabel("lexical_resource"))
        assertEquals("Grammar Range & Accuracy", criterionLabel("grammatical_range_accuracy"))
        assertEquals("Fluency & Coherence", criterionLabel("fluency_coherence"))
        assertEquals("Pronunciation", criterionLabel("pronunciation"))
    }

    @Test
    fun criterionLabel_fallbackKeys_formatsProperly() {
        assertEquals("Task achievement", criterionLabel("task_achievement"))
    }
}

