package schedule

import kotlin.test.Test
import kotlin.test.assertIs

class ParserValidationTest {

    private val parser = Parser()

    @Test
    fun `should fail when field separator is missing`() {
        val line = "2026-10-05LECTURETopicRoom312Portnov"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.MissingSeparator>(result.error)
    }

    @Test
    fun `should fail when field count is not five`() {
        val line = "2026-10-05|LECTURE|Topic|Room312"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.InvalidFieldCount>(result.error)
    }

    @Test
    fun `should fail when required field is empty`() {
        val line = "|LECTURE|Topic|Room312|Portnov"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.EmptyValue>(result.error)
    }

    @Test
    fun `should fail when date format is invalid`() {
        val line = "2026-13-45|LECTURE|Topic|Room312|Portnov"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.EmptyOrInvalidDate>(result.error)
    }

    @Test
    fun `should fail when year starts with four zeros`() {
        val line = "0000-10-05|LECTURE|Topic|Room 312|Portnov"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.EmptyOrInvalidDate>(result.error)
    }

    @Test
    fun `should fail when session type is unknown`() {
        val line = "2026-10-05|SEMINAR|Topic|Room 312|Portnov"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.UnknownSessionType>(result.error)
    }

    @Test
    fun `should prioritize date error over unknown session type`() {
        val line = "2026-13-45|SEMINAR|Topic|Room 312|Portnov"
        val result = parser.parse(line)

        assertIs<ParseResult.FailureParse>(result)
        assertIs<ParseError.EmptyOrInvalidDate>(result.error)
    }
}
