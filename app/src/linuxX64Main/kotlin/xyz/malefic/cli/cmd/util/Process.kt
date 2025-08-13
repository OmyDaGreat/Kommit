package xyz.malefic.cli.cmd.util

import com.kgit2.kommand.process.Command

/**
 * Cross-platform process execution utility using Kommand library
 * Currently exploring kommand API - will be improved incrementally
 */
actual fun executeCommand(vararg command: String): ProcessResult {
    return try {
        if (command.isEmpty()) {
            return ProcessResult(1, "", "No command provided")
        }
        
        // Try basic kommand usage - exploring API step by step
        val baseCommand = command[0]
        val args = command.drop(1)
        
        // For now, fall back to basic approach while researching kommand API
        val commandString = command.joinToString(" ")
        val exitCode = platform.posix.system(commandString)
        
        if (exitCode == 0) {
            ProcessResult(0, "Command executed successfully", "")
        } else {
            ProcessResult(exitCode, "", "Command failed with exit code $exitCode")
        }
    } catch (e: Exception) {
        ProcessResult(1, "", e.message ?: "Unknown error")
    }
}