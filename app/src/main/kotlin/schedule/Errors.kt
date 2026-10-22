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
    data object MissingSeparator : ParseError
    data class InvalidFieldCount(val rawValue: String) : ParseError
    data class EmptyValue(val rawValue: String) : ParseError
    data class EmptyOrInvalidDate(val rawValue: String) : ParseError
    data class UnknownSessionType(val rawValue: String) : ParseError
    data class InsufficientExamTeachers(val teachersCount: Int) : ParseError

    sealed interface OthersViolation : ParseError {
        data class ContainsControlCharacters(val fieldName: String) : OthersViolation
        data class HasTrailingOrLeadingWhitespaces(val fieldName: String) : OthersViolation
        data class InvalidCancelledField(val fieldName: String) : OthersViolation
        data object InvalidActiveLocation : OthersViolation
        data object InvalidTeacherName : OthersViolation
        data object DuplicateTeachers : OthersViolation
        data class DomainError(val rawValue: String) : OthersViolation
    }
}

fun ParseError.logMessage(): String = when (this) {
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
        "type \"EXAM\" requires at least 2 teachers, got $teachersCount"
    }
    is ParseError.OthersViolation.ContainsControlCharacters -> "$fieldName value contains control characters"
    is ParseError.OthersViolation.HasTrailingOrLeadingWhitespaces -> "$fieldName value has trailing or leading whitespaces"
    is ParseError.OthersViolation.InvalidCancelledField -> "cancelled session $fieldName field must be \"-\""
    is ParseError.OthersViolation.InvalidActiveLocation -> "active session location cannot be \"-\""
    is ParseError.OthersViolation.InvalidTeacherName -> "invalid teacher name"
    is ParseError.OthersViolation.DuplicateTeachers -> "teachers list contains duplicates"
    is ParseError.OthersViolation.DomainError -> escape(this.rawValue)
}