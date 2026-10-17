package xyz.malefic.cli.cmd.commit

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.commit.Parsing.KommitConfig
import xyz.malefic.cli.cmd.util.executeCommand
import xyz.malefic.cli.cmd.util.readInput
import xyz.malefic.cli.cmd.util.readInputOptions
import xyz.malefic.cli.cmd.util.readInputRetry

internal const val DEFAULT_CONFIG_PATH = ".kommit.yaml"

class CommitCommand : BaseCommand() {
    override fun Session.run(args: Array<String>) {
        generateCommit(extractConfigPath(args))
    }

    override fun MainRenderScope.showHelp() {
        textLine("Generate a commit message")
        textLine()
        textLine("Usage: kommit commit [options]")
        textLine()
        textLine("Options:")
        textLine("  -c, --config PATH   Path to the configuration file (default: .kommit.yaml)")
        textLine("  -h, --help          Show this help message")
        textLine()
        textLine("If no configuration exists, run 'kommit create' to scaffold a default .kommit.yaml")
    }

    private fun extractConfigPath(args: Array<String>): String {
        val configIndex = args.indexOfFirst { it == "-c" || it == "--config" }
        return if (configIndex >= 0 && configIndex + 1 < args.size) {
            args[configIndex + 1]
        } else {
            DEFAULT_CONFIG_PATH
        }
    }

    // ----- Commit Flow -----

    private fun Session.generateCommit(configPath: String) {
        var error by liveVarOf(false)

        section {
            textLine("Generating conventional commit...")
        }.run()

        var config by liveVarOf(KommitConfig.empty)
        var configLoaded = false
        section {
            if (!configLoaded) {
                yellow { textLine("Loading configuration from '$configPath'...") }
            } else if (config.types.isEmpty()) {
                red { textLine("No commit types defined in configuration.") }
                error = true
            } else {
                green { textLine("Commit types loaded.") }
            }
        }.run {
            config = Parsing.parseConfig(configPath)
            configLoaded = true
        }
        if (error) return

        val options = config.options
        section {
            if (options.remindToStageChanges || options.autoStage) {
                val staged = getStagedFiles()
                if (staged.isEmpty()) {
                    if (options.autoStage) {
                        textLine("No staged changes. Auto-staging all changes (configured)...")
                        stageAll { error = true }
                    } else if (options.remindToStageChanges) {
                        red {
                            textLine(
                                "No staged changes detected. Stage changes first (configure the autoStage option to stage automatically).",
                            )
                        }
                        error = true
                    }
                }
            }
        }.run()
        if (error) return

        val selectedType = readInputOptions("\nSelect the type of change: ", *config.types.map { it.type }.toTypedArray())

        val availableScopes = (config.scopes["all"].orEmpty() + config.scopes[selectedType].orEmpty()).distinct()
        if (availableScopes.isEmpty() && !options.allowCustomScopes && !options.allowEmptyScopes) {
            section {
                red { textLine("No scopes available for the selected type, and custom/empty scopes are disabled. Please fix the config.") }
            }.run()
            return
        }
        val scope =
            if (availableScopes.isNotEmpty() && !(options.allowCustomScopes || options.allowEmptyScopes)) {
                readInputOptions(
                    "\nSelect a scope: ",
                    *availableScopes.toTypedArray(),
                )
            } else if (availableScopes.isNotEmpty()) {
                val scopeOptionString = "${
                    if (options.allowCustomScopes) " (or enter a custom one)" else ""
                } ${
                    if (options.allowEmptyScopes) " (or leave blank to skip)" else ""
                }"
                readInputRetry(
                    "\nSelect a scope$scopeOptionString: ",
                    {
                        it in availableScopes || (options.allowCustomScopes && it.isNotBlank()) ||
                            (options.allowEmptyScopes && it.isBlank())
                    },
                    "Invalid scope. Please choose from the available scopes$scopeOptionString.",
                    *availableScopes.toTypedArray(),
                )
            } else {
                readInputRetry(
                    "\nEnter a scope ${if (options.allowEmptyScopes) "(or leave blank to skip)" else ""}:",
                    { it.isNotBlank() || (options.allowEmptyScopes && it.isBlank()) },
                    "Invalid scope. Please enter a valid scope.",
                ).trim()
            }

