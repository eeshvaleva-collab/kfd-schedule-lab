package schedule

import java.time.LocalDate

sealed interface Session {
    val date: LocalDate
    val topic: String
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

        val protectedTeachers = java.util.List.copyOf(teachers)
        require(protectedTeachers.none { it.isBlank() || it.trim() == "-" }) { "Invalid teacher name" }
        require(protectedTeachers.toSet().size == protectedTeachers.size) { "Teachers list cannot contain duplicates" }
        require(protectedTeachers.isNotEmpty()) { "Teachers list cannot be empty" }
        require(protectedTeachers.size >= minTeachers) { "At least $minTeachers teacher(s) required" }

        return protectedTeachers
    }
}

data class Lecture(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    private val teachersRaw: List<String>
) : ActiveSession {
        override val teachers: List<String> = SessionRules.checkActiveConstraints(date, topic, location, teachersRaw)
}

data class Practice(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    private val teachersRaw: List<String>
) : ActiveSession {
    override val teachers: List<String> = SessionRules.checkActiveConstraints(date, topic, location, teachersRaw)
}

data class Exam(
    override val date: LocalDate,
    override val topic: String,
    override val location: String,
    private val teachersRaw: List<String>
) : ActiveSession {
    override val teachers: List<String> = SessionRules.checkActiveConstraints(date, topic, location, teachersRaw, 2)
}

data class Cancellation(
    override val date: LocalDate,
    override val topic: String
) : Session {
    init {
        SessionRules.checkCommonConstraints(date, topic)
    }
}
