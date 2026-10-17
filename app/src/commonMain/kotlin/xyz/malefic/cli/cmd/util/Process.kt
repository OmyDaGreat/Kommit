@file:Suppress("ktlint:standard:filename")

package xyz.malefic.cli.cmd.util

import com.kgit2.kommand.process.Command

/**
 * Cross-platform process execution utility
 * Uses kommand library for proper stdout/stderr capture
 */
fun executeCommand(vararg command: String): ProcessResult {
    return try {
        if (command.isEmpty()) {
            return ProcessResult.Error("No command provided")
        }

        val baseCommand = command[0]
        val args = command.drop(1)

        val cmd = Command(baseCommand)
        if (args.isNotEmpty()) {
            cmd.args(*args.toTypedArray())
        }

        val result = cmd.output()

        if (result.status == 0) {
            ProcessResult.Success(result.stdout.orEmpty())
        } else {
            ProcessResult.Error(
                output = result.stdout.orEmpty(),
            )
        }
    } catch (e: Exception) {
        try {
            val commandString = command.joinToString(" ")
            val exitCode = platform.posix.system(commandString)

            if (exitCode == 0) {
                ProcessResult.Success(output = "Command executed successfully")
            } else {
                ProcessResult.Error(
                    output = "Command failed with exit code $exitCode: ${e.message}",
                )
            }
        } catch (fallbackError: Exception) {
            ProcessResult.Error(
                output = "Command execution failed: ${e.message}, fallback failed: ${fallbackError.message}",
            )
        }
    }
}

sealed class ProcessResult(
    open val output: String,
) {
    data class Error(
        override val output: String,
    ) : ProcessResult(output)

    data class Success(
        override val output: String,
    ) : ProcessResult(output)
}