        val description =
            readInputRetry(
                "\nEnter a short description:",
                { it.isNotEmpty() },
                "Description cannot be empty. Please enter a valid description.",
            ).trim()

        var breakingChangeDetails: String? = null
        if (selectedType in options.allowBreakingChanges) {
            val resp = readInput("\nIs this a breaking change? (y/N):", "y", "n", "yes", "no").trim().lowercase()
            if (resp.lowercase() in listOf("y", "yes")) {
                val bc = readInput("Describe the breaking change:").trim()
                if (bc.isNotEmpty()) {
                    breakingChangeDetails = bc
                }
            }
        }

        var issuesLine: String? = null
        if (selectedType in options.allowIssues) {
            val issuesRaw = readInput("\nReference issues? (comma-separated like 123,456 — leave blank to skip):").trim()
            if (issuesRaw.isNotEmpty()) {
                val cleaned =
                    issuesRaw
                        .split(",")
                        .map { it.trim().trimStart('#') }
                        .filter { it.matches(Regex("\\d+")) }
                if (cleaned.isNotEmpty()) {
                    issuesLine = cleaned.joinToString(", ") { "#$it" }
                }
            }
        }

        val commitMessage =
            buildString {
                append(selectedType)
                if (scope.isNotEmpty()) {
                    append("($scope)")
                }
                append(": $description")

                if (breakingChangeDetails != null || issuesLine != null) {
                    append("\n\n")
                }
                if (breakingChangeDetails != null) {
                    append("${options.changesPrefix} ${breakingChangeDetails.trim()}")
                }
                if (issuesLine != null) {
                    if (breakingChangeDetails != null) append("\n")
                    append("${options.issuePrefix} $issuesLine")
                }
            }

        section {
            textLine("\nGenerated commit message:")
            textLine(commitMessage)
        }.run()
        val response = readInput("\nDo you want to commit with this message? (y/N):", "y", "n", "yes", "no").lowercase()
        if (response.lowercase() in listOf("y", "yes")) {
            val success = commitChanges(commitMessage)
            if (success && options.autoPush) {
                pushChanges()
            }
        } else {
            section {
                textLine("Commit aborted.")
            }.run()
        }
    }

    private fun Session.commitChanges(message: String): Boolean =
        try {
            val result = executeCommand("git", "commit", "-m", message)
            if (result.exitCode == 0) {
                section {
                    textLine("Commit created successfully!")
                    yellow { if (result.output.isNotEmpty()) textLine(result.output) }
                }.run()
                true
            } else {
                section {
                    red { textLine("Failed to create commit. Exit code: ${result.exitCode}") }
                }.run()
                if (result.error.isNotEmpty()) {
                    section { red { textLine(result.error) } }.run()
                }
                false
            }
        } catch (e: Exception) {
            section {
                red { textLine("Error creating commit: ${e.message}") }
            }.run()
            false
        }

    private fun Session.pushChanges() {
        try {
            section {
                textLine("Pushing changes (autoPush enabled)...")
            }.run()
            val result = executeCommand("git", "push")
            if (result.exitCode == 0) {
                section {
                    textLine("Push successful.")
                    yellow { if (result.output.isNotEmpty()) textLine(result.output) }
                }.run()
            } else {
                section {
                    red { textLine("Push failed. Exit code: ${result.exitCode}") }
                    yellow { if (result.error.isNotEmpty()) textLine(result.error) }
                }.run()
            }
        } catch (e: Exception) {
            section {
                red { textLine("Error pushing changes: ${e.message}") }
            }.run()
        }
    }

    // ----- Git Helpers -----

    private fun getStagedFiles(): List<String> =
        try {
            val result = executeCommand("git", "diff", "--cached", "--name-only")
            if (result.exitCode == 0) {
                result.output.lines().mapNotNull { it.trim().ifEmpty { null } }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

    private fun MainRenderScope.stageAll(onError: () -> Unit) {
        try {
            val result = executeCommand("git", "add", ".")
            if (result.exitCode != 0) {
                red { textLine("Failed to stage changes automatically.") }
                yellow { if (result.error.isNotEmpty()) textLine(result.error) }
                onError()
            }
        } catch (e: Exception) {
            red { textLine("Error staging changes: ${e.message}") }
            onError()
        }
    }
}
