package schedule

import java.time.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class SessionModelTest {

    private val validDate = LocalDate.of(2026, 10, 15)

    // 1. Создать занятие с пустой темой.
    @Test
    fun `should throw exception when topic is blank`() {
        assertThrows<IllegalArgumentException> {
            Lecture(validDate, "   ", "Room 101", listOf("Ivanov"))
        }
    }

    // 2. Создать занятие с годом 0 или 10000.
    @Test
    fun `should throw exception when year is out of bounds`() {
        assertThrows<IllegalArgumentException> {
            val invalidDateZero = LocalDate.of(0, 10, 15) // Примечание: LocalDate.of(0) поддерживается Java, но запрещен ТЗ
            Lecture(invalidDateZero, "Topic", "Room 101", listOf("Ivanov"))
        }
        assertThrows<IllegalArgumentException> {
            val invalidDateFuture = LocalDate.of(10000, 10, 15)
            Lecture(invalidDateFuture, "Topic", "Room 101", listOf("Ivanov"))
        }
    }

    // 3. Создать действующее занятие без преподавателей.
    @Test
    fun `should throw exception when active session has no teachers`() {
        assertThrows<IllegalArgumentException> {
            Lecture(validDate, "Topic", "Room 101", emptyList())
        }
    }

    // 4. Создать сдачу с одним преподавателем.
    @Test
    fun `should throw exception when exam has less than two teachers`() {
        assertThrows<IllegalArgumentException> {
            Exam(validDate, "Oral Exam", "Room 401", listOf("Ivanov"))
        }
    }

    // 5. Создать сдачу с двумя одинаковыми преподавателями.
    @Test
    fun `should throw exception when exam has duplicate teachers`() {
        assertThrows<IllegalArgumentException> {
            Exam(validDate, "Oral Exam", "Room 401", listOf("Ivanov", "Ivanov"))
        }
    }

    // 6. Изменить исходный список после создания занятия.
    @Test
    fun `should not mutate session when source teachers list is modified`() {
        val mutableTeachers = mutableListOf("Ivanov", "Petrov")
        val lecture = Lecture(validDate, "Topic", "Room 101", mutableTeachers)

        // Ломаем исходный список снаружи
        mutableTeachers.clear()

        // У созданной лекции по-прежнему должны остаться два преподавателя
        assertEquals(2, lecture.teachers.size)
        assertEquals("Ivanov", lecture.teachers[0])
        assertEquals("Petrov", lecture.teachers[1])
    }

    // 7. Изменить состояние занятия через выданный список.
    @Test
    fun `should throw exception when trying to mutate returned teachers list via cast`() {
        val lecture = Lecture(validDate, "Topic", "Room 101", listOf("Ivanov", "Petrov"))
        val exposedTeachers = lecture.teachers

        // Попытка взлома через принудительный каст к изменяемому списку JVM
        assertThrows<UnsupportedOperationException> {
            (exposedTeachers as MutableList<String>).clear()
        }
    }

    // 8. Обойти проверки через copy, если он присутствует в вашем API.
    @Test
    fun `should re-evaluate constraints and immutability when copy is invoked`() {
        val goodLecture = Lecture(validDate, "Valid Topic", "Room 101", listOf("Ivanov"))

        // Проверяем, что .copy() триггерит валидацию полей через свойства
        assertThrows<IllegalArgumentException> {
            goodLecture.copy(topic = "   ")
        }

        // Проверяем, что .copy() заново замораживает переданный изменяемый список
        val badMutableList = mutableListOf("Sidorov")
        val copiedLecture = goodLecture.copy(teachersRaw = badMutableList)

        badMutableList.clear() // Пытаемся сломать скопированный объект через внешний список

        assertEquals(1, copiedLecture.teachers.size)
        assertEquals("Sidorov", copiedLecture.teachers[0])
    }
}
