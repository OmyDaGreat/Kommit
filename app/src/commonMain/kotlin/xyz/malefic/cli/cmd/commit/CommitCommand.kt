package xyz.malefic.cli.cmd.commit

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.commit.Git.commitChanges
import xyz.malefic.cli.cmd.commit.Git.getChangedFiles
import xyz.malefic.cli.cmd.commit.Git.getStagedFiles
import xyz.malefic.cli.cmd.commit.Git.pushChanges
import xyz.malefic.cli.cmd.commit.Git.reviewStaging
import xyz.malefic.cli.cmd.commit.Git.stageAll
import xyz.malefic.cli.cmd.commit.Parsing.KommitConfig
import xyz.malefic.cli.cmd.commit.Parsing.StageMode
import xyz.malefic.cli.cmd.util.readInput
import xyz.malefic.cli.cmd.util.readInputOptions
import xyz.malefic.cli.cmd.util.readInputRetry
import xyz.malefic.cli.cmd.util.readMultilineInput

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
        textLine("If no configuration exists, run 'kommit create' to set up a .kommit.yaml")
    }

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
            when (options.stageMode) {
                StageMode.NONE -> {
                    if (getStagedFiles().isEmpty()) {
                        red {
                            textLine(
                                "No staged changes detected. Stage changes first (set the stageMode option to 'auto' or 'review' to stage from Kommit).",
                            )
                        }
                        error = true
                    }
                }

                StageMode.REVIEW -> {
                    if (getChangedFiles().isEmpty()) {
                        red { textLine("No changes to commit.") }
                        error = true
                    }
                }

                StageMode.AUTO -> {
                    if (getStagedFiles().isEmpty()) {
                        textLine("No staged changes. Auto-staging all changes (configured)...")
                        stageAll { error = true }
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
                    "\nEnter a scope${if (options.allowEmptyScopes) " (or leave blank to skip)" else ""}:",
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

        var body: String? = null
        if (options.allowBody) {
            val bodyRaw = readMultilineInput("\nEnter a longer description (press Enter on an empty line to finish — leave blank to skip):")
            if (bodyRaw.isNotBlank()) {
                body = bodyRaw
            }
        }

        var isBreakingChange = false
        var breakingChangeDetails: String? = null
        if (options.allowBreakingChanges) {
            val resp = readInputOptions("\nIs this a breaking change? (y/N):", "y", "n", "yes", "no").trim().lowercase()
            if (resp.lowercase() in listOf("y", "yes")) {
                isBreakingChange = true
                val bc = readInput("Describe the breaking change (leave blank to skip):").trim()
                if (bc.isNotEmpty()) {
                    breakingChangeDetails = bc
                }
            }
        }

        var issuesLine: String? = null
        if (options.allowIssues) {
            val issuesRaw = readInput("\nReference issues? (comma-separated like 123,456 — leave blank to skip):").trim()
            if (issuesRaw.isNotEmpty()) {
                val cleaned = issuesRaw.split(",").map { it.trim().trimStart('#') }.filter { it.matches(Regex("\\d+")) }
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
                if (isBreakingChange) {
                    append("!")
                }
                if (description.isNotBlank()) {
                    append(": $description")
                }
                if (!body.isNullOrBlank()) {
                    append("\n\n$body")
                }
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
        val response = readInputOptions("\nDo you want to commit with this message? (y/N):", "y", "n", "yes", "no").lowercase()
        if (response.lowercase() in listOf("y", "yes")) {
            if (options.stageMode == StageMode.REVIEW && !reviewStaging()) {
                section { yellow { textLine("Commit aborted.") } }.run()
                return
            }
            if (commitChanges(commitMessage) && options.autoPush) {
                pushChanges()
            }
        } else {
            section { yellow { textLine("Commit aborted.") } }.run()
        }
    }
}
