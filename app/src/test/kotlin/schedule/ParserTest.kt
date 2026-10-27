package schedule

import kotlin.test.Test
import kotlin.test.assertIs
import java.time.LocalDate
import kotlin.test.assertEquals

class ParserValidationTest {
    private val parser = Parser()

    @Test
    fun `should successfully parse valid lecture`() {
        val line = "2026-10-05|LECTURE|abcde fghij|klmno 123|pqrst"
        val result = parser.parse(line)

        assertIs<ParseResult.SuccessParse<Session>>(result)
        val session = result.value
        assertIs<Lecture>(session)
        assertEquals(LocalDate.of(2026, 10, 5), session.date)
        assertEquals("abcde fghij", session.topic)
        assertEquals("klmno 123", session.location)
        assertEquals(listOf("pqrst"), session.teachers)
    }

    @Test
    fun `should successfully parse valid practice`() {
        val line = "2026-10-08|PRACTICE|uvwxy zabc|defgh 456|ij"
        val result = parser.parse(line)

        assertIs<ParseResult.SuccessParse<Session>>(result)
        val session = result.value
        assertIs<Practice>(session)
        assertEquals(LocalDate.of(2026, 10, 8), session.date)
        assertEquals("uvwxy zabc", session.topic)
        assertEquals("defgh 456", session.location)
        assertEquals(listOf("ij"), session.teachers)
    }

    @Test
    fun `should successfully parse valid exam with multiple teachers`() {
        val line = "2026-10-15|EXAM|klmno pqrst|uvwxy 789|abc,def"
        val result = parser.parse(line)

        assertIs<ParseResult.SuccessParse<Session>>(result)
        val session = result.value
        assertIs<Exam>(session)
        assertEquals(LocalDate.of(2026, 10, 15), session.date)
        assertEquals("klmno pqrst", session.topic)
        assertEquals("uvwxy 789", session.location)
        assertEquals(listOf("abc", "def"), session.teachers)
    }

    @Test
    fun `should successfully parse valid cancelled session with dashes`() {
        val line = "2026-10-19|CANCELLED|ghijk lmnop|-|-"
        val result = parser.parse(line)

        assertIs<ParseResult.SuccessParse<Session>>(result)
        val session = result.value
        assertIs<Cancelled>(session)
        assertEquals(LocalDate.of(2026, 10, 19), session.date)
        assertEquals("ghijk lmnop", session.topic)
    }

    @Test
    fun `should successfully parse values with allowed internal spaces`() {
        val line = "2026-11-01|LECTURE|qwert asdfg zxcvb|poiuy mnbvc 777|lkjh gfdsa"
        val result = parser.parse(line)

        assertIs<ParseResult.SuccessParse<Session>>(result)
        val session = result.value
        assertIs<Lecture>(session)
        assertEquals("qwert asdfg zxcvb", session.topic)
        assertEquals("poiuy mnbvc 777", session.location)
        assertEquals(listOf("lkjh gfdsa"), session.teachers)
    }

    @Test
    fun `should fail when field separator is missing`() {
        val line = "2026-10-05LECTUREabcdefghijk"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.MissingSeparator>(result.error)
    }

    @Test
    fun `should fail when field count is not five`() {
        val line = "2026-10-05|LECTURE|abcde|fgh"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.InvalidFieldCount>(result.error)
    }

    @Test
    fun `should fail when required field is empty`() {
        val line = "|LECTURE|abcde|fghij|klmno"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.EmptyValue>(result.error)
    }

    @Test
    fun `should fail when date format is invalid`() {
        val line = "2026-13-45|LECTURE|abcde|fghij|klmno"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.EmptyOrInvalidDate>(result.error)
    }

    @Test
    fun `should fail when year starts with four zeros`() {
        val line = "0000-10-05|LECTURE|abcde|fghij|klmno"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.EmptyOrInvalidDate>(result.error)
    }

    @Test
    fun `should fail when session type is unknown`() {
        val line = "2026-10-05|XYZWVU|abcde|fghij|klmno"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.UnknownSessionType>(result.error)
    }

    @Test
    fun `should prioritize date error over unknown session type`() {
        val line = "2026-13-45|XYZWVU|abcde|fghij|klmno"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.EmptyOrInvalidDate>(result.error)
    }

    @Test
    fun `should fail with trailing whitespace error when teachers list contains empty spaces between commas`() {
        val line = "2026-10-15|LECTURE|Valid Lecture|Room 401|Portnov, ,Sidorov"
        val result = parser.parse(line)
        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.OthersViolation.HasTrailingOrLeadingWhitespaces>(result.error)
    }

    @Test
    fun `should fail with invalid teachers name when teachers name is - symbol`() {
        val line = "2026-10-15|LECTURE|Valid Lecture|Room 401|Portnov,-,Sidorov"
        val result = parser.parse(line)
        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.OthersViolation.InvalidTeacherName>(result.error)
    }

    @Test
    fun `should prioritize field count error over empty field values`() {
        val line = "2026-10-05||Topic|||ExtraField"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.InvalidFieldCount>(result.error)
        assertEquals(6, result.error.rawValue.split('|').size)
    }

    @Test
    fun `should format logged message with correct escaping for control characters and quotes`() {
        val errorWithQuotes = ParseError.EmptyValue("topic\"name")
        assertEquals("empty required field \"topic\\\"name\"", errorWithQuotes.logMessage())

        val errorWithControlChar = ParseError.UnknownSessionType("ABC\tDEF")
        assertEquals("unknown session type \"ABC\\u0009DEF\"", errorWithControlChar.logMessage())
    }
}
