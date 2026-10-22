package schedule

interface SessionRepository {
    fun add(session: Session)
    fun addAll(sessions: List<Session>)
    fun all(): List<Session>
}

class SessionRepositoryImpl : SessionRepository {
    private val sessions = mutableListOf<Session>()

    override fun add(session: Session){
        sessions.add(session)
    }
    override fun addAll(sessions: List<Session>){
        this.sessions.addAll(sessions)
    }
    override fun all(): List<Session>{
        return java.util.List.copyOf(sessions)
    }
}

enum class OperationType { ADD, ADD_ALL }

data class AuditEvent(
    val operation: OperationType,
    val count : Int
)
data class Stats(
    val sessions: Int = 0,
    val added: Int = 0,
    val auditEvents: Int = 0
)

class AuditedSessionRepository(
    private val delegate: SessionRepository
) : SessionRepository by delegate {
    private var currentStats = Stats()
    val stats: Stats
        get() = currentStats.copy(sessions = delegate.all().size)
    var auditLog: List<AuditEvent> = emptyList()
        private set


    override fun add(session: Session) {
        delegate.add(session)
        currentStats = currentStats.copy(
            sessions = currentStats.sessions + 1,
            added = currentStats.added + 1,
            auditEvents = currentStats.auditEvents + 1
        )

        val updatedLog = mutableListOf<AuditEvent>().apply {
            addAll(auditLog)
            add(AuditEvent(operation = OperationType.ADD, count = 1))
        }
        auditLog = java.util.List.copyOf(updatedLog)
    }

    override fun addAll(sessions: List<Session>) {
        this.delegate.addAll(sessions)

        val count = sessions.size
        currentStats = currentStats.copy(
            sessions = currentStats.sessions + count,
            added = currentStats.added + count,
            auditEvents = currentStats.auditEvents + 1
        )

        val updatedLog = mutableListOf<AuditEvent>().apply {
            addAll(auditLog)
            add(AuditEvent(operation = OperationType.ADD_ALL, count = count))
        }
        auditLog = java.util.List.copyOf(updatedLog)
    }
    override fun all(): List<Session> {
        return java.util.Collections.unmodifiableList(delegate.all().toMutableList())
    }
}

