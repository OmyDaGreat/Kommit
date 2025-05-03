package xyz.malefic.cli.cmd.gitsys

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.mordant.rendering.TextColors
import xyz.malefic.cli.cmd.util.git

/**
 * A command for displaying the working tree status of a Git repository.
 *
 * This command runs the `git status` command to show the current state of
 * the working directory and staging area.
 */
class StatusCommand :
    CliktCommand(
        name = "status",
    ) {
    /**
     * Provides a custom help message for the `status` command.
     *
     * @param context The Clikt command context.
     * @return A string containing the help message.
     */
    override fun help(context: Context): String = context.theme.info(TextColors.brightBlue("Show the working tree status"))

    /**
     * Executes the `status` command.
     *
     * This method displays a message indicating that the Git status is being
     * checked and then calls the `showStatus` method to execute the `git status` command.
     */
    override fun run() {
        echo(TextColors.brightBlue("Checking git status..."))
        showStatus()
    }

    /**
     * Runs the `git status` command to display the working tree status.
     *
     * This method uses a `ProcessBuilder` to execute the `git status` command.
     * It redirects the output and error streams to the console. If an error
     * occurs during execution, an error message is displayed.
     *
     * @throws Exception If an error occurs while executing the `git status` command.
     */
    private fun showStatus() {
        try {
            val statusProcess = git("status")

            statusProcess.waitFor()
        } catch (e: Exception) {
            echo(TextColors.red("Error showing status: ${e.message}"), err = true)
        }
    }
}
