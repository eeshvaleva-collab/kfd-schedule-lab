package schedule

import java.time.LocalDate

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

class Parser {
    fun parse(line: String, delimiter: Char = '|'): ParseResult<Session> {
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

        val (dateRaw, typeRaw, topicRaw, locationRaw, teachersRaw) = parts
        val date = parseDate(dateRaw).check(error = { return ParseResult.FailureParse(it) })
        val type = parseType(typeRaw).check(error = { return ParseResult.FailureParse(it) })
        if (type.name == "CANCELLED"){
            if (teachersRaw != "-") {
                return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancelledField(teachersRaw))
            }
            if (locationRaw != "-"){
                return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancelledField(locationRaw))
            }
        }
        val teachers = parseTeachers(teachersRaw, type).check(error = { return ParseResult.FailureParse(it) })
        val topic = validate(topicRaw).check(error = { return ParseResult.FailureParse(it) })
        val location = validate(locationRaw).check(error = { return ParseResult.FailureParse(it) })

        return try {
            val session = when (type) {
                SessionType.LECTURE -> Lecture(date, topic, location, teachers)
                SessionType.PRACTICE -> Practice(date, topic, location, teachers)
                SessionType.EXAM -> Exam(date, topic, location, teachers)
                SessionType.CANCELLED -> Cancelled(date, topic)
            }
            ParseResult.SuccessParse(session)
            }
        catch (e: IllegalArgumentException) {
                ParseResult.FailureParse(ParseError.OthersViolation.DomainError(e.message ?: ""))
            }
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
        if (sessionType.name == "CANCELLED" && (teachersRaw != "-")) {
            return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancelledField(teachersRaw))
        }
        val teachers = teachersRaw.split(',')
        val validTeachersCount = if (teachersRaw == "-") 0 else teachers.count { it.isNotBlank() && it.trim() != "-"
        }

        if (sessionType.name == "EXAM" && (validTeachersCount != 0 || teachers.size < 2)) {
                return ParseResult.FailureParse(ParseError.InsufficientExamTeachers(teachersRaw))
        }
        if (validTeachersCount != 0) {
            return ParseResult.FailureParse(ParseError.OthersViolation.InvalidTeacherName(teachersRaw))
        }
        if (teachers.toSet().size != teachers.size) {
            return ParseResult.FailureParse(ParseError.OthersViolation.DuplicateTeachers(teachersRaw))
        }
        return ParseResult.SuccessParse(teachers)
    }

    private fun validate(valueRaw: String): ParseResult<String> {
        if (valueRaw.any { it.isISOControl() }){
            return ParseResult.FailureParse(ParseError.OthersViolation.ContainsControlCharacters(valueRaw))
        }
        if (valueRaw.trim() != valueRaw) {
            return ParseResult.FailureParse(ParseError.OthersViolation.HasTrailingOrLeadingWhitespaces(valueRaw))
        }
        return ParseResult.SuccessParse(valueRaw)
    }
}


