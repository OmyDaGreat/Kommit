@file:Suppress("ktlint:standard:filename")

package xyz.malefic.cli.cmd.util

import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio

/**
 * The outcome of running an external command. [exitCode] is -1 if the command could not be started
 * or was killed by a signal.
 */
data class ProcessResult(
    val exitCode: Int,
    val stdout: String = "",
    val stderr: String = "",
) {
    val success get() = exitCode == 0

    /** The text worth showing the user: stdout on success, otherwise stderr (or stdout if stderr is empty). */
    val message get() = (if (success) stdout else stderr.ifBlank { stdout }).trim()

    /** Non-blank lines of stdout, or an empty list if the command failed. */
    val lines get() = if (success) stdout.lines().filter { it.isNotBlank() } else emptyList()
}

/**
 * Run a command and capture its output. Stdin receives [input] if given and is closed otherwise, so the command can never block on or read from the terminal Kotter is using.
 *
 * Output must not contain NUL bytes: kommand passes it through a C string and aborts the process otherwise, so avoid flags like `git -z`.
 */
fun runCommand(
    vararg command: String,
    input: String? = null,
): ProcessResult =
    spawn(command) {
        stdout(Stdio.Pipe).stderr(Stdio.Pipe)
        val output =
            if (input == null) {
                stdin(Stdio.Null).output()
            } else {
                stdin(Stdio.Pipe).spawn().run {
                    bufferedStdin()?.apply {
                        writeLine(input)
                        flush()
                    }
                    waitWithOutput()
                }
            }
        ProcessResult(output.status ?: -1, output.stdout.orEmpty(), output.stderr.orEmpty())
    }

/**
 * Run a command attached to the real terminal, so it can prompt the user (pinentry, credentials, editors).
 * Must not be called while a Kotter session is active — see [xyz.malefic.cli.cmd.BaseCommand.afterSession].
 */
fun runInteractive(vararg command: String): ProcessResult =
    spawn(command) {
        ProcessResult(stdin(Stdio.Inherit).stdout(Stdio.Inherit).stderr(Stdio.Inherit).status())
    }

private inline fun spawn(
    command: Array<out String>,
    block: Command.() -> ProcessResult,
): ProcessResult {
    if (command.isEmpty()) return ProcessResult(-1, stderr = "No command provided")
    return try {
        Command(command[0]).args(command.drop(1)).block()
    } catch (e: Exception) {
        ProcessResult(-1, stderr = "Could not run '${command[0]}': ${e.message}")
    }
}
