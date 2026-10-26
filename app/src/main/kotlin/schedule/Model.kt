package schedule

import java.time.LocalDate

sealed interface Session {
    val date: LocalDate
    val topic: String
    val sessionType: SessionType

    fun reschedule(newDate: LocalDate, now: LocalDate = LocalDate.now()): RescheduleResult<ActiveSession> = when {
        this !is ActiveSession -> RescheduleResult.Failure(RescheduleError.CancelledSession)

        newDate.year !in 1..9999 -> RescheduleResult.Failure(RescheduleError.InvalidYear)
        newDate == date -> RescheduleResult.Failure(RescheduleError.SameDate)
        newDate <= now -> RescheduleResult.Failure(RescheduleError.DateNotInFuture)

        else -> RescheduleResult.Success(createRescheduledSession(newDate))
    }

    fun formString(showTeachers: Boolean = false): String {
        val typeName = this.sessionType
        val locationStr = if (this is ActiveSession) this.location else "-"

        val base = "$date | $typeName | $topic | $locationStr"
        return if (showTeachers) {
            val teachersStr = if (this is ActiveSession) this.teachers.joinToString(", ") else "-"
            "$base | $teachersStr"
        } else base
    }
}

sealed interface RescheduleResult<out T> {
    data class Success<out T>(val value: T) : RescheduleResult<T>
    data class Failure(val error: RescheduleError) : RescheduleResult<Nothing>
}

sealed interface RescheduleError {
    data object CancelledSession : RescheduleError
    data object InvalidYear : RescheduleError
    data object DateNotInFuture : RescheduleError
    data object SameDate : RescheduleError
}

fun RescheduleError.logMessage(): String = when (this) {
    is RescheduleError.CancelledSession -> "Cannot reschedule a cancelled session"
    is RescheduleError.DateNotInFuture -> "New date must be in the future"
    is RescheduleError.InvalidYear -> "Year should be in between 1 - 9999"
    is RescheduleError.SameDate -> "New date can't be the same as the old date"
}
sealed interface ActiveSession : Session {
    val location: String
    val teachers: List<String>

    fun createRescheduledSession(newDate: LocalDate): ActiveSession
}

abstract class BaseSession : Session {
    override fun equals(other: Any?): Boolean = when{
        this === other -> true
        other == null || other !is Session || this.sessionType != other.sessionType -> false
        date != other.date || topic != other.topic -> false
        this is ActiveSession && other is ActiveSession ->
            location == other.location && teachers == other.teachers
        else -> true
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + topic.hashCode()
        if (this is ActiveSession) {
            result = 31 * result + location.hashCode()
            result = 31 * result + teachers.hashCode()
        }
        return result
    }
}

internal object SessionRules {
    fun checkCommonConstraints(date: LocalDate, topic: String) {
        require(date.year in 1..9999) { "Year must be between 1 and 9999" }
        require(topic.none { it.isISOControl() }) { "Topic cannot contain control characters" }
        require(topic.isNotBlank()) { "Topic cannot be empty" }
        require(topic.trim() == topic) { "Topic cannot have leading or trailing spaces" }
    }

    fun checkActiveConstraints(date: LocalDate, topic: String, location: String, teachers: List<String>, minTeachers: Int = 1): List<String> {
        checkCommonConstraints(date, topic)

        require(location.none { it.isISOControl() }) { "Location cannot contain control characters" }
        require(location.isNotBlank() && location != "-") { "Location cannot be empty or '-'" }
        require(location.trim() == location) { "Location cannot have leading or trailing spaces" }

        require(teachers.none { it.isBlank() || it.trim() == "-" }) { "Invalid teacher name" }
        require(teachers.toSet().size == teachers.size) { "Teachers list cannot contain duplicates" }
        require(teachers.isNotEmpty()) { "Teachers list cannot be empty" }
        require(teachers.size >= minTeachers) { "At least $minTeachers teacher(s) required" }
        for (teacher in teachers) {
            require(teacher.none { it.isISOControl() }) { "Teacher name cannot contain control characters" }
            require(teacher.trim() == teacher) { "Teacher name cannot have leading or trailing spaces" }
            require(teacher.isNotEmpty() && teacher != "-") { "Invalid teacher name" }
        }

        return teachers
    }
}

class Lecture private constructor(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    override val teachers: List<String>
) : BaseSession(), ActiveSession {
    override val sessionType: SessionType = SessionType.LECTURE
    override fun createRescheduledSession(newDate: LocalDate) =
        Lecture(newDate, topic, location, teachers)

    companion object {
        operator fun invoke(date: LocalDate, topic: String, location: String, teachersRaw: List<String>): Lecture {
            val teachers: List<String> = java.util.List.copyOf(teachersRaw)
            SessionRules.checkActiveConstraints(date, topic, location, teachers)
            return Lecture(date, topic, location, teachers)
        }
    }
}

class Practice private constructor(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    override val teachers: List<String>
) : BaseSession(), ActiveSession {
    override val sessionType: SessionType = SessionType.PRACTICE
    override fun createRescheduledSession(newDate: LocalDate) =
        Practice(newDate, topic, location, teachers)

    companion object {
        operator fun invoke(date: LocalDate, topic: String, location: String, teachersRaw: List<String>): Practice {
            val teachers: List<String> = java.util.List.copyOf(teachersRaw)
            SessionRules.checkActiveConstraints(date, topic, location, teachers)
            return Practice(date, topic, location, teachers)
        }
    }
}

class Exam private constructor(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    override val teachers: List<String>
) : BaseSession(), ActiveSession {
    override val sessionType: SessionType = SessionType.EXAM
    override fun createRescheduledSession(newDate: LocalDate) =
        Exam(newDate, topic, location, teachers)

    companion object {
        operator fun invoke(date: LocalDate, topic: String, location: String, teachersRaw: List<String>): Exam {
            val teachers: List<String> = java.util.List.copyOf(teachersRaw)
            SessionRules.checkActiveConstraints(date, topic, location, teachers, minTeachers=2)
            return Exam(date, topic, location, teachers)
        }
    }
}

class Cancelled private constructor(
    override val date: LocalDate,
    override val topic: String
) : BaseSession() {
    override val sessionType: SessionType = SessionType.CANCELLED

    companion object {
        operator fun invoke(date: LocalDate, topic: String): Cancelled {
            SessionRules.checkCommonConstraints(date, topic)
            return Cancelled(date, topic)
        }
    }
}
