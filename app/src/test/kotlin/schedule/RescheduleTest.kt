package schedule

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RescheduleTest {

    @Test
    fun `should create new lecture on reschedule and leave original unchanged`() {
        val originalDate = LocalDate.now().plusDays(7)
        val newDate = LocalDate.now().plusDays(1)

        val originalLecture = Lecture(originalDate, "Classes", "Room 312", listOf("Portnov"))

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
}
