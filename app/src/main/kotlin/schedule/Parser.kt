package schedule

import java.time.LocalDate
import java.time.format.DateTimeParseException



sealed interface ParseError {
    data class MissingSeparator(val rawValue: String) : ParseError
    data class InvalidFieldCount(val rawValue: String) : ParseError
    data class EmptyValue(val rawValue: String) : ParseError
    data class EmptyOrInvalidDate(val rawValue: String) : ParseError
    data class UnknownSessionType(val rawValue: String) : ParseError
    data class InsufficientExamTeachers(val rawValue: String) : ParseError

    sealed interface OthersViolation : ParseError {
        data class ContainsControlCharacters(val rawValue: String) : OthersViolation
        data class HasTrailingOrLeadingWhitespaces(val rawValue: String) : OthersViolation
        data class InvalidCancellationField(val rawValue: String) : OthersViolation
        data class InvalidActiveLocation(val rawValue: String) : OthersViolation
        data class InvalidTeacherName(val rawValue: String) : OthersViolation
        data class DuplicateTeachers(val rawValue: String) : OthersViolation
    }
}
sealed interface ParseResult <out T> {
    data class SuccessParse<out T>(
        val value: T
     ) : ParseResult<T>

    data class FailureParse(
        var error: ParseError
    ) : ParseResult<Nothing>
}

inline fun <T> ParseResult<T>.check(error: (ParseError) -> Nothing) : T{
    return when (this) {
        is ParseResult.SuccessParse -> this.value
        is ParseResult.FailureParse -> error(this.error)
    }
}

//val dataRegex = """^[0-9]{4}-[0-9]{2}-[0-9]{2}$""".toRegex()
//Нет | — неверный разделитель.
//Полей не пять — неверное количество полей.
//Есть пустое или полностью пробельное поле — первое такое поле в порядке следования.
//Неверный формат или значение даты.
//Неизвестный тип занятия.
//У сдачи менее двух непустых и непробельных элементов списка, отличных от -.
//Остальные нарушения домена.

data class Record(
    val date: LocalDate,
    val type: SessionType,
    val topic: String,
    val location: String,
    val teachers: List<String>
)

class Parser {
    fun parse(line: String, lineNumber: Int, delimiter: Char = '|'): ParseResult<Record> {
        if (delimiter !in line) {
            return ParseResult.FailureParse(ParseError.MissingSeparator(line))
        }

        val parts = line.split(delimiter)
        if (parts.size != 5) {
            return ParseResult.FailureParse(ParseError.InvalidFieldCount(line))
        }
        val (dateRaw, typeRaw, topicRaw, locationRaw, teachersRaw) = parts
        for (field in parts) {
            if (field.isBlank()) {
                return ParseResult.FailureParse(ParseError.EmptyValue(field))
            }
        }
        val date = parseDate(dateRaw).check(error = { return ParseResult.FailureParse(it) })
        val type = parseType(typeRaw).check(error = { return ParseResult.FailureParse(it) })
        val teachers = parseTeachers(teachersRaw, type).check(error = { return ParseResult.FailureParse(it) })

        return ParseResult.SuccessParse(
            Record(
                date = date,
                type = type,
                topic = topic,
                location = location,
                teachers = teachers
            )
        )
    }

    private fun parseDate(dateStr: String): ParseResult<LocalDate> {
        return try {
            if (dateStr.startsWith("0000")) {
                return ParseResult.FailureParse(ParseError.EmptyOrInvalidDate(dateStr))
            }
            val parsedDate = LocalDate.parse(dateStr)

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
        val invalidTeacher = teachers.firstOrNull { it.isBlank() || it.trim() == "-" }
        // check cancellation type
        if (sessionType.name == "EXAM") {
            if (invalidTeacher != null || teachers.size < 2) {
                return ParseResult.FailureParse(ParseError.InsufficientExamTeachers(teachersRaw))
            }
        }
        // teachers.toSet().size != teachers.size   DuplicateTeachers
        return ParseResult.SuccessParse(teachers)
    }
// check "-" for teachers and location for cancellation
    private fun validate(valueRaw: String): ParseError? {
        if (valueRaw.any { it.isISOControl() }){
            return ParseError.OthersViolation.ContainsControlCharacters(valueRaw)
        }
        if (valueRaw.trim() != valueRaw) {
            return ParseError.OthersViolation.HasTrailingOrLeadingWhitespaces(valueRaw)
        }

        return null
    }
}

