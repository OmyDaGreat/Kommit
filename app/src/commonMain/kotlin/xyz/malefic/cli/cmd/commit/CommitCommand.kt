package xyz.malefic.cli.cmd.commit

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path.Companion.toPath
import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.executeCommand
import xyz.malefic.cli.cmd.util.readInput

internal const val DEFAULT_CONFIG_PATH = ".kommit.yaml"

class CommitCommand : BaseCommand() {
    override fun Session.run(args: Array<String>) {
        generateCommit(extractConfigPath(args))
    }

    override fun MainRenderScope.showHelp() {
        textLine("Generate a commit message")
        textLine("")
        textLine("Usage: kommit commit [options]")
        textLine("")
        textLine("Options:")
        textLine("  -c, --config PATH   Path to the configuration file (default: .kommit.yaml)")
        textLine("  -h, --help          Show this help message")
        textLine("")
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

    // ----- Configuration Model -----

    data class TypeEntry(
        val type: String,
        val description: String = "",
    )

    @Serializable
    data class KommitOptions(
        val remindToStageChanges: Boolean? = null,
        val autoStage: Boolean? = null,
        val autoPush: Boolean? = null,
        val allowBreakingChanges: List<String>? = null,
        val allowIssues: List<String>? = null,
        val allowCustomScopes: Boolean? = null,
        val allowEmptyScopes: Boolean? = null,
        val changesPrefix: String? = null,
        val issuePrefix: String? = null,
    ) {
        companion object {
            fun withDefaults(raw: KommitOptions? = null): ResolvedOptions {
                val r = raw ?: KommitOptions()
                return ResolvedOptions(
                    remindToStageChanges = r.remindToStageChanges ?: false,
                    autoStage = r.autoStage ?: false,
                    autoPush = r.autoPush ?: false,
                    allowBreakingChanges = r.allowBreakingChanges?.toSet() ?: emptySet(),
                    allowIssues = r.allowIssues?.toSet() ?: emptySet(),
                    allowCustomScopes = r.allowCustomScopes ?: true,
                    allowEmptyScopes = r.allowEmptyScopes ?: true,
                    changesPrefix = r.changesPrefix ?: "BREAKING CHANGES:",
                    issuePrefix = r.issuePrefix ?: "ISSUES CLOSED:",
                )
            }
        }
    }

    data class ResolvedOptions(
        val remindToStageChanges: Boolean,
        val autoStage: Boolean,
        val autoPush: Boolean,
        val allowBreakingChanges: Set<String>,
        val allowIssues: Set<String>,
        val allowCustomScopes: Boolean,
        val allowEmptyScopes: Boolean,
        val changesPrefix: String,
        val issuePrefix: String,
    )

    data class KommitConfig(
        val types: List<TypeEntry>,
        val scopes: Map<String, List<String>>,
        val options: ResolvedOptions,
    ) {
        companion object {
            val empty = KommitConfig(emptyList(), emptyMap(), KommitOptions.withDefaults())
        }
    }

    // ----- Parsing -----

    private fun parseConfig(configPath: String): KommitConfig {
        val path = configPath.toPath()
        check(FileSystem.SYSTEM.exists(path)) {
            "Configuration file '$configPath' not found. Create one with 'kommit create' or specify a path with --config."
        }
        val configContent = FileSystem.SYSTEM.read(path) { readUtf8() }

        val root = Yaml.default.parseToYamlNode(configContent)
        check(root is YamlMap) {
            "Configuration root must be a map."
        }

        val types = parseTypes(root)
        val scopes = parseScopes(root)
        val options = parseOptions(root)

        return KommitConfig(
            types = types,
            scopes = scopes,
            options = KommitOptions.withDefaults(options),
        )
    }

    private fun parseTypes(root: YamlMap): List<TypeEntry> {
        val node = root.get<YamlNode>("types") ?: return emptyList()
        check(node is YamlList) {
            "Value for 'types' is invalid: Expected a list."
        }
        return node.items.map { item ->
            when (item) {
                is YamlScalar -> {
                    val raw = item.content.trim()
                    val parts = raw.split(":", limit = 2)
                    if (parts.size == 2) {
                        TypeEntry(parts[0].trim(), parts[1].trim())
                    } else {
                        TypeEntry(raw)
                    }
                }

                is YamlMap -> {
                    val entry = item.entries.iterator().next()
                    val key = entry.key.content.trim()
                    val desc =
                        when (entry.value) {
                            is YamlScalar -> (entry.value as YamlScalar).content.trim()
                            else -> error("Type description must be a scalar.")
                        }
                    TypeEntry(key, desc)
                }

                else -> {
                    error("Value for 'types' is invalid: Expected a string or map.")
                }
            }
        }
    }

    private fun parseScopes(root: YamlMap): Map<String, List<String>> {
        val node = root.get<YamlNode>("scopes") ?: return emptyMap()
        check(node is YamlMap) {
            "Value for 'scopes' is invalid: Expected a map."
        }
        return node.entries
            .map { (kNode, vNode) ->
                val key = kNode.content.trim()
                val list =
                    when (vNode) {
                        is YamlList -> {
                            vNode.items.mapNotNull {
                                (it as? YamlScalar)?.content?.trim()
                            }
                        }

                        else -> {
                            error("Scope '$key' must be a list.")
                        }
                    }
                key to list
            }.toMap()
    }

    private fun parseOptions(root: YamlMap): KommitOptions? {
        val node = root.get<YamlNode>("options") ?: return null
        check(node is YamlMap) {
            "Value for 'options' is invalid: Expected a map."
        }

        fun boolOrNull(n: YamlNode?): Boolean? =
            (n as? YamlScalar)?.content?.trim()?.lowercase()?.let {
                when (it) {
                    "true" -> true
                    "false" -> false
                    else -> null
                }
            }

        fun stringList(n: YamlNode?): List<String>? =
            if (n is YamlList) n.items.mapNotNull { (it as? YamlScalar)?.content?.trim() } else null

        fun stringOrNull(n: YamlNode?): String? = (n as? YamlScalar)?.content?.trim()

        return KommitOptions(
            remindToStageChanges = boolOrNull(node["remindToStageChanges"]),
            autoStage = boolOrNull(node["autoStage"]),
            autoPush = boolOrNull(node["autoPush"]),
            allowBreakingChanges = stringList(node["allowBreakingChanges"]),
            allowIssues = stringList(node["allowIssues"]),
            allowCustomScopes = boolOrNull(node["allowCustomScopes"]),
            allowEmptyScopes = boolOrNull(node["allowEmptyScopes"]),
            changesPrefix = stringOrNull(node["changesPrefix"]),
            issuePrefix = stringOrNull(node["issuePrefix"]),
        )
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
            config = parseConfig(configPath)
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
                        red { textLine("No staged changes detected. Stage changes first (configure autoStage to stage automatically).") }
                        error = true
                    }
                }
            }
        }.run()
        if (error) return

        val selectedType = readInput("\nSelect the type of change: ", *config.types.map { it.type }.toTypedArray())

        val availableScopes = (config.scopes["all"].orEmpty() + config.scopes[selectedType].orEmpty()).distinct()

        var scope by liveVarOf("")
        var scopeInput by liveVarOf("")
        section {
            if (availableScopes.isNotEmpty()) {
                textLine("\nSelect a scope:")
                availableScopes.forEachIndexed { index, s ->
                    textLine("${index + 1}. $s")
                }
                if (options.allowCustomScopes) {
                    textLine("${availableScopes.size + 1}. (Custom scope)")
                }
                textLine(
                    "Enter your choice (1-${availableScopes.size}${if (options.allowCustomScopes) ", or ${availableScopes.size + 1} for custom" else ""}${if (options.allowEmptyScopes) ", or press Enter to skip" else ""}): ",
                )
            } else if (options.allowEmptyScopes) {
                textLine("\nEnter a scope (optional, press Enter to skip): ")
            } else {
                textLine("\nEnter a scope (required): ")
            }
            text("> ")
            input()
        }.runUntilInputEntered {
            onInputEntered { scopeInput = input }
        }

        if (availableScopes.isNotEmpty()) {
            when {
                scopeInput.isBlank() && options.allowEmptyScopes -> {
                    scope = ""
                }

                scopeInput.isBlank() -> {
                    section {
                        red { textLine("Empty scopes are disabled. Please enter a valid scope.") }
                    }.run()
                    return
                }

                scopeInput.toIntOrNull() != null -> {
                    val idx = scopeInput.toInt() - 1
                    when {
                        idx in availableScopes.indices -> {
                            scope = availableScopes[idx]
                        }

                        options.allowCustomScopes && idx == availableScopes.size -> {
                            var customScope by liveVarOf("")
                            section {
                                textLine("Enter custom scope:")
                                text("> ")
                                input()
                            }.runUntilInputEntered {
                                onInputEntered { customScope = input }
                            }
                            scope = customScope.trim()
                        }

                        else -> {
                            section {
                                red { textLine("Invalid scope selection.") }
                            }.run()
                            return
                        }
                    }
                }

                options.allowCustomScopes -> {
                    scope = scopeInput.trim()
                }

                else -> {
                    section {
                        red { textLine("Custom scopes are disabled. Please enter a valid scope.") }
                    }.run()
                    return
                }
            }
        } else if (options.allowEmptyScopes) {
            scope = scopeInput.trim()
        } else {
            scope = scopeInput.trim()
            if (scope.isEmpty()) {
                section {
                    red { textLine("Empty scopes are disabled. Please enter a valid scope.") }
                }.run()
                return
            }
        }

        val description = readInput("\nEnter a short description:").trim()
        if (description.isEmpty()) {
            section {
                red { textLine("The description cannot be empty. Please enter a valid description.") }
            }.run()
            return
        }

        var breakingChangeDetails: String? = null
        if (selectedType in options.allowBreakingChanges) {
            val resp = readInput("\nIs this a breaking change? (y/N):", "y", "n", "yes", "no").trim().lowercase()
            if (resp == "y" || resp == "yes") {
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
        if (response == "y" || response == "yes") {
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
