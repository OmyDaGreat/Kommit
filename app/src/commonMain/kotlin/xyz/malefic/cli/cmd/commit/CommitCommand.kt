package xyz.malefic.cli.cmd.commit

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.commit.Git.commit
import xyz.malefic.cli.cmd.commit.Git.getChangedFiles
import xyz.malefic.cli.cmd.commit.Git.getStagedFiles
import xyz.malefic.cli.cmd.commit.Git.push
import xyz.malefic.cli.cmd.commit.Git.stage
import xyz.malefic.cli.cmd.commit.Git.stageAll
import xyz.malefic.cli.cmd.commit.Git.unstage
import xyz.malefic.cli.cmd.commit.Parsing.KommitConfig
import xyz.malefic.cli.cmd.commit.Parsing.StageMode
import xyz.malefic.cli.cmd.util.Check
import xyz.malefic.cli.cmd.util.SelectOption
import xyz.malefic.cli.cmd.util.readInput
import xyz.malefic.cli.cmd.util.readInputOptions
import xyz.malefic.cli.cmd.util.readInputRetry
import xyz.malefic.cli.cmd.util.readChecklist
import xyz.malefic.cli.cmd.util.readMultilineInput
import xyz.malefic.cli.cmd.util.readSelect

class CommitCommand : BaseCommand() {
    override fun Session.run(args: Array<String>) {
        generateCommit(
            extractConfigPath(args),
            typeArg = extractOption(args, "-t", "--type"),
            scopeArg = extractOption(args, "-s", "--scope"),
        )
    }

    override fun MainRenderScope.showHelp() {
        textLine("Generate a commit message")
        textLine()
        textLine("Usage: kommit commit [options]")
        textLine()
        textLine("Options:")
        textLine("  -c, --config PATH   Path to the configuration file (default: .kommit.yaml)")
        textLine("  -t, --type TYPE     Commit type, skipping the type picker")
        textLine("  -s, --scope SCOPE   Commit scope, skipping the scope picker (\"\" for no scope)")
        textLine("  -h, --help          Show this help message")
        textLine()
        textLine("If no configuration exists, run 'kommit create' to set up a .kommit.yaml")
    }

    private fun Session.generateCommit(
        configPath: String,
        typeArg: String?,
        scopeArg: String?,
    ) {
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
        val typeNames = config.types.map { it.type }
        fun scopesFor(type: String) = (config.scopes["all"].orEmpty() + config.scopes[type].orEmpty()).distinct()

        fun scopeError(
            type: String,
            scope: String,
        ): String? {
            val available = scopesFor(type)
            val valid =
                scope in available ||
                    (options.allowCustomScopes && scope.isNotBlank()) ||
                    (options.allowEmptyScopes && scope.isBlank())
            if (valid) return null
            val allowed = available + if (options.allowEmptyScopes) listOf("\"\" (none)") else emptyList()
            return "Invalid scope '$scope' for type '$type'. Available: ${allowed.joinToString(", ").ifEmpty { "none" }}"
        }

        // Check flags before staging anything, so a typo doesn't leave the index changed.
        val flagError =
            when {
                typeArg != null && typeArg !in typeNames -> {
                    "Unknown type '$typeArg'. Available: ${typeNames.joinToString(", ")}"
                }

                typeArg != null && scopeArg != null -> {
                    scopeError(typeArg, scopeArg.trim())
                }

                else -> {
                    null
                }
            }
        if (flagError != null) {
            section { red { textLine(flagError) } }.run()
            return
        }

        if (!checkStaging(options.stageMode)) return

        val selectedType =
            typeArg?.also { section { textLine("\nType: $it") }.run() }
                ?: readSelect(
                    "\nSelect the type of change:",
                    config.types.map { SelectOption(it.type, description = it.description) },
                )

        val availableScopes = scopesFor(selectedType)
        if (availableScopes.isEmpty() && !options.allowCustomScopes && !options.allowEmptyScopes) {
            section {
                red { textLine("No scopes available for the selected type, and custom/empty scopes are disabled. Please fix the config.") }
            }.run()
            return
        }
        val scope =
            if (scopeArg != null) {
                val trimmed = scopeArg.trim()
                scopeError(selectedType, trimmed)?.let { message ->
                    section { red { textLine(message) } }.run()
                    return
                }
                section { textLine("\nScope: ${trimmed.ifEmpty { "(none)" }}") }.run()
                trimmed
            } else if (availableScopes.isEmpty() && !options.allowCustomScopes) {
                "" // Only an empty scope is allowed, so there's nothing to ask.
            } else {
                val scopeOptions =
                    availableScopes.map { SelectOption(it) } +
                        if (options.allowEmptyScopes) listOf(SelectOption("", "(none)", "no scope")) else emptyList()
                val hint = if (options.allowCustomScopes) " (type a new one to use a custom scope)" else ""
                readSelect("\nSelect a scope$hint:", scopeOptions, allowCustom = options.allowCustomScopes).trim()
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
            afterSession {
                if (!commit(commitMessage).success) {
                    println("Failed to create commit.")
                    return@afterSession
                }
                println("Commit created successfully!")
                if (options.autoPush) {
                    println("Pushing changes (autoPush enabled)...")
                    println(if (push().success) "Push successful." else "Push failed.")
                }
            }
        } else {
            section { yellow { textLine("Commit aborted.") } }.run()
        }
    }

