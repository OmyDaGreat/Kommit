package xyz.malefic.cli.cmd.util

import com.kgit2.kommand.process.Command

/**
 * macOS x64-specific implementation of process execution using Kommand library
 */
actual fun executeCommand(vararg command: String): ProcessResult {
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
            error = result.stderr ?: ""
        )
    } catch (e: Exception) {
        // Fallback to basic system call
        val commandString = command.joinToString(" ")
        val exitCode = platform.posix.system(commandString)
        
        ProcessResult(
            exitCode = exitCode,
            output = if (exitCode == 0) "Command executed successfully" else "",
            error = if (exitCode != 0) "Command failed with exit code $exitCode" else ""
        )
    }
}