package schedule

import java.time.LocalDate


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
        data class DomainError(val rawValue: String) : OthersViolation
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

class Parser {
    fun parse(line: String, delimiter: Char = '|'): ParseResult<Session> {
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
        if (type.name == "CANCELLED"){
            if (teachersRaw != "-") {
                return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancellationField(teachersRaw))
            }
            if (locationRaw != "-"){
                return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancellationField(locationRaw))
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
                SessionType.CANCELLED -> Cancellation(date, topic)
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
        if (sessionType.name == "CANCELLATION" && (teachersRaw != "-")) {
            return ParseResult.FailureParse(ParseError.OthersViolation.InvalidCancellationField(teachersRaw))
        }
        val teachers = teachersRaw.split(',')
        val invalidTeacher = teachers.firstOrNull { it.isBlank() || it.trim() == "-" }
        // check cancellation type
        if (sessionType.name == "EXAM" && (invalidTeacher != null || teachers.size < 2)) {
                return ParseResult.FailureParse(ParseError.InsufficientExamTeachers(teachersRaw))
        }
        if (teachers.toSet().size != teachers.size) {
            return ParseResult.FailureParse(ParseError.OthersViolation.DuplicateTeachers(teachersRaw))
        }
        return ParseResult.SuccessParse(teachers)
    }
// check "-" for teachers and location for cancellation
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

//fun main(){
//    val f = Parser()
//    println(f.parse("0000-12-22|PRACTI|Topic 010|Room 312|Anokhin"))
//}

