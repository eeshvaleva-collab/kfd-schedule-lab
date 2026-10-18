package schedule

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SessionModelTest {

    private val validDate = LocalDate.of(2026, 10, 15)

    @Test
    fun `should create all four types successfully with valid data`() {
        val cancelled = Cancelled(validDate, "abcde")
        assertIs<Session>(cancelled)

        val lecture = Lecture(validDate, "fghij", "klmno", listOf("pqrst"))
        assertIs<ActiveSession>(lecture)

        val practice = Practice(validDate, "uvwxy", "zabc", listOf("defgh"))
        assertIs<ActiveSession>(practice)

        val exam = Exam(validDate, "ijk", "lmno", listOf("pqr", "stu"))
        assertIs<ActiveSession>(exam)
    }

    @Test
    fun `should fail when topic is empty or blank`() {
        assertFailsWith<IllegalArgumentException> {
            Cancelled(validDate, "")
        }
        assertFailsWith<IllegalArgumentException> {
            Lecture(validDate, "   ", "abc", listOf("def"))
        }
    }

    @Test
    fun `should fail when year is 0 or 10000 for any session type`() {
        val year0 = LocalDate.of(0, 5, 5)
        val year10000 = LocalDate.of(10000, 1, 1)

        assertFailsWith<IllegalArgumentException> { Cancelled(year0, "abc") }
        assertFailsWith<IllegalArgumentException> { Cancelled(year10000, "abc") }

        assertFailsWith<IllegalArgumentException> { Lecture(year0, "abc", "def", listOf("ghi")) }
        assertFailsWith<IllegalArgumentException> { Lecture(year10000, "abc", "def", listOf("ghi")) }

        assertFailsWith<IllegalArgumentException> { Exam(year0, "abc", "def", listOf("ghi", "jkl")) }
        assertFailsWith<IllegalArgumentException> { Exam(year10000, "abc", "def", listOf("ghi", "jkl")) }
    }

    @Test
    fun `should fail when active session has no teachers`() {
        assertFailsWith<IllegalArgumentException> {
            Lecture(validDate, "abc", "def", emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            Practice(validDate, "abc", "def", emptyList())
        }
    }

    @Test
    fun `should fail when exam has less than two teachers`() {
        assertFailsWith<IllegalArgumentException> {
            Exam(validDate, "abc", "def", listOf("ghi"))
        }
    }

    @Test
    fun `should fail when teachers list contains duplicates`() {
        assertFailsWith<IllegalArgumentException> {
            Lecture(validDate, "abc", "def", listOf("ghi", "ghi"))
        }
        assertFailsWith<IllegalArgumentException> {
            Exam(validDate, "abc", "def", listOf("ghi", "jkl", "ghi"))
        }
    }

    @Test
    fun `should protect internal teachers list from modifying the original outer list`() {
        val mutableTeachers = mutableListOf("abc", "def")
        val lecture = Lecture(validDate, "ghi", "jkl", mutableTeachers)

        mutableTeachers.clear()

        assertEquals(2, lecture.teachers.size)
        assertEquals(listOf("abc", "def"), lecture.teachers)
    }

    @Test
    fun `should protect state by returning immutable list through teachers property`() {
        val lecture = Lecture(validDate, "abc", "def", listOf("ghi", "jkl"))
        val internalList = lecture.teachers

        assertFailsWith<UnsupportedOperationException> {
            (internalList as MutableList<String>).clear()
        }
    }

    @Test
    fun `should verify that copy method cannot bypass invariants`() {
        val goodLecture = Lecture(validDate, "abc", "def", listOf("ghi"))

        assertFailsWith<IllegalArgumentException> {
            goodLecture.copy(teachersRaw = emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            goodLecture.copy(teachersRaw = listOf("ghi", "ghi"))
        }

        val goodExam = Exam(validDate, "abc", "def", listOf("ghi", "jkl"))
        assertFailsWith<IllegalArgumentException> {
            goodExam.copy(teachersRaw = listOf("ghi"))
        }
    }

    @Test
    fun `should fail when topic contains leading or trailing spaces or control characters`() {
        assertFailsWith<IllegalArgumentException> { Cancelled(validDate, " abc") }
        assertFailsWith<IllegalArgumentException> { Cancelled(validDate, "abc ") }
        assertFailsWith<IllegalArgumentException> { Cancelled(validDate, "ab\ncd") }
    }

    @Test
    fun `should fail when active session location is blank or dash`() {
        assertFailsWith<IllegalArgumentException> { Lecture(validDate, "abc", "   ", listOf("def")) }
        assertFailsWith<IllegalArgumentException> { Lecture(validDate, "abc", "-", listOf("def")) }
    }
}
