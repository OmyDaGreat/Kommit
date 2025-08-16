package xyz.malefic.cli.cmd.gitsys

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.git

/**
 * Command for managing Git tags.
 */
class TagCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }

        when {
            args.isEmpty() -> listTags()
            args[0] == "-d" || args[0] == "--delete" -> {
                if (args.size > 1) {
                    deleteTag(args[1])
                } else {
                    echo("Error: Tag name required for delete", err = true)
                    showHelp()
                }
            }
            args[0] == "-l" || args[0] == "--list" -> listTags()
            args[0].startsWith("-") -> {
                echo("Error: Unknown option ${args[0]}", err = true)
                showHelp()
            }
            else -> {
                val tagName = args[0]
                val message =
                    args.indexOfFirst { it == "-m" || it == "--message" }.let { index ->
                        if (index >= 0 && index + 1 < args.size) {
                            args[index + 1]
                        } else {
                            null
                        }
                    }
                createTag(tagName, message)
            }
        }
    }

    override fun showHelp() {
        echo("Manage Git tags")
        echo("")
        echo("Usage: kommit tag [options] [tag-name]")
        echo("")
        echo("Options:")
        echo("  -l, --list          List all tags")
        echo("  -d, --delete NAME   Delete a tag")
        echo("  -m, --message MSG   Add a message to the tag")
        echo("  -h, --help          Show this help message")
        echo("")
        echo("Examples:")
        echo("  kommit tag                      # List all tags")
        echo("  kommit tag v1.0.0               # Create tag v1.0.0")
        echo("  kommit tag v1.0.0 -m \"Release\"  # Create annotated tag")
        echo("  kommit tag -d v1.0.0            # Delete tag v1.0.0")
    }

    private fun listTags() {
        try {
            val result = git("tag", "--list")

            if (result.exitCode == 0) {
                if (result.output.isEmpty()) {
                    echo("No tags found")
                } else {
                    echo("Tags:")
                    echo(result.output)
                }
            } else {
                echo("Failed to list tags. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error listing tags: ${e.message}", err = true)
        }
    }

    private fun createTag(
        tagName: String,
        message: String?,
    ) {
        try {
            val command = mutableListOf("tag")

            message?.let {
                command.add("-a")
                command.add(tagName)
                command.add("-m")
                command.add(it)
            } ?: run {
                command.add(tagName)
            }

            val result = git(*command.toTypedArray())

            if (result.exitCode == 0) {
                val tagType = if (message != null) "annotated tag" else "tag"
                echo("$tagType '$tagName' created successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to create tag '$tagName'. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error creating tag: ${e.message}", err = true)
        }
    }

    private fun deleteTag(tagName: String) {
        try {
            val result = git("tag", "-d", tagName)

            if (result.exitCode == 0) {
                echo("Tag '$tagName' deleted successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to delete tag '$tagName'. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error deleting tag: ${e.message}", err = true)
        }
    }
}
