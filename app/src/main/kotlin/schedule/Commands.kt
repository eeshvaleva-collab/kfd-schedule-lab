package schedule

import java.time.LocalDate

interface CommandHandlers {
    fun period(command: Command.Period): Int
    fun byType(command: Command.ByType): Int
    fun errors(command: Command.Errors): Int
}

object StudentCommandHandlers : CommandHandlers {
    override fun period(command: Command.Period): Int = runPeriod(command)
    override fun byType(command: Command.ByType): Int = runType(command)
    override fun errors(command: Command.Errors): Int = runErrors(command)
}

fun runPeriod(command: Command.Period): Int {
    var status = 0
    var totalLines = 0L
    streamFile(command.file) { result ->
        if (result is FileLineResult.Failure) {
            status = 1
        }
        else if (result is FileLineResult.Success && result.session.date in command.from..command.to) {
            totalLines++
           println(result.session.formString())
        }
    }
    println("Total: $totalLines")
    return status
}

fun runType(command: Command.ByType): Int {
    var status = 0
    var totalLines = 0L

    streamFile(command.file) { result ->
        if (result is FileLineResult.Failure) {
            status = 1
        }
        else if (result is FileLineResult.Success && result.session::class.simpleName?.uppercase() == command.type.name) {
            println(result.session.formString(showTeachers = true))
            totalLines++
        }
    }
    println("Total: $totalLines")
    return status
}

fun runErrors(command: Command.Errors): Int {
    var totalLines = 0L
    var rejectedLines = 0L
    streamFile(command.file) { result ->
        totalLines++
        if (result is FileLineResult.Failure) {
            rejectedLines++
            println("line ${result.lineNumber}: ${result.error.logMessage()}")
        }
    }
    println("Rejected: $rejectedLines of $totalLines")
    return if (rejectedLines > 0) 1 else 0
}

private fun streamFile(filePath: String, action: (FileLineResult) -> Unit) {
    val parser = Parser()
    forEachScheduleLine(filePath) { number, line ->
        val result = when (val parseResult = parser.parse(line)) {
            is ParseResult.SuccessParse -> FileLineResult.Success(parseResult.value)
            is ParseResult.FailureParse -> FileLineResult.Failure(number, parseResult.error)
        }
        action(result)
    }
}

sealed interface FileLineResult {
    data class Success(val session: Session) : FileLineResult

    data class Failure(val lineNumber: Long, val error: ParseError) : FileLineResult
}