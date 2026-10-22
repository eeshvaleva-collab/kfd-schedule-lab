package schedule

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs


class RescheduleTest {

    @Test
    fun `should create new lecture on reschedule and leave original unchanged`() {
        val originalDate = LocalDate.now().plusDays(7)
        val newDate = LocalDate.now().plusDays(1)

        val originalLecture = Lecture(originalDate, "Classes", "Room 312", listOf("Ivanov"))

        val result = originalLecture.reschedule(newDate)

        assertTrue(result is RescheduleResult.Success)
        val newLecture = result.value

        assertEquals(newDate, newLecture.date)
        assertEquals(originalLecture.topic, newLecture.topic)
        assertEquals(originalLecture.location, newLecture.location)
        assertEquals(originalLecture.teachers, newLecture.teachers)
        assertEquals(originalDate, originalLecture.date)
    }

    @Test
    fun `should return failure when rescheduling cancelled session`() {
        val originalDate = LocalDate.now().plusDays(7)
        val newDate = LocalDate.now().plusDays(1)

        val cancelled = Cancelled(originalDate, "Lecture postponed")
        val result = cancelled.reschedule(newDate)

        assertTrue(result is RescheduleResult.Failure)
    }

    @Test
    fun `should return failure when rescheduling to the same date`() {
        val date = LocalDate.of(2026, 12, 1)
        val lecture = Lecture(date, "Topic", "Room 1", listOf("Ivanov"))

        val result = lecture.reschedule(date, now = LocalDate.of(2026, 11, 1))

        assertIs<RescheduleResult.Failure>(result)
        assertEquals(RescheduleError.SameDate, result.error)
        assertEquals("New date can't be the same as the old date", result.error.logMessage())
    }

    @Test
    fun `should return failure when new date is not in the future`() {
        val originalDate = LocalDate.of(2026, 12, 1)
        val now = LocalDate.of(2026, 11, 1)
        val lecture = Lecture(originalDate, "Topic", "Room 1", listOf("Ivanov"))

        val pastDate = LocalDate.of(2026, 10, 1)
        val result = lecture.reschedule(pastDate, now = now)

        assertIs<RescheduleResult.Failure>(result)
        assertEquals(RescheduleError.DateNotInFuture, result.error)
        assertEquals("New date must be in the future", result.error.logMessage())
    }

    @Test
    fun `should return failure when new date has invalid year`() {
        val originalDate = LocalDate.of(2026, 12, 1)
        val lecture = Lecture(originalDate, "Topic", "Room 1", listOf("Ivanov"))

        val invalidDate = LocalDate.of(10000, 1, 1)
        val result = lecture.reschedule(invalidDate, now = LocalDate.of(2026, 11, 1))

        assertIs<RescheduleResult.Failure>(result)
        assertEquals(RescheduleError.InvalidYear, result.error)
        assertEquals("Year should be in between 1 - 9999", result.error.logMessage())
    }
}
