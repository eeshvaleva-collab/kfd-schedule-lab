package schedule

interface SessionRepository {
    fun add(session: Session)
    fun addAll(sessions: List<Session>)
    fun all(): List<Session>
}

class SessionRepositoryImpl() : SessionRepository {
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

@JvmInline
value class OperationType private constructor(val value: String) {
    companion object {
        val ADD = OperationType("add")
        val ADD_ALL = OperationType("addAll")
    }
}

data class AuditEvent(
    val operation: OperationType,
    val count : Int
)
data class Stats(
    var sessions: Int = 0,
    var added: Int = 0,
    var auditEvents: Int = 0
)

class AuditedSessionRepository(
    private val delegate: SessionRepository
) : SessionRepository by delegate {
    val stats = Stats()
    var auditLog: List<AuditEvent> = emptyList()
        private set


    override fun add(session: Session) {
        delegate.add(session)

        stats.sessions += 1
        stats.added += 1
        stats.auditEvents += 1

        val updatedLog = mutableListOf<AuditEvent>().apply {
            addAll(auditLog)
            add(AuditEvent(operation = OperationType.ADD, count = 1))
        }
        auditLog = java.util.List.copyOf(updatedLog)
    }

    override fun addAll(sessions: List<Session>) {
        this.delegate.addAll(sessions)

        val count = sessions.size

        stats.sessions += count
        stats.added += count
        stats.auditEvents += 1

        val updatedLog = mutableListOf<AuditEvent>().apply {
            addAll(auditLog)
            add(AuditEvent(operation = OperationType.ADD_ALL, count = count))
        }
        auditLog = java.util.List.copyOf(updatedLog)
    }
}