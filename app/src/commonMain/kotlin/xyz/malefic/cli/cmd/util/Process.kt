package xyz.malefic.cli.cmd.util

// Using platform-specific process execution for now
// TODO: Replace with kommand library once API is properly researched

/**
 * Cross-platform process execution utility
 * Currently using expect/actual pattern until kommand integration is complete
 */
expect fun executeCommand(vararg command: String): ProcessResult

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
    val error: String
)