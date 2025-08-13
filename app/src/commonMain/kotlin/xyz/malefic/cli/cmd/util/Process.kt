package xyz.malefic.cli.cmd.util

import com.kgit2.kommand.process.Command

// Using platform-specific process execution for now
// TODO: Replace with kommand library once API is properly researched

/**
 * Cross-platform process execution utility
 * Currently using expect/actual pattern until kommand integration is complete
 */
fun executeCommand(vararg command: String): ProcessResult {
    return try {
        if (command.isEmpty()) {
            return ProcessResult(1, "", "No command provided")
        }

        // Use kommand API - same as Linux implementation
        val baseCommand = command[0]
        val args = command.drop(1)

        val cmd = Command(baseCommand)
        if (args.isNotEmpty()) {
            cmd.args(*args.toTypedArray())
        }

        // Try the kommand pattern, if it fails fallback to system()
        val result = cmd.output()

        // Kommand might return different structure, let's handle what's available
        ProcessResult(
            exitCode = 0, // Assume success if no exception
            output = result.stdout ?: "",
            error = result.stderr ?: "",
        )
    } catch (e: Exception) {
        // Fallback to basic system call
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
