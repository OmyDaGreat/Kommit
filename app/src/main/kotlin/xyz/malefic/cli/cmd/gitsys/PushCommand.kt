package xyz.malefic.cli.cmd.gitsys

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import xyz.malefic.cli.cmd.util.git

/**
 * Command for pushing commits to the remote repository.
 */
class PushCommand :
    CliktCommand(
        name = "push",
    ) {
    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context): String = context.theme.info(brightBlue("Push commits to the remote repository"))

    /**
     * Executes the command to push commits.
     */
    override fun run() {
        pushCommits()
    }
}

/**
 * Pushes commits to the remote repository.
 * Executes 'git push'.
 */
fun CliktCommand.pushCommits() =
    try {
        echo(brightBlue("Pushing commits to remote..."))
        val pushProcess = git("push")

        val exitCode = pushProcess.waitFor()
        when (exitCode) {
            0 -> echo(green("Commits pushed successfully!"))
            else -> echo(red("Failed to push commits. Exit code: $exitCode"), err = true)
        }
    } catch (e: Exception) {
        echo(red("Error pushing commits: ${e.message}"), err = true)
    }
