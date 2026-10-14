package schedule

import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface Session {
    val date: LocalDate
    val topic: String
    companion object {
        fun checkCommonConstraints(date: LocalDate, topic: String) {
            require(date.year in 1..9999) { "Year must be between 1 and 9999" }
            require(topic.isNotBlank()) { "Topic cannot be empty" }
            require(topic.trim() == topic) { "Topic cannot have leading or trailing spaces" }
            require(topic.none { it.isISOControl() }) { "Topic cannot contain control characters" }
        }
    }

}

sealed interface ActiveSession : Session {
    val location: String
    val teachers: List<String>
    companion object {
        fun checkActiveConstraints(date: LocalDate, topic: String, location: String, teachers: List<String>) {
            Session.checkCommonConstraints(date, topic)
            require(location.isNotBlank() && location != "-") { "Location cannot be empty or '-'" }
            require(location.trim() == location) { "Location cannot have leading or trailing spaces" }
            require(teachers.none { it.isBlank() || it.trim() == "-" }) { "Invalid teacher name" }
            require(teachers.toSet().size == teachers.size) { "Teachers list cannot contain duplicates" }
            require(teachers.isNotEmpty()) { "Teachers list cannot be empty" }
        }
    }
}

abstract class BaseEvent : Session{
    override fun toString(): String {
        val className = javaClass.simpleName
        return "$className(date=$date, topic='$topic')"
    }
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is ActiveSession -> false
        javaClass != other.javaClass -> false
        else -> date == other.date && topic == other.topic
    }

    override fun hashCode(): Int = 31 * date.hashCode() + topic.hashCode()
}

abstract class BaseActiveEvent : ActiveSession{
    override fun toString(): String {
        val className = javaClass.simpleName
        return "$className(date=$date, topic='$topic', location='$location', teachers=$teachers)"
    }
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is ActiveSession -> false
        javaClass != other.javaClass -> false
        else -> date == other.date && topic == other.topic && location == other.location && teachers == other.teachers
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + topic.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + teachers.hashCode()
        return result
    }

}

class Lecture private constructor(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    override val teachers: List<String>
) : BaseActiveEvent() {
    companion object {
        fun create(date: LocalDate, topic: String, location: String, teachers: List<String>): Lecture {
            val protectedTeachers : List<String> = java.util.List.copyOf(teachers) as List<String>
            ActiveSession.checkActiveConstraints(date, topic, location, protectedTeachers)
            return Lecture(date, topic, location, protectedTeachers)
        }
    }

}

class Practice private constructor(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    override val teachers: List<String>
) : BaseActiveEvent() {
    companion object {
        fun create(date: LocalDate, topic: String, location: String, teachers: List<String>): Practice {
            val protectedTeachers : List<String> = java.util.List.copyOf(teachers) as List<String>
            ActiveSession.checkActiveConstraints(date, topic, location, protectedTeachers)
            return Practice(date, topic, location, protectedTeachers)
        }
    }
}

class Exam private constructor(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    override val teachers: List<String>
) : BaseActiveEvent() {
    companion object {
        fun create(date: LocalDate, topic: String, location: String, teachers: List<String>): Exam {
            val protectedTeachers : List<String> = java.util.List.copyOf(teachers) as List<String>
            ActiveSession.checkActiveConstraints(date, topic, location, protectedTeachers)
            require(protectedTeachers.size > 1) {"Exam requires at least two different teachers"}
            return Exam(date, topic, location, protectedTeachers)
        }
    }
}

class Cancellation private constructor(
    override val date: LocalDate,
    override val topic: String
) : BaseEvent() {
    companion object {
        fun create(date: LocalDate, topic: String): Cancellation {
            Session.checkCommonConstraints(date, topic)
            return Cancellation(date, topic)
        }
    }
}
