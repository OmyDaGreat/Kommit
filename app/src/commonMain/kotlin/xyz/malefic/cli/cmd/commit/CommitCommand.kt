package xyz.malefic.cli.cmd.commit

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.executeCommand

internal const val DEFAULT_CONFIG_PATH = ".kommit.yaml"

/**
 * Command for generating a commit message based on the configuration file.
 */
class CommitCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        val configPath = extractConfigPath(args)

        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }

        try {
            generateCommit(configPath)
        } catch (e: Exception) {
            echo("Error: ${e.message}", err = true)
        }
    }

    override fun showHelp() {
        echo("Generate a commit message")
        echo("")
        echo("Usage: kommit commit [options]")
        echo("")
        echo("Options:")
        echo("  -c, --config PATH   Path to the configuration file (default: .kommit.yaml)")
        echo("  -h, --help          Show this help message")
    }

    private fun extractConfigPath(args: Array<String>): String {
        val configIndex = args.indexOfFirst { it == "-c" || it == "--config" }
        return if (configIndex >= 0 && configIndex + 1 < args.size) {
            args[configIndex + 1]
        } else {
            DEFAULT_CONFIG_PATH
        }
    }

    private fun generateCommit(configPath: String) {
        echo("Generating conventional commit...")

        // For now, implement a simple interactive commit generator
        // TODO: Add YAML configuration parsing once file I/O is implemented

        val types =
            listOf(
                "fix" to "Bug fix",
                "feat" to "New feature",
                "docs" to "Documentation changes",
                "style" to "Code style changes",
                "refactor" to "Code refactoring",
                "test" to "Adding or modifying tests",
                "chore" to "Maintenance tasks",
            )

        echo("\nSelect the type of change:")
        types.forEachIndexed { index, (type, description) ->
            echo("${index + 1}. $type - $description")
        }

        print("Enter your choice (1-${types.size}): ")
        val choice = readLine()?.toIntOrNull()?.minus(1)

        if (choice == null || choice !in types.indices) {
            echo("Invalid selection.", err = true)
            return
        }

        val selectedType = types[choice].first

        print("\nEnter a scope (optional, press Enter to skip): ")
        val scope = readLine()?.trim() ?: ""

        print("\nEnter a short description: ")
        val description = readLine()?.trim() ?: ""

        if (description.isEmpty()) {
            echo("Description cannot be empty.", err = true)
            return
        }

        // Build commit message
        val commitMessage =
            buildString {
                append(selectedType)
                if (scope.isNotEmpty()) {
                    append("($scope)")
                }
                append(": $description")
            }

        echo("\nGenerated Commit Message:")
        echo(commitMessage)

        print("\nDo you want to commit with this message? (y/N): ")
        val response = readLine()?.lowercase()

        if (response == "y" || response == "yes") {
            commitChanges(commitMessage)
        } else {
            echo("Commit aborted.")
        }
    }

    private fun commitChanges(message: String) {
        try {
            val result = executeCommand("git", "commit", "-m", message)
            if (result.exitCode == 0) {
                echo("Commit created successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to create commit. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error creating commit: ${e.message}", err = true)
        }
    }
}
