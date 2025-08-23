package xyz.malefic.cli.cmd.commit

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path.Companion.toPath
import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.executeCommand

internal const val DEFAULT_CONFIG_PATH = ".kommit.yaml"

/**
 * Enhanced CommitCommand supporting:
 *  - Map style type entries ( `- feat: A new feature` )
 *  - Plain string type entries ( `- feat` or `- "feat: A new feature"` )
 *  - Per-type scopes plus global scopes (`all` + specific type key)
 *  - Configuration options (breaking changes, issues, auto stage/push, etc.)
 *  - Helpful guidance when configuration file is missing
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
        echo("")
        echo("If no configuration exists, run 'kommit create' to scaffold a default .kommit.yaml")
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
            fun withDefaults(raw: KommitOptions?): ResolvedOptions {
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
    )

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
                else -> error("Value for 'types' is invalid: Expected a string or map.")
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
                        is YamlList ->
                            vNode.items.mapNotNull {
                                (it as? YamlScalar)?.content?.trim()
                            }
                        else -> error("Scope '$key' must be a list.")
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

    private fun generateCommit(configPath: String) {
        echo("Generating conventional commit...")

        val config = parseConfig(configPath)

        if (config.types.isEmpty()) {
            echo("No commit types defined in configuration.", err = true)
            return
        }

        val options = config.options

        // Optionally remind / auto-stage
        if (options.remindToStageChanges || options.autoStage) {
            val staged = getStagedFiles()
            if (staged.isEmpty()) {
                if (options.autoStage) {
                    echo("No staged changes. Auto-staging all changes (configured)...")
                    stageAll()
                } else if (options.remindToStageChanges) {
                    echo("No staged changes detected. Stage changes first (configure autoStage to stage automatically).", err = true)
                }
            }
        }

        // Present types
        echo("\nSelect the type of change:")
        config.types.forEachIndexed { index, entry ->
            val line =
                if (entry.description.isNotBlank()) {
                    "${index + 1}. ${entry.type} - ${entry.description}"
                } else {
                    "${index + 1}. ${entry.type}"
                }
            echo(line)
        }

        print("Enter your choice (1-${config.types.size}): ")
        val choice = readLine()?.toIntOrNull()?.minus(1)
        if (choice == null || choice !in config.types.indices) {
            echo("Invalid selection.", err = true)
            return
        }
        val selectedType = config.types[choice].type

        // Determine scopes available: global 'all' + per-type if present
        val availableScopes =
            (config.scopes["all"].orEmpty() + config.scopes[selectedType].orEmpty())
                .distinct()

        val scope =
            if (availableScopes.isNotEmpty()) {
                echo("\nSelect a scope:")
                availableScopes.forEachIndexed { index, s ->
                    echo("${index + 1}. $s")
                }
                if (options.allowCustomScopes) {
                    echo("${availableScopes.size + 1}. (Custom scope)")
                }
                print(
                    "Enter your choice (1-${availableScopes.size}${if (options.allowCustomScopes) ", or ${availableScopes.size + 1} for custom" else ""}${if (options.allowEmptyScopes) ", or press Enter to skip" else ""}): ",
                )

                val scopeInput = readLine()
                when {
                    scopeInput.isNullOrBlank() && options.allowEmptyScopes -> ""
                    scopeInput?.toIntOrNull() != null -> {
                        val idx = scopeInput.toInt() - 1
                        when {
                            idx in availableScopes.indices -> availableScopes[idx]
                            options.allowCustomScopes && idx == availableScopes.size -> {
                                print("Enter custom scope: ")
                                readLine()?.trim().orEmpty()
                            }
                            else -> {
                                echo("Invalid scope selection.", err = true)
                                return
                            }
                        }
                    }
                    else -> {
                        if (options.allowCustomScopes) {
                            scopeInput!!.trim()
                        } else {
                            echo("Custom scopes are disabled. Invalid selection.", err = true)
                            return
                        }
                    }
                }
            } else {
                // Fallback if no scopes defined at all
                if (options.allowEmptyScopes) {
                    print("\nEnter a scope (optional, press Enter to skip): ")
                    readLine()?.trim().orEmpty()
                } else {
                    print("\nEnter a scope (required): ")
                    val entered = readLine()?.trim().orEmpty()
                    if (entered.isEmpty()) {
                        echo("A scope is required (allowEmptyScopes=false).", err = true)
                        return
                    }
                    entered
                }
            }

        print("\nEnter a short description: ")
        val description = readLine()?.trim().orEmpty()
        if (description.isEmpty()) {
            echo("Description cannot be empty.", err = true)
            return
        }

        // Optional breaking changes
        var breakingChangeDetails: String? = null
        if (selectedType in options.allowBreakingChanges) {
            print("\nIs this a breaking change? (y/N): ")
            val resp = readLine()?.trim()?.lowercase()
            if (resp == "y" || resp == "yes") {
                print("Describe the breaking change: ")
                val bc = readLine()?.trim().orEmpty()
                if (bc.isNotEmpty()) {
                    breakingChangeDetails = bc
                }
            }
        }

        // Optional issues
        var issuesLine: String? = null
        if (selectedType in options.allowIssues) {
            print("\nReference issues? (comma-separated like 123,456 — leave blank to skip): ")
            val issuesRaw = readLine()?.trim().orEmpty()
            if (issuesRaw.isNotEmpty()) {
                // Normalize: accept with or without '#'
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

                // Footer content separated by blank line if needed
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

        echo("\nGenerated Commit Message:")
        echo(commitMessage)
        print("\nDo you want to commit with this message? (y/N): ")
        val response = readLine()?.lowercase()
        if (response == "y" || response == "yes") {
            val success = commitChanges(commitMessage)
            if (success && options.autoPush) {
                pushChanges()
            }
        } else {
            echo("Commit aborted.")
        }
    }

    private fun commitChanges(message: String): Boolean =
        try {
            val result = executeCommand("git", "commit", "-m", message)
            if (result.exitCode == 0) {
                echo("Commit created successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
                true
            } else {
                echo("Failed to create commit. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
                false
            }
        } catch (e: Exception) {
            echo("Error creating commit: ${e.message}", err = true)
            false
        }

    private fun pushChanges() {
        try {
            echo("Pushing changes (autoPush enabled)...")
            val result = executeCommand("git", "push")
            if (result.exitCode == 0) {
                echo("Push successful.")
                if (result.output.isNotEmpty()) echo(result.output)
            } else {
                echo("Push failed. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) echo(result.error, err = true)
            }
        } catch (e: Exception) {
            echo("Error pushing changes: ${e.message}", err = true)
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

    private fun stageAll() {
        try {
            val result = executeCommand("git", "add", ".")
            if (result.exitCode != 0) {
                echo("Failed to stage changes automatically.", err = true)
                if (result.error.isNotEmpty()) echo(result.error, err = true)
            }
        } catch (e: Exception) {
            echo("Error staging changes: ${e.message}", err = true)
        }
    }
}
