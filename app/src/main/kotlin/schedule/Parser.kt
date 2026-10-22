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

class Parser {
    fun parse(line: String, delimiter: Char = '|'): ParseResult<Session> {
        val (dateRaw, typeRaw, topic, location, teachersRaw) = validateGeneral(line, delimiter = delimiter).check{
            return ParseResult.FailureParse(it)
        }
        val date = parseDate(dateRaw).check {
            return ParseResult.FailureParse(it)
        }
        val type = parseType(typeRaw).check {
            return ParseResult.FailureParse(it)
        }

        val teachersList = teachersRaw.split(',')
        if (type == SessionType.EXAM) {
            val validTeachersCount = teachersList.count {
                it.isNotBlank() && it.trim() != "-"
            }

            if (validTeachersCount < 2) {
                return ParseResult.FailureParse(
                    ParseError.InsufficientExamTeachers(validTeachersCount)
                )
            }
        }

        checkControlCharacters(topic, "topic").check { return ParseResult.FailureParse(it) }
        checkControlCharacters(location, "location").check { return ParseResult.FailureParse(it) }
        checkControlCharacters(teachersRaw, "teachers").check { return ParseResult.FailureParse(it) }

        checkExternalWhitespaces(topic, "topic").check { return ParseResult.FailureParse(it) }
        checkExternalWhitespaces(location, "location").check { return ParseResult.FailureParse(it) }
        checkExternalWhitespaces(teachersRaw, "teachers").check { return ParseResult.FailureParse(it) }


        if (type == SessionType.CANCELLED) {
            if (location != "-" || teachersRaw != "-") {
                return ParseResult.FailureParse(
                    ParseError.OthersViolation.InvalidCancelledField(
                        if (location != "-") "location" else "teachers"
                    )
                )
            }
            return ParseResult.SuccessParse(Cancelled(date, topic))
        }

        if (location == "-") {
            return ParseResult.FailureParse(
                ParseError.OthersViolation.InvalidActiveLocation
            )
        }

        val teachers = parseTeachers(teachersRaw).check {
            return ParseResult.FailureParse(it)
        }

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
            return ParseResult.FailureParse(ParseError.MissingSeparator)
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

    private fun parseTeachers(teachersRaw: String): ParseResult<List<String>> {
        val teachers = teachersRaw.split(',')

        if (teachers.any { it.isBlank() || it.trim() == "-" || (it.trim() != it) }) {
            return ParseResult.FailureParse(ParseError.OthersViolation.InvalidTeacherName)
        }
        if (teachers.toSet().size != teachers.size) {
            return ParseResult.FailureParse(ParseError.OthersViolation.DuplicateTeachers)
        }

        return ParseResult.SuccessParse(teachers)
    }
    private fun checkControlCharacters(value: String, fieldName: String): ParseResult<Unit> {
        if (value.any { it.isISOControl() }) {
                return ParseResult.FailureParse(ParseError.OthersViolation.ContainsControlCharacters(fieldName))
        }
        return ParseResult.SuccessParse(Unit)
    }

    private fun checkExternalWhitespaces(value: String, fieldName: String): ParseResult<Unit> {
        if (value.trim() != value) {
                return ParseResult.FailureParse(ParseError.OthersViolation.HasTrailingOrLeadingWhitespaces(fieldName))
        }
        return ParseResult.SuccessParse(Unit)
    }
}