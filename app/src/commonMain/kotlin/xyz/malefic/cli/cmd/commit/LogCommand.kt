package xyz.malefic.cli.cmd.commit

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.git

/**
 * A command-line tool for displaying Git logs or generating a changelog.
 */
class LogCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }

        val count =
            args.indexOfFirst { it == "-n" || it == "--number" }.let { index ->
                if (index >= 0 && index + 1 < args.size) {
                    args[index + 1].toIntOrNull() ?: 10
                } else {
                    10
                }
            }

        val pretty = args.contains("-p") || args.contains("--pretty")
        val changelog = args.contains("-c") || args.contains("--changelog")

        val outputFile =
            args.indexOfFirst { it == "-o" || it == "--output" }.let { index ->
                if (index >= 0 && index + 1 < args.size) {
                    args[index + 1]
                } else {
                    "CHANGELOG.md"
                }
            }

        if (changelog) {
            generateChangelog(outputFile)
        } else {
            showLog(count, pretty)
        }
    }

    override fun showHelp() {
        echo("Display Git logs or generate a changelog")
        echo("")
        echo("Usage: kommit log [options]")
        echo("")
        echo("Options:")
        echo("  -n, --number NUM    Number of commits to show (default: 10)")
        echo("  -p, --pretty        Use pretty format")
        echo("  -c, --changelog     Generate a changelog")
        echo("  -o, --output FILE   Output file path for changelog (default: CHANGELOG.md)")
        echo("  -h, --help          Show this help message")
    }

    /**
     * Displays Git logs in the terminal.
     */
    private fun showLog(
        count: Int,
        pretty: Boolean,
    ) {
        try {
            val command = mutableListOf("log", "-n", count.toString())
            if (pretty) {
                command.add("--pretty=format:%C(yellow)%h%Creset %C(green)%ad%Creset | %s %C(red)[%an]%Creset")
                command.add("--date=short")
            }

            val result = git(*command.toTypedArray())

            if (result.exitCode == 0) {
                if (result.output.isEmpty()) {
                    echo("No commits found.")
                } else {
                    echo("Recent Git Commits:")
                    echo(result.output)
                }
            } else {
                echo("Failed to show log. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error showing log: ${e.message}", err = true)
        }
    }

    /**
     * Generates a changelog file based on commit messages.
     */
    private fun generateChangelog(outputFile: String) {
        try {
            val result = git("log", "--pretty=format:%s")

            if (result.exitCode != 0) {
                echo("Failed to get commit messages. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
                return
            }

            val commits = result.output.lines().filter { it.isNotBlank() }
            if (commits.isEmpty()) {
                echo("No commits found.")
                return
            }

            val changelog = buildChangelog(commits)

            // For now, just display the changelog instead of writing to file
            // TODO: Implement file writing when kotlinx-io is properly configured
            echo("Generated changelog content:")
            echo(changelog)
            echo("Note: File writing will be implemented with proper kotlinx-io setup")
        } catch (e: Exception) {
            echo("Error generating changelog: ${e.message}", err = true)
        }
    }

    /**
     * Builds a changelog string from a list of commit messages.
     */
    private fun buildChangelog(commits: List<String>): String =
        buildString {
            appendLine("# Changelog")
            appendLine()
            appendLine("## Latest Changes")
            appendLine()

            val typeMap = mutableMapOf<String, MutableList<String>>()
            val regex = Regex("^(\\w+)(\\(.*\\))?(!)?:(.+)$")

            commits.forEach { commit ->
                regex.find(commit)?.apply {
                    val (type, _, _, description) = destructured
                    typeMap.getOrPut(type) { mutableListOf() }.add(description.trim())
                }
            }

            typeMap.forEach { (type, messages) ->
                val header =
                    when (type) {
                        "feat" -> "Features"
                        "fix" -> "Bug Fixes"
                        "docs" -> "Documentation"
                        "style" -> "Styling"
                        "refactor" -> "Refactors"
                        "perf" -> "Performance"
                        "test" -> "Tests"
                        "build" -> "Build"
                        "ci" -> "CI"
                        "chore" -> "Chores"
                        else -> type.replaceFirstChar { it.uppercaseChar() }
                    }

                appendLine("### $header")
                appendLine()
                messages.forEach { message -> appendLine("- $message") }
                appendLine()
            }
        }
}
