@file:Suppress("ktlint:standard:filename")

package xyz.malefic.cli.cmd.util

import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio

/**
 * Cross-platform process execution utility
 * Currently using expect/actual pattern until kommand integration is complete
 */
fun executeCommand(vararg command: String): ProcessResult {
    return try {
        if (command.isEmpty()) {
            return ProcessResult(1, "", "No command provided")
        }

        val baseCommand = command[0]
        val args = command.drop(1)

        val cmd = Command(baseCommand)
        if (args.isNotEmpty()) {
            cmd.args(*args.toTypedArray())
        }

        val result = cmd.stdout(Stdio.Inherit).output()

        ProcessResult(
            exitCode = 0,
            output = result.stdout ?: "",
            error = result.stderr ?: "",
        )
    } catch (_: Exception) {
        val commandString = command.joinToString(" ")
        val exitCode = platform.posix.system(commandString)

        ProcessResult(
            exitCode = exitCode,
            output = if (exitCode == 0) "Command executed successfully" else "",
            error = if (exitCode != 0) "Command failed with exit code $exitCode" else "",
        )
    }
}

/**
 * Execute a git command with the specified arguments
 */
fun git(vararg args: String): ProcessResult = executeCommand("git", *args)

/**
 * Result of a process execution
 */
data class ProcessResult(
    val exitCode: Int,
    val output: String,
    val error: String,
)
