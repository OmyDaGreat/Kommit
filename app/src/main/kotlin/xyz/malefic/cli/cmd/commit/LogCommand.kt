package xyz.malefic.cli.cmd.commit

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import xyz.malefic.cli.cmd.util.gitPipe
import java.io.File
import java.time.LocalDate
import java.util.Locale

/**
 * A command-line tool for displaying Git logs or generating a changelog.
 *
 * This command supports two main functionalities:
 * 1. Displaying Git logs in the terminal with optional formatting.
 * 2. Generating a changelog file based on commit messages.
 */
class LogCommand : CliktCommand(name = "log") {
    private val terminal = Terminal()

    /**
     * The number of commits to display in the log.
     * Default value is 10.
     */
    private val count by option("-n", "--number", help = "Number of commits to show").int().default(10)

    /**
     * A flag to enable pretty formatting for the log output.
     * If set, the log will be displayed in a more readable format.
     */
    private val pretty by option("-p", "--pretty", help = "Use pretty format").flag()

    /**
     * The file path where the changelog will be written.
     * Default value is "CHANGELOG.md".
     */
    private val outputFile by option("-o", "--output", help = "Output file path for changelog").default("CHANGELOG.md")

    /**
     * A flag to indicate whether to generate a changelog.
     * If set, the changelog will be created instead of displaying logs.
     */
    private val changelog by option("-c", "--changelog", help = "Generate a changelog").flag()

    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context): String = context.theme.info("Display Git logs or generate a changelog")

    /**
     * Executes the command based on the provided options.
     * If the `--changelog` flag is set, a changelog is generated.
     * Otherwise, Git logs are displayed in the terminal.
     */
    override fun run() {
        if (changelog) {
            generateChangelog(outputFile)
        } else {
            showLog(count, pretty)
        }
    }

    /**
     * Displays Git logs in the terminal.
     *
     * @param count The number of commits to display.
     * @param pretty Whether to use pretty formatting for the log output.
     */
    private fun showLog(
        count: Int,
        pretty: Boolean,
    ) {
        try {
            val command = mutableListOf("git", "log", "-n", count.toString())
            if (pretty) {
                command.add("--pretty=format:%C(yellow)%h%Creset %C(green)%ad%Creset | %s %C(red)[%an]%Creset")
                command.add("--date=short")
            }

            val logProcess = ProcessBuilder(command).start()
            val logs = logProcess.inputStream.bufferedReader().readLines()
            logProcess.waitFor()

            if (logs.isEmpty()) {
                terminal.println(TextColors.red("No commits found."))
            } else {
                terminal.println(TextColors.brightBlue("Recent Git Commits:"))
                logs.forEach { log -> terminal.println(TextColors.green(log)) }
            }
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error showing log: ${e.message}"))
        }
    }

    /**
     * Generates a changelog file based on Git commit messages.
     *
     * @param outputPath The file path where the changelog will be written.
     */
    private fun generateChangelog(outputPath: String) {
        try {
            val gitLogProcess = gitPipe("log", "--pretty=format:%s")
            val commits = gitLogProcess.inputStream.bufferedReader().readLines()
            gitLogProcess.waitFor()

            if (commits.isEmpty()) {
                terminal.println(TextColors.red("No commits found to generate changelog."))
                return
            }

            val changelog = buildChangelog(commits)
            File(outputPath).writeText(changelog)
            terminal.println(TextColors.green("Changelog generated at $outputPath"))
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error generating changelog: ${e.message}"))
        }
    }

    /**
     * Builds a changelog string from a list of commit messages.
     *
     * @param commits A list of commit messages.
     * @return A formatted changelog string.
     */
    private fun buildChangelog(commits: List<String>): String =
        buildString {
            appendLine(TextColors.brightBlue("# Changelog"))
            appendLine()
            appendLine(TextColors.green("## ${LocalDate.now()}"))
            appendLine()

            val typeMap = mutableMapOf<String, MutableList<String>>()
            val regex = Regex("^(\\w+)(\\(.*\\))?(!)?:(.+)$")

            commits.forEach { commit ->
                regex.find(commit)?.apply {
                    val (type, _, _, description) = destructured
                    typeMap.getOrPut(type) { mutableListOf() }.add(description.trim())
                }
            }

            typeMap.mapValues { it.value.toList() }.forEach { (type, messages) ->
                val header =
                    when (type) {
                        "feat" -> TextColors.green("Features")
                        "fix" -> TextColors.red("Bug Fixes")
                        "docs" -> TextColors.brightBlue("Documentation")
                        "style" -> TextColors.brightBlue("Styling")
                        "refactor" -> TextColors.green("Refactors")
                        "perf" -> TextColors.green("Performance")
                        "test" -> TextColors.brightBlue("Tests")
                        "build" -> TextColors.brightBlue("Build")
                        "ci" -> TextColors.brightBlue("CI")
                        "chore" -> TextColors.brightBlue("Chores")
                        else -> TextColors.brightBlue(type.replaceFirstChar { it.titlecase(Locale.getDefault()) })
                    }

                appendLine("### $header")
                appendLine()
                messages.forEach { message -> appendLine("- ${TextColors.green(message)}") }
                appendLine()
            }
        }
}
