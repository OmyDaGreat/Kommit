package xyz.malefic.cli.cmd.gitsys

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
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
     * The remote repository to push to (e.g., 'origin').
     * If not specified, the default remote will be used.
     */
    private val remote by argument(help = "Remote repository to push to").optional()

    /**
     * The branch to push to (e.g., 'main', 'feature/amazing-feature').
     * If not specified, the current branch will be pushed.
     */
    private val branch by argument(help = "Branch to push to").optional()

    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context): String =
        context.theme.info(
            brightBlue(
                "Push commits to the remote repository, optionally specifying remote and branch",
            ),
        )

    /**
     * Executes the command to push commits.
     */
    override fun run() {
        pushCommits(remote, branch)
    }
}

/**
 * Pushes commits to the remote repository.
 * Executes 'git push' with optional remote and branch arguments.
 *
 * @param remote The remote repository to push to (e.g., 'origin'). If null, the default remote will be used.
 * @param branch The branch to push to (e.g., 'main', 'feature/amazing-feature'). If null, the current branch will be pushed.
 */
fun CliktCommand.pushCommits(
    remote: String? = null,
    branch: String? = null,
) = try {
    val command = mutableListOf("push")

    if (remote != null) {
        command.add(remote)
        if (branch != null) {
            command.add(branch)
        }
    }

    echo(brightBlue("Pushing commits to remote${remote?.let { " $it" } ?: ""}${branch?.let { " branch $it" } ?: ""}..."))
    val pushProcess = git(*command.toTypedArray())

    val exitCode = pushProcess.waitFor()
    when (exitCode) {
        0 -> echo(green("Commits pushed successfully!"))
        else -> echo(red("Failed to push commits. Exit code: $exitCode"), err = true)
    }
} catch (e: Exception) {
    echo(red("Error pushing commits: ${e.message}"), err = true)
}
