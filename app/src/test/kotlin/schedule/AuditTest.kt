package schedule

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditedRepositoryTest {

    private val validDate = LocalDate.of(2026, 10, 5)
    private val session1 = Cancelled(validDate, "Topic 1")
    private val session2 = Cancelled(validDate, "Topic 2")

    @Test
    fun `should log single addition and update stats correctly`() {
        val baseRepo = SessionRepositoryImpl()
        val auditedRepo = AuditedSessionRepository(baseRepo)

        auditedRepo.add(session1)

        val stats = auditedRepo.stats
        assertEquals(1, stats.sessions)
        assertEquals(1, stats.added)
        assertEquals(1, stats.auditEvents)

        assertEquals(1, auditedRepo.auditLog.size)
        val event = auditedRepo.auditLog.first()
        assertEquals(OperationType.ADD, event.operation)
        assertEquals(1, event.count)
    }

    @Test
    fun `should log batch addition with total size and single event`() {
        val baseRepo = SessionRepositoryImpl()
        val auditedRepo = AuditedSessionRepository(baseRepo)
        val batch = listOf(session1, session2)

        auditedRepo.addAll(batch)

        val stats = auditedRepo.stats
        assertEquals(2, stats.sessions)
        assertEquals(2, stats.added)
        assertEquals(1, stats.auditEvents)

        assertEquals(1, auditedRepo.auditLog.size)
        val event = auditedRepo.auditLog.first()
        assertEquals(OperationType.ADD_ALL, event.operation)
        assertEquals(2, event.count)
    }

    @Test
    fun `should log empty batch addition as an event with zero count`() {
        val baseRepo = SessionRepositoryImpl()
        val auditedRepo = AuditedSessionRepository(baseRepo)

        auditedRepo.addAll(emptyList())

        val stats = auditedRepo.stats
        assertEquals(0, stats.sessions)
        assertEquals(0, stats.added)
        assertEquals(1, stats.auditEvents)

        assertEquals(1, auditedRepo.auditLog.size)
        val event = auditedRepo.auditLog.first()
        assertEquals(OperationType.ADD_ALL, event.operation)
        assertEquals(0, event.count)
    }

    @Test
    fun `should not produce audit events during all operation and keep snapshot immutable`() {
        val baseRepo = SessionRepositoryImpl()
        val auditedRepo = AuditedSessionRepository(baseRepo)

        auditedRepo.add(session1)
        val sizeBeforeAll = auditedRepo.auditLog.size

        val snapshot = auditedRepo.all()

        assertEquals(sizeBeforeAll, auditedRepo.auditLog.size)

        auditedRepo.add(session2)
        assertEquals(1, snapshot.size)
    }
}
