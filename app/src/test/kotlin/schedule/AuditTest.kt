package schedule

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditedRepositoryTest {

    private val validDate = LocalDate.of(2026, 10, 5)
    private val session1 = Cancelled(validDate, "Topic 1")
    private val session2 = Cancelled(validDate, "Topic 2")
    private val dummySession = Cancelled(validDate, "Test Topic")

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

    @Test
    fun `should correctly accumulate sessions and events when starting with non-empty repository`() {
        val baseRepository = SessionRepositoryImpl()
        val lecture1 = Lecture(validDate, "Topic 1", "Room 1", listOf("Ivanov"))
        val lecture2 = Lecture(validDate, "Topic 2", "Room 2", listOf("Petrov"))

        baseRepository.add(lecture1)
        baseRepository.add(lecture2)

        val auditedRepository = AuditedSessionRepository(baseRepository)
        val newLecture = Lecture(validDate, "New Topic", "Room 3", listOf("Sidorov"))

        auditedRepository.add(newLecture)

        val stats = auditedRepository.stats
        assertEquals(3, auditedRepository.all().size)
        assertEquals(1, stats.added)
        assertEquals(1, stats.auditEvents)
    }

    @Test
    fun `stats returns zeros when repository is empty`() {
        val innerRepo = SessionRepositoryImpl()
        val auditedRepo = AuditedSessionRepository(innerRepo)

        val expected = Stats(sessions = 0, added = 0, auditEvents = 0)
        assertEquals(expected, auditedRepo.stats)
    }

    @Test
    fun `single add increments sessions, added count and logs event`() {
        val innerRepo = SessionRepositoryImpl()
        val auditedRepo = AuditedSessionRepository(innerRepo)

        auditedRepo.add(dummySession)

        val stats = auditedRepo.stats
        assertEquals(1, stats.sessions)
        assertEquals(1, stats.added)
        assertEquals(1, stats.auditEvents)

        assertEquals(1, auditedRepo.auditLog.size)
        assertEquals(OperationType.ADD, auditedRepo.auditLog[0].operation)
        assertEquals(1, auditedRepo.auditLog[0].count)
    }

    @Test
    fun `batch addAll increments statistics by batch size as a single event`() {
        val innerRepo = SessionRepositoryImpl()
        val auditedRepo = AuditedSessionRepository(innerRepo)
        val batch = listOf(dummySession, dummySession, dummySession)

        auditedRepo.addAll(batch)

        val stats = auditedRepo.stats
        assertEquals(3, stats.sessions)
        assertEquals(3, stats.added)
        assertEquals(1, stats.auditEvents)

        assertEquals(1, auditedRepo.auditLog.size)
        assertEquals(OperationType.ADD_ALL, auditedRepo.auditLog[0].operation)
        assertEquals(3, auditedRepo.auditLog[0].count)
    }

    @Test
    fun `addAll with empty list creates audit event with zero count`() {
        val innerRepo = SessionRepositoryImpl()
        val auditedRepo = AuditedSessionRepository(innerRepo)

        auditedRepo.addAll(emptyList())

        val stats = auditedRepo.stats
        assertEquals(0, stats.sessions)
        assertEquals(0, stats.added)
        assertEquals(1, stats.auditEvents)

        assertEquals(1, auditedRepo.auditLog.size)
        assertEquals(OperationType.ADD_ALL, auditedRepo.auditLog[0].operation)
        assertEquals(0, auditedRepo.auditLog[0].count)
    }
}
