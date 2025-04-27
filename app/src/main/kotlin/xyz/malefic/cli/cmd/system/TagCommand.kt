package xyz.malefic.cli.cmd.system

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
 * A command for creating or listing Git tags.
 *
 * This command allows users to create annotated or lightweight tags, or list
 * all existing tags in a Git repository.
 */
class TagCommand :
    CliktCommand(
        name = "tag",
    ) {
    /**
     * The name of the tag to create.
     * This argument is optional and is used when creating a new tag.
     */
    private val tagName by argument(help = "Tag name (e.g., v1.0.0)").optional()

    /**
     * A flag indicating whether to list all existing tags.
     */
    private val list by option("-l", "--list", help = "List all tags").flag()

    /**
     * The message for the tag, used when creating an annotated tag.
     */
    private val message by option("-m", "--message", help = "Tag message")

    /**
     * Provides a custom help message for the `tag` command.
     *
     * @param context The Clikt command context.
     * @return A string containing the help message.
     */
    override fun help(context: Context) = context.theme.info(brightBlue("Create or list tags"))

    /**
     * Executes the `tag` command.
     *
     * The `--list` flag lists all tags. Providing a tag name creates a new tag. If neither is specified, an error message appears.
     */
    override fun run() {
        when {
            list -> listTags()
            tagName != null -> createTag(tagName!!, message)
            else -> echo(red("Tag name required. Use --list to view existing tags."), err = true)
        }
    }

    /**
     * Lists all existing Git tags.
     *
     * This method runs the `git tag` command to display all tags in the
     * repository. If an error occurs, an error message is displayed.
     *
     * @throws Exception If an error occurs while listing tags.
     */
    private fun listTags() {
        try {
            val tagProcess =
                ProcessBuilder("git", "tag")
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            tagProcess.waitFor()
        } catch (e: Exception) {
            echo(red("Error listing tags: ${e.message}"), err = true)
        }
    }

    /**
     * Creates a new Git tag.
     *
     * This method runs the `git tag` command to create a new tag. If a
     * message is provided, an annotated tag is created. Otherwise, a
     * lightweight tag is created. If an error occurs, an error message is
     * displayed.
     *
     * @param name The name of the tag to create.
     * @param message The message for the tag (optional).
     * @throws Exception If an error occurs while creating the tag.
     */
    private fun createTag(
        name: String,
        message: String?,
    ) {
        try {
            val command = mutableListOf("tag")

            if (message != null) {
                command.add("-a")
                command.add(name)
                command.add("-m")
                command.add(message)
            } else {
                command.add(name)
            }

            val tagProcess = git(*command.toTypedArray())

            val exitCode = tagProcess.waitFor()
            if (exitCode == 0) {
                echo(green("Tag '$name' created successfully!"))
            } else {
                echo(red("Failed to create tag. Exit code: $exitCode"), err = true)
            }
        } catch (e: Exception) {
            echo(red("Error creating tag: ${e.message}"), err = true)
        }
    }
}
