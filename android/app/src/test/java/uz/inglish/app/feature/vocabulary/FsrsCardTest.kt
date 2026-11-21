package uz.inglish.app.feature.vocabulary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.inglish.app.core.FsrsScheduler
import uz.inglish.app.core.ReviewRating
import uz.inglish.app.core.ReviewState
import uz.inglish.app.core.ScheduleInput
import java.time.Instant

class FsrsCardTest {

    private val fixedNow = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun newWord_ratedGood_schedulesForReviewAfterTwoDays() {
        val input = ScheduleInput(
            state = ReviewState.NEW,
            stability = 0.0,
            difficulty = 0.0,
            reps = 0,
            lapses = 0,
            rating = ReviewRating.GOOD,
            now = fixedNow,
        )

        val result = FsrsScheduler.scheduleReview(input)

        assertEquals(ReviewState.REVIEW, result.state)
        assertEquals(1, result.reps)
        assertEquals(0, result.lapses)
        assertEquals(2.0, result.stability, 0.001)
        assertEquals(2.0, result.intervalDays, 0.001)
        assertEquals(Instant.parse("2026-01-03T00:00:00Z"), result.dueAt)
    }

    @Test
    fun reviewWord_ratedAgain_movesToRelearningWithTenMinuteInterval() {
        val input = ScheduleInput(
            state = ReviewState.REVIEW,
            stability = 10.0,
            difficulty = 5.0,
            reps = 3,
            lapses = 0,
            rating = ReviewRating.AGAIN,
            now = fixedNow,
        )

        val result = FsrsScheduler.scheduleReview(input)

        assertEquals(ReviewState.RELEARNING, result.state)
        assertEquals(4, result.reps)
        assertEquals(1, result.lapses)
        // 10 daqiqa = 10 / (24 * 60) kun = 0.00694 kun
        assertEquals(10.0 / (24.0 * 60.0), result.intervalDays, 0.0001)
        assertEquals(Instant.parse("2026-01-01T00:10:00Z"), result.dueAt)
        assertTrue("Qiyinlik oshishi kerak", result.difficulty > 5.0)
    }

    @Test
    fun reviewWord_ratedEasy_increasesStabilityMoreThanGood() {
        val base = ScheduleInput(
            state = ReviewState.REVIEW,
            stability = 4.0,
            difficulty = 5.0,
            reps = 2,
            lapses = 0,
            rating = ReviewRating.GOOD,
            now = fixedNow,
        )

        val goodResult = FsrsScheduler.scheduleReview(base)
        val easyResult = FsrsScheduler.scheduleReview(base.copy(rating = ReviewRating.EASY))

        assertTrue(
            "Easy reytingi Good ga qaraganda kattaroq stability berishi kerak",
            easyResult.stability > goodResult.stability,
        )
        assertTrue(
            "Easy reytingida qiyinlik darajasi (difficulty) kamayishi kerak",
            easyResult.difficulty < goodResult.difficulty,
        )
    }

    @Test
    fun newWord_ratedHard_schedulesForLearningWithShorterInterval() {
        val input = ScheduleInput(
            state = ReviewState.NEW,
            stability = 0.0,
            difficulty = 0.0,
            reps = 0,
            lapses = 0,
            rating = ReviewRating.HARD,
            now = fixedNow,
        )

        val result = FsrsScheduler.scheduleReview(input)

        assertEquals(ReviewState.LEARNING, result.state)
        assertEquals(0.6, result.stability, 0.001)
        assertEquals(1.0, result.intervalDays, 0.001)
        assertTrue("Hard reytingida qiyinlik 5.0 dan yuqori bo'ladi", result.difficulty >= 6.0)
    }

    @Test
    fun maxInterval_doesNotExceed180Days() {
        val input = ScheduleInput(
            state = ReviewState.REVIEW,
            stability = 250.0,
            difficulty = 2.0,
            reps = 10,
            lapses = 0,
            rating = ReviewRating.EASY,
            now = fixedNow,
        )

        val result = FsrsScheduler.scheduleReview(input)

        assertTrue(
            "Interval maksimal 180 kundan oshmasligi kerak",
            result.intervalDays <= 180.0,
        )
    }
}
