package schedule

import java.time.LocalDate
import kotlin.text.isISOControl

sealed interface ParseResult <out T> {
    data class SuccessParse<out T>(
        val value: T
     ) : ParseResult<T>

    data class FailureParse(
        val error: ParseError
    ) : ParseResult<Nothing>
}

inline fun <T> ParseResult<T>.check(error: (ParseError) -> Nothing) : T{
    return when (this) {
        is ParseResult.SuccessParse -> this.value
        is ParseResult.FailureParse -> error(this.error)
    }
}

//Порядок проверок
//На одну строку возвращается одна ошибка. Используйте следующий приоритет:
//
//Нет | — неверный разделитель.
//Полей не пять — неверное количество полей.
//Есть пустое или полностью пробельное поле — первое такое поле в порядке следования.
//Неверный формат или значение даты.
//Неизвестный тип занятия.
//У сдачи менее двух непустых и непробельных элементов списка, отличных от -.
//Остальные нарушения домена.
//Внутри пункта 7 проверяйте последовательно:
//
//Управляющие символы.
//Внешние пробельные символы.
//Неверные поля отмены.
//Недопустимое место действующего занятия.
//Недопустимые элементы списка преподавателей.
//Повторные имена преподавателей.

class Parser {
    fun parse(line: String, delimiter: Char = '|'): ParseResult<Session> {
        val (dateRaw, typeRaw, topicRaw, locationRaw, teachersRaw) = validateGeneral(line, delimiter = delimiter).check(error = { return ParseResult.FailureParse(it) })
        val date = parseDate(dateRaw).check(error = { return ParseResult.FailureParse(it) })
        val type = parseType(typeRaw).check(error = { return ParseResult.FailureParse(it) })
        if (type == SessionType.CANCELLED) {
            if (teachersRaw != "-") {
                return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancelledField(teachersRaw))
            }
            if (locationRaw != "-") {
                return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancelledField(locationRaw))
            }
            val topic = validate(topicRaw).check(error = { return ParseResult.FailureParse(it) })
            return ParseResult.SuccessParse(Cancelled(date, topic))
        }

        val teachers = parseTeachers(teachersRaw, type).check(error = { return ParseResult.FailureParse(it) })
        val topic = validate(topicRaw).check(error = { return ParseResult.FailureParse(it) })
        val location = validate(locationRaw, checkInvalid = true).check(error = { return ParseResult.FailureParse(it) })

        return try {
            val session = when (type) {
                SessionType.LECTURE -> Lecture(date, topic, location, teachers)
                SessionType.PRACTICE -> Practice(date, topic, location, teachers)
                SessionType.EXAM -> Exam(date, topic, location, teachers)
            }
            ParseResult.SuccessParse(session)
        } catch (e: IllegalArgumentException) {
                ParseResult.FailureParse(ParseError.OthersViolation.DomainError(e.message ?: "Domain violation"))
        }
    }
    private fun validateGeneral(line: String, delimiter: Char) :ParseResult<List<String>> {
        if (delimiter !in line) {
            return ParseResult.FailureParse(ParseError.MissingSeparator(line))
        }

        val parts = line.split(delimiter)
        if (parts.size != 5) {
            return ParseResult.FailureParse(ParseError.InvalidFieldCount(line))
        }

        val fieldNames = listOf("date", "type", "topic", "location", "teachers")
        for (index in parts.indices) {
            if (parts[index].isBlank()) {
                return ParseResult.FailureParse(ParseError.EmptyValue(fieldNames[index]))
            }
        }

        return ParseResult.SuccessParse(parts)
    }

    private fun parseDate(dateStr: String): ParseResult<LocalDate> {
        return try {
            val parsedDate = LocalDate.parse(dateStr)
            if (parsedDate.year !in 1..9999){
                return ParseResult.FailureParse(ParseError.EmptyOrInvalidDate(dateStr))
            }
            return ParseResult.SuccessParse(parsedDate)
        } catch (e: Exception) {
            ParseResult.FailureParse(ParseError.EmptyOrInvalidDate(dateStr))
        }
    }

    private fun parseType(typeRaw: String): ParseResult<SessionType> {
        for (element in SessionType.entries) {
            if (typeRaw == element.name) {
                return ParseResult.SuccessParse(element)
            }
        }
        return ParseResult.FailureParse(ParseError.UnknownSessionType(typeRaw))
    }

    private fun parseTeachers(teachersRaw: String, sessionType: SessionType): ParseResult<List<String>> {
        val teachers = teachersRaw.split(',')
        val validTeachersCount = teachers.count { it.isNotBlank() && it.trim() != "-"}

        if (sessionType == SessionType.EXAM && (validTeachersCount < 2)) {
                return ParseResult.FailureParse(ParseError.InsufficientExamTeachers(validTeachersCount))
        }
    // check teachers count for all active sessions
        for (teacher in teachers){
            if (teacher.any{it.isISOControl()}){
                return ParseResult.FailureParse(ParseError.OthersViolation.ContainsControlCharacters(teachersRaw))
            }
            if (teacher.isNotBlank() && teacher.trim() != teacher){
                return ParseResult.FailureParse(ParseError.OthersViolation.HasTrailingOrLeadingWhitespaces(teachersRaw))
            }
        }

        if (validTeachersCount != teachers.size) {
            return ParseResult.FailureParse(ParseError.OthersViolation.InvalidTeacherName(teachersRaw))
        }
        if (teachers.toSet().size != teachers.size) {
            return ParseResult.FailureParse(ParseError.OthersViolation.DuplicateTeachers(teachersRaw))
        }

        return ParseResult.SuccessParse(teachers)
    }

    private fun validate(valueRaw: String, checkInvalid: Boolean = false): ParseResult<String> {
        if (valueRaw.any { it.isISOControl() }){
            return ParseResult.FailureParse(ParseError.OthersViolation.ContainsControlCharacters(valueRaw))
        }
        if (valueRaw.trim() != valueRaw) {
            return ParseResult.FailureParse(ParseError.OthersViolation.HasTrailingOrLeadingWhitespaces(valueRaw))
        }
        if (checkInvalid && (valueRaw.isEmpty() || valueRaw == "-")) {
            return ParseResult.FailureParse(ParseError.OthersViolation.InvalidActiveLocation(valueRaw))
        }
        return ParseResult.SuccessParse(valueRaw)
    }
}


fun main() {
    val line: String = "2026-10-05|LECTURE| TOPIC|Room 1|fg"
    val res = Parser().parse(line)
    res.check { error ->
        println(error.logMessage())
        return
    }


}