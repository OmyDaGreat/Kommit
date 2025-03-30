package xyz.malefic.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.lang.ProcessBuilder.Redirect.INHERIT

/**
 * Command for amending the last commit.
 */
class AmendCommand :
    CliktCommand(
        name = "amend",
    ) {
    /**
     * Option to keep the same commit message.
     */
    private val noEdit by option("--no-edit", help = "Keep the same commit message").flag()

    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context): String = context.theme.info("Amend the last commit")

    /**
     * Executes the command to amend the last commit.
     */
    override fun run() {
        amendCommit(noEdit)
    }

    /**
     * Amends the last commit.
     * @param noEdit If true, keeps the same commit message.
     */
    private fun amendCommit(noEdit: Boolean) {
        try {
            val command = mutableListOf("git", "commit", "--amend")
            if (noEdit) {
                command.add("--no-edit")
            }

            val amendProcess =
                ProcessBuilder(command)
                    .redirectOutput(INHERIT)
                    .redirectError(INHERIT)
                    .start()

            val exitCode = amendProcess.waitFor()
            if (exitCode == 0) {
                echo("Commit amended successfully!")
            } else {
                echo("Failed to amend commit. Exit code: $exitCode", err = true)
            }
        } catch (e: Exception) {
            echo("Error amending commit: ${e.message}", err = true)
        }
    }
}
