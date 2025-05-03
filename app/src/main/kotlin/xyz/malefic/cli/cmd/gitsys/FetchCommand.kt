package xyz.malefic.cli.cmd.gitsys

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import xyz.malefic.cli.cmd.util.git

/**
 * A command that fetches changes from a remote Git repository.
 *
 * This command provides options to fetch from a specific remote, all remotes,
 * or the default remote. It also supports pruning remote-tracking branches
 * that no longer exist.
 */
class FetchCommand :
    CliktCommand(
        name = "fetch",
    ) {
    /**
     * The name of the remote to fetch from, if specified.
     */
    private val remote by argument(help = "Remote to fetch from").optional()

    /**
     * A flag indicating whether to fetch from all remotes.
     */
    private val all by option("-a", "--all", help = "Fetch from all remotes").flag()

    /**
     * A flag indicating whether to prune remote-tracking branches that no longer exist.
     */
    @Suppress("kotlin:S1192")
    private val prune by option("-p", "--prune", help = "Remove remote-tracking branches that no longer exist").flag()

    /**
     * Provides a custom help message for the `fetch` command.
     *
     * @param context The Clikt command context.
     * @return A string containing the help message.
     */
    override fun help(context: Context): String = context.theme.info(brightBlue("Fetch from a remote repository"))

    /**
     * Executes the `fetch` command.
     *
     * Determines the appropriate fetch operation based on the provided arguments
     * and options, and then calls the corresponding method.
     */
    override fun run() {
        when {
            all -> fetchAll(prune)
            remote != null -> fetchRemote(remote!!, prune)
            else -> fetchDefaultRemote(prune)
        }
    }

    /**
     * Fetches changes from all remotes.
     *
     * @param prune Whether to prune remote-tracking branches.
     */
    private fun fetchAll(prune: Boolean) {
        try {
            val command = mutableListOf("fetch", "--all")
            if (prune) command.add("--prune")

            val fetchProcess = git(*command.toTypedArray())

            val exitCode = fetchProcess.waitFor()
            if (exitCode == 0) {
                echo(green("Fetched from all remotes successfully!"))
            } else {
                echo(red("Failed to fetch from all remotes. Exit code: $exitCode"), err = true)
            }
        } catch (e: Exception) {
            echo(red("Error fetching from remotes: ${e.message}"), err = true)
        }
    }

    /**
     * Fetches changes from a specific remote.
     *
     * @param remote The name of the remote to fetch from.
     * @param prune Whether to prune remote-tracking branches.
     */
    private fun fetchRemote(
        remote: String,
        prune: Boolean,
    ) {
        try {
            val command = mutableListOf("fetch", remote)
            if (prune) command.add("--prune")

            val fetchProcess = git(*command.toTypedArray())

            val exitCode = fetchProcess.waitFor()
            if (exitCode == 0) {
                echo(green("Fetched from '$remote' successfully!"))
            } else {
                echo(red("Failed to fetch from '$remote'. Exit code: $exitCode"), err = true)
            }
        } catch (e: Exception) {
            echo(red("Error fetching from remote: ${e.message}"), err = true)
        }
    }

    /**
     * Fetches changes from the default remote.
     *
     * @param prune Whether to prune remote-tracking branches.
     */
    private fun fetchDefaultRemote(prune: Boolean) {
        try {
            val command = mutableListOf("fetch")
            if (prune) command.add("--prune")

            val fetchProcess = git(*command.toTypedArray())

            val exitCode = fetchProcess.waitFor()
            if (exitCode == 0) {
                echo(green("Fetched from default remote successfully!"))
            } else {
                echo(red("Failed to fetch from default remote. Exit code: $exitCode"), err = true)
            }
        } catch (e: Exception) {
            echo(red("Error fetching from default remote: ${e.message}"), err = true)
        }
    }
}