    /**
     * Make sure there is something to commit for the given [mode], auto-staging if configured.
     * Returns false (after reporting why) if the commit can't go ahead.
     */
    private fun Session.checkStaging(mode: StageMode): Boolean {
        val issue =
            when (mode) {
                StageMode.NONE -> {
                    "No staged changes detected. Stage changes first (set the stageMode option to 'auto' or 'review' to stage from Kommit)."
                        .takeIf { getStagedFiles().isEmpty() }
                }

                StageMode.REVIEW -> {
                    "No changes to commit.".takeIf { getChangedFiles().isEmpty() }
                }

                StageMode.AUTO -> {
                    getStagedFiles().takeIf { it.isEmpty() }?.run {
                        section { textLine("No staged changes. Auto-staging all changes (configured)...") }.run()
                        stageAll().takeUnless { it.success }?.let { "Failed to stage changes automatically.\n${it.message}".trim() }
                    }
                }
            } ?: return true
        section { red { textLine(issue) } }.run()
        return false
    }

    /**
     * Let the user pick which changed files to commit, then stage the picked ones and unstage the rest.
     * Partially staged files can also be kept as they are, so only their staged changes are committed.
     * Returns false if nothing was selected or git failed.
     */
    private fun Session.reviewStaging(): Boolean {
        val files =
            getChangedFiles().ifEmpty {
                section { red { textLine("No changes to commit.") } }.run()
                return false
            }
        val anyStaged = files.any { it.staged }
        val initial =
            files.map {
                when {
                    it.partiallyStaged -> Check.PARTIAL
                    it.staged || !anyStaged -> Check.ON
                    else -> Check.OFF
                }
            }
        val legend =
            "[x] stage whole file  [~] keep staged part only  [ ] leave out".takeIf { Check.PARTIAL in initial }
        val states = readChecklist("\nSelect the files to stage and commit:", files.map { it.display }, initial, legend)
        if (states.all { it == Check.OFF }) {
            section { yellow { textLine("No files selected.") } }.run()
            return false
        }

        // [~] files are left exactly as they are. [x] files are only added if they have unstaged changes: an already
        // fully staged file (e.g. a staged deletion) may not exist anywhere git add looks, and an unmatched pathspec
        // makes git abort the whole add.
        val toStage = files.filterIndexed { i, f -> states[i] == Check.ON && f.hasUnstagedChanges }.map { it.paths.first() }
        val toUnstage = files.filterIndexed { i, f -> states[i] == Check.OFF && f.staged }.flatMap { it.paths }
        val failure =
            listOfNotNull(
                if (toStage.isNotEmpty()) stage(toStage) else null,
                if (toUnstage.isNotEmpty()) unstage(toUnstage) else null,
            ).firstOrNull { !it.success } ?: return true
        section {
            red {
                textLine("Failed to update staged files.")
                if (failure.message.isNotEmpty()) textLine(failure.message)
            }
        }.run()
        return false
    }
}
