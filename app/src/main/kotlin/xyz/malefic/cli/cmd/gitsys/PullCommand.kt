package xyz.malefic.cli.cmd.gitsys

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.mordant.rendering.TextColors
import xyz.malefic.cli.cmd.util.git

/**
 * A command that pulls changes from the remote Git repository.
 *
 * This command uses the `git pull` command to fetch and merge changes
 * from the remote repository into the current branch. It provides
 * feedback to the user about the success or failure of the operation.
 */
class PullCommand :
    CliktCommand(
        name = "pull",
    ) {
    /**
     * Provides a custom help message for the `pull` command.
     *
     * @param context The Clikt command context.
     * @return A string containing the help message.
     */
    override fun help(context: Context): String = context.theme.info(TextColors.brightBlue("Pull changes from the remote repository"))

    /**
     * Executes the `pull` command.
     *
     * This method displays a message indicating that the pull operation
     * is starting, then calls the `pullChanges` method to perform the
     * actual operation.
     */
    override fun run() {
        echo(TextColors.brightBlue("Pulling changes from remote..."))
        pullChanges()
    }

    /**
     * Executes the `git pull` command to fetch and merge changes from the remote repository.
     *
     * This method uses a `ProcessBuilder` to run the `git pull` command. It redirects
     * the output and error streams to the console and waits for the process to complete.
     * If the process exits with a non-zero code, an error message is displayed.
     *
     * @throws Exception If an error occurs while executing the `git pull` command.
     */
    private fun pullChanges() {
        try {
            val pullProcess = git("pull")

            val exitCode = pullProcess.waitFor()
            if (exitCode == 0) {
                echo(TextColors.green("Changes pulled successfully!"))
            } else {
                echo(TextColors.red("Failed to pull changes. Exit code: $exitCode"), err = true)
            }
        } catch (e: Exception) {
            echo(TextColors.red("Error pulling changes: ${e.message}"), err = true)
        }
    }
}
