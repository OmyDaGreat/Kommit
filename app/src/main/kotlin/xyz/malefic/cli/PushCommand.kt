package xyz.malefic.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import java.lang.ProcessBuilder.Redirect.INHERIT

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
    override fun help(context: Context): String = context.theme.info("Push commits to the remote repository")

    /**
     * Executes the command to push commits.
     */
    override fun run() {
        echo("Pushing commits to remote...")
        pushCommits()
    }

    /**
     * Pushes commits to the remote repository.
     * Executes 'git push'.
     */
    private fun pushCommits() {
        try {
            val pushProcess =
                ProcessBuilder("git", "push")
                    .redirectOutput(INHERIT)
                    .redirectError(INHERIT)
                    .start()

            val exitCode = pushProcess.waitFor()
            if (exitCode == 0) {
                echo("Commits pushed successfully!")
            } else {
                echo("Failed to push commits. Exit code: $exitCode", err = true)
            }
        } catch (e: Exception) {
            echo("Error pushing commits: ${e.message}", err = true)
        }
    }
}
