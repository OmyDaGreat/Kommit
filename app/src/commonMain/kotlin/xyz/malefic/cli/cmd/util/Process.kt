package xyz.malefic.cli.cmd.util

/**
 * Cross-platform process execution utility for Kotlin/Native
 */
expect fun executeCommand(vararg command: String): ProcessResult

/**
 * Result of a process execution
 */
data class ProcessResult(
    val exitCode: Int,
    val output: String,
    val error: String
)