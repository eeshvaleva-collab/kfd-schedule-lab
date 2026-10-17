package schedule

fun escape(value: String): String {
    return buildString {
        for (ch in value) {
            when {
                ch == '\\' -> append("\\\\")
                ch == '"' -> append("\\\"")
                ch.isISOControl() || ch.code > 127 -> {
                    append(String.format("\\u%04x", ch.code))
                }
                else -> append(ch)
            }
        }
    }
}

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
        data class InvalidCancelledField(val rawValue: String) : OthersViolation
        data class InvalidActiveLocation(val rawValue: String) : OthersViolation
        data class InvalidTeacherName(val rawValue: String) : OthersViolation
        data class DuplicateTeachers(val rawValue: String) : OthersViolation
        data class DomainError(val rawValue: String) : OthersViolation
    }
}

fun ParseError.toLogMessage(): String = when (this) {
    is ParseError.MissingSeparator -> "invalid field separator"
    is ParseError.InvalidFieldCount -> {
        val actualFields = this.rawValue.split('|').size
        "got $actualFields fields, expected 5"
    }
    is ParseError.EmptyValue -> {
        "empty required field \"${escape(this.rawValue)}\""
    }
    is ParseError.EmptyOrInvalidDate -> "invalid date \"${escape(this.rawValue)}\""
    is ParseError.UnknownSessionType -> "unknown session type \"${escape(this.rawValue)}\""
    is ParseError.InsufficientExamTeachers -> {
        val teachersCount = if (this.rawValue == "-") 0 else this.rawValue.split(',').size
        "type \"EXAM\" requires at least 2 teachers, got $teachersCount"
    }
    is ParseError.OthersViolation.ContainsControlCharacters -> "value contains control characters"
    is ParseError.OthersViolation.HasTrailingOrLeadingWhitespaces -> "value has trailing or leading whitespaces"
    is ParseError.OthersViolation.InvalidCancelledField -> "cancelled session fields must be \"-\""
    is ParseError.OthersViolation.InvalidActiveLocation -> "active session location cannot be \"-\""
    is ParseError.OthersViolation.InvalidTeacherName -> "invalid teacher name"
    is ParseError.OthersViolation.DuplicateTeachers -> "teachers list contains duplicates"
    is ParseError.OthersViolation.DomainError -> escape(this.rawValue)
}