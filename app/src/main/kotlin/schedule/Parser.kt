package schedule

import java.time.LocalDate
import kotlin.text.isISOControl

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

    //TODO
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
        if (type == SessionType.CANCELLED){
            if (teachersRaw != "-") {
                return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancelledField(teachersRaw))
            }
            if (locationRaw != "-"){
                return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancelledField(locationRaw))
            }
            val topic = validate(topicRaw).check(error = { return ParseResult.FailureParse(it) })
            return ParseResult.SuccessParse(Cancelled(date, topic))
        }

        val teachers = parseTeachers(teachersRaw, type).check(error = { return ParseResult.FailureParse(it) })
        val topic = validate(topicRaw).check(error = { return ParseResult.FailureParse(it) })
        val location = validate(locationRaw, checkInvalid = true).check(error = { return ParseResult.FailureParse(it) })

        val session = when (type) {
                SessionType.LECTURE -> Lecture(date, topic, location, teachers)
                SessionType.PRACTICE -> Practice(date, topic, location, teachers)
                SessionType.EXAM -> Exam(date, topic, location, teachers)
            }
        return ParseResult.SuccessParse(session)
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
        val validTeachersCount = teachers.count { it.isNotBlank() && it.trim() != "-"}

        if (sessionType == SessionType.EXAM && (validTeachersCount < 2)) {
                return ParseResult.FailureParse(ParseError.InsufficientExamTeachers(teachersRaw))
        }
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


