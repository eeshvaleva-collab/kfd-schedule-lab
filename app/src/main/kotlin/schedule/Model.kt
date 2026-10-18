package schedule

import java.time.LocalDate

sealed interface Session {
    val date: LocalDate
    val topic: String

    fun formString(showTeachers: Boolean = false): String {
        val typeName = this::class.simpleName?.uppercase()
        val locationStr = if (this is ActiveSession) this.location else "-"

        val base = "$date | $typeName | $topic | $locationStr"
        return if (showTeachers) {
            val teachersStr = if (this is ActiveSession) this.teachers.joinToString(", ") else "-"
            "$base | $teachersStr"
        } else base

    }
}

sealed interface ActiveSession : Session {
    val location: String
    val teachers: List<String>
}

internal object SessionRules {
    fun checkCommonConstraints(date: LocalDate, topic: String) {
        require(date.year in 1..9999) { "Year must be between 1 and 9999" }
        require(topic.isNotBlank()) { "Topic cannot be empty" }
        require(topic.trim() == topic) { "Topic cannot have leading or trailing spaces" }
        require(topic.none { it.isISOControl() }) { "Topic cannot contain control characters" }
    }

    fun checkActiveConstraints(date: LocalDate, topic: String, location: String, teachers: List<String>, minTeachers: Int = 1): List<String> {
        checkCommonConstraints(date, topic)
        require(location.isNotBlank() && location != "-") { "Location cannot be empty or '-'" }
        require(location.trim() == location) { "Location cannot have leading or trailing spaces" }

        require(teachers.none { it.isBlank() || it.trim() == "-" }) { "Invalid teacher name" }
        require(teachers.toSet().size == teachers.size) { "Teachers list cannot contain duplicates" }
        require(teachers.isNotEmpty()) { "Teachers list cannot be empty" }
        require(teachers.size >= minTeachers) { "At least $minTeachers teacher(s) required" }

        return teachers
    }
}

data class Lecture(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    private val teachersRaw: List<String>
) : ActiveSession {
        override val teachers: List<String> = java.util.List.copyOf(teachersRaw)
        init {
            SessionRules.checkActiveConstraints(date, topic, location, teachers)
        }
}

data class Practice(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    private val teachersRaw: List<String>
) : ActiveSession {
    override val teachers: List<String> = java.util.List.copyOf(teachersRaw)
    init {
        SessionRules.checkActiveConstraints(date, topic, location, teachers)
    }
}

data class Exam(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    private val teachersRaw: List<String>
) : ActiveSession {
    override val teachers: List<String> = java.util.List.copyOf(teachersRaw)
    init {
        SessionRules.checkActiveConstraints(date, topic, location, teachers, 2)
    }
}

data class Cancelled(
    override val date: LocalDate,
    override val topic: String
) : Session {
    init {
        SessionRules.checkCommonConstraints(date, topic)
    }
}
