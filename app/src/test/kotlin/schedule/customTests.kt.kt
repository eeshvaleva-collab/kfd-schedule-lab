package schedule

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AuditedRepositoryTest {

    private val validDate = LocalDate.of(2026, 10, 15)

    private class InMemorySessionRepository : SessionRepository {
        private val storage = mutableListOf<Session>()
        override fun add(session: Session) {
            storage.add(session)
        }

        override fun addAll(sessions: List<Session>) {
            storage.addAll(sessions)
        }

        override fun all(): List<Session> = storage
    }

    @Test
    fun `should log single and batch additions and track stats correctly`() {
        val baseRepo = InMemorySessionRepository()
        val auditedRepo = AuditedSessionRepository(baseRepo)

        val s1 = Cancelled(validDate, "Cancelled Lecture")
        val s2 = Lecture(validDate, "Topic 1", "Room 1", listOf("Ivanov"))
        val s3 = Practice(validDate, "Topic 2", "Room 2", listOf("Petrov"))

        auditedRepo.add(s1)
        assertEquals(1, auditedRepo.stats.sessions)
        assertEquals(1, auditedRepo.stats.added)
        assertEquals(1, auditedRepo.stats.auditEvents)
        assertEquals(OperationType.ADD, auditedRepo.auditLog[0].operation)
        assertEquals(1, auditedRepo.auditLog[0].count)

        auditedRepo.addAll(listOf(s2, s3))
        assertEquals(3, auditedRepo.stats.sessions)
        assertEquals(3, auditedRepo.stats.added)
        assertEquals(2, auditedRepo.stats.auditEvents)
        assertEquals(OperationType.ADD_ALL, auditedRepo.auditLog[1].operation)
        assertEquals(OperationType.ADD_ALL, auditedRepo.auditLog[1].operation)
        assertEquals(2, auditedRepo.auditLog[1].count)

        // 3. Пакетное добавление пустого списка
        auditedRepo.addAll(emptyList())
        assertEquals(3, auditedRepo.stats.auditEvents)
        assertEquals(0, auditedRepo.auditLog[2].count)
    }

    @Test
    fun `should protect all snapshot from external modifications`() {
        val baseRepo = InMemorySessionRepository()
        val auditedRepo = AuditedSessionRepository(baseRepo)

        val initialSession = Cancelled(validDate, "Initial Session")
        auditedRepo.add(initialSession)

        val snapshotBefore = auditedRepo.all()
        assertEquals(1, snapshotBefore.size)

        val newSession = Lecture(validDate, "New Topic", "Room 1", listOf("Ivanov"))
        auditedRepo.add(newSession)

        assertEquals(1, snapshotBefore.size)
        assertFalse(snapshotBefore.contains(newSession))

        val snapshotAfter = auditedRepo.all()
        assertEquals(2, snapshotAfter.size)
        assertTrue(snapshotAfter.contains(newSession))
    }
}

