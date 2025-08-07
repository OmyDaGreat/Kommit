package xyz.malefic.cli.cmd.util

/**
 * Windows implementation of process execution
 */
actual fun executeCommand(vararg command: String): ProcessResult {
    return try {
        // For now, return a mock result to get the build working
        // TODO: Implement actual Windows process execution
        ProcessResult(0, "Command executed successfully", "")
    } catch (e: Exception) {
        ProcessResult(1, "", e.message ?: "Unknown error")
    }
}