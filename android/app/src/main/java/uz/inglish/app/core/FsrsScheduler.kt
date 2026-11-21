package uz.inglish.app.core

import java.time.Instant

/**
 * Lug'at kartalari uchun FSRS (Free Spaced Repetition Scheduler) takrorlash hisoblagichi.
 * Backenddagi scheduler.ts mantiqi bilan 100% mos keladi.
 */
enum class ReviewState {
    NEW,
    LEARNING,
    REVIEW,
    RELEARNING,
}

enum class ReviewRating(val value: Int) {
    AGAIN(1), // Qayta o'rganish (eslay olmadi)
    HARD(2),  // Qiyin
    GOOD(3),  // To'g'ri / Yaxshi
    EASY(4);  // Oson

    companion object {
        fun fromInt(value: Int): ReviewRating =
            entries.firstOrNull { it.value == value } ?: GOOD
    }
}

data class ScheduleInput(
    val state: ReviewState,
    val stability: Double,
    val difficulty: Double,
    val reps: Int,
    val lapses: Int,
    val rating: ReviewRating,
    val now: Instant = Instant.now(),
)

data class ScheduleResult(
    val state: ReviewState,
    val stability: Double,
    val difficulty: Double,
    val reps: Int,
    val lapses: Int,
    val dueAt: Instant,
    val intervalDays: Double,
)

object FsrsScheduler {
    private const val MAX_INTERVAL_DAYS = 180.0

    private fun clamp(value: Double, min: Double, max: Double): Double =
        maxOf(min, minOf(max, value))

    fun scheduleReview(input: ScheduleInput): ScheduleResult {
        val now = input.now
        val rating = input.rating

        var state = input.state
        var stability = input.stability
        var difficulty = input.difficulty
        var reps = input.reps
        var lapses = input.lapses
        val intervalDays: Double

        if (rating == ReviewRating.AGAIN) {
            state = ReviewState.RELEARNING
            lapses += 1
            stability = maxOf(0.2, if (stability > 0.0) stability * 0.4 else 0.2)
            difficulty = clamp(difficulty + 1.2, 1.0, 10.0)
            intervalDays = 10.0 / (24.0 * 60.0) // 10 daqiqa keyin
        } else {
            val isFirstReview = reps == 0 || state == ReviewState.NEW

            if (isFirstReview) {
                stability = when (rating) {
                    ReviewRating.HARD -> 0.6
                    ReviewRating.GOOD -> 2.0
                    ReviewRating.EASY -> 4.0
                    ReviewRating.AGAIN -> 0.2
                }
                difficulty = clamp(
                    5.0 + when (rating) {
                        ReviewRating.HARD -> 1.0
                        ReviewRating.EASY -> -1.0
                        else -> 0.0
                    },
                    1.0,
                    10.0,
                )
            } else {
                val multiplier = when (rating) {
                    ReviewRating.HARD -> 1.2
                    ReviewRating.GOOD -> 2.3
                    ReviewRating.EASY -> 3.2
                    ReviewRating.AGAIN -> 0.4
                }
                stability = maxOf(0.5, stability * multiplier)
                difficulty = clamp(
                    difficulty + when (rating) {
                        ReviewRating.HARD -> 0.4
                        ReviewRating.EASY -> -0.3
                        else -> 0.0
                    },
                    1.0,
                    10.0,
                )
            }

            state = if (rating == ReviewRating.HARD) ReviewState.LEARNING else ReviewState.REVIEW
            intervalDays = minOf(
                MAX_INTERVAL_DAYS,
                maxOf(if (rating == ReviewRating.HARD) 1.0 else 0.5, stability),
            )
        }

        reps += 1

        val roundedStability = Math.round(stability * 1000.0) / 1000.0
        val roundedDifficulty = Math.round(difficulty * 1000.0) / 1000.0
        val roundedIntervalDays = Math.round(intervalDays * 10000.0) / 10000.0
        val millisToAdd = Math.round(intervalDays * 24.0 * 60.0 * 60.0 * 1000.0)

        return ScheduleResult(
            state = state,
            stability = roundedStability,
            difficulty = roundedDifficulty,
            reps = reps,
            lapses = lapses,
            dueAt = now.plusMillis(millisToAdd),
            intervalDays = roundedIntervalDays,
        )
    }
}
