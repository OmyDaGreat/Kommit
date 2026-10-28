package xyz.malefic.cli.cmd.create

import com.varabyte.kotter.foundation.text.bold
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import okio.FileSystem
import okio.Path.Companion.toPath
import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.commit.Parsing.KommitConfig
import xyz.malefic.cli.cmd.commit.Parsing.KommitOptions
import xyz.malefic.cli.cmd.commit.Parsing.StageMode
import xyz.malefic.cli.cmd.commit.Parsing.TypeEntry
import xyz.malefic.cli.cmd.commit.git
import xyz.malefic.cli.cmd.create.ConfigYaml.defaultConfig
import xyz.malefic.cli.cmd.create.ConfigYaml.defaultTypeNames
import xyz.malefic.cli.cmd.create.ConfigYaml.nameRegex
import xyz.malefic.cli.cmd.create.ConfigYaml.standardTypes
import xyz.malefic.cli.cmd.create.ConfigYaml.toYaml
import xyz.malefic.cli.cmd.util.readConfirm
import xyz.malefic.cli.cmd.util.readInput
import xyz.malefic.cli.cmd.util.readInputRetry
import xyz.malefic.cli.cmd.util.readMultiSelect

class CreateCommand : BaseCommand() {
    override fun Session.run(args: Array<String>) {
        val configPath = extractConfigPath(args)
        val useDefaults = args.contains("-d") || args.contains("--defaults")
        val path = configPath.toPath()

        if (FileSystem.SYSTEM.exists(path)) {
            if (useDefaults) {
                section { yellow { textLine("'$configPath' already exists. Aborting to avoid overwrite.") } }.run()
                return
            }
            if (!readConfirm("'$configPath' already exists. Replace it?", false)) {
                section { yellow { textLine("Left '$configPath' unchanged.") } }.run()
                return
            }
        }

        val config =
            if (useDefaults) {
                defaultConfig
            } else {
                section {
                    bold { textLine("Let's set up Kommit for this project.") }
                    textLine("Press enter at any prompt to accept its default.")
                }.run()
                runWizard()
            }
        val yaml = config.toYaml()

        if (!useDefaults) {
            section {
                bold { textLine("\nHere's your configuration:") }
                cyan { textLine(yaml) }
            }.run()
            if (!readConfirm("Write it to '$configPath'?", true)) {
                section { yellow { textLine("Nothing was written.") } }.run()
                return
            }
        }

        try {
            path.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
            FileSystem.SYSTEM.write(path) { writeUtf8(yaml) }
            section {
                green { textLine("Created '$configPath'.") }
                textLine("Push it so your team shares the same rules; then anyone can run 'kommit' to make a commit.")
            }.run()
        } catch (e: Exception) {
            section { red { textLine("Error creating config file: ${e.message}") } }.run()
        }
    }

    private fun Session.runWizard(): KommitConfig {
        val types = askTypes()
        val scopes = askScopes(types.map { it.type })
        var options = askOptions()

        val unscopedTypes = types.map { it.type }.filter { (scopes["all"].orEmpty() + scopes[it].orEmpty()).isEmpty() }
        if (unscopedTypes.isNotEmpty() && !options.allowCustomScopes && !options.allowEmptyScopes) {
            section {
                yellow {
                    textLine("\n${unscopedTypes.joinToString()} would have no usable scopes, so empty scopes have been allowed.")
                }
            }.run()
            options = options.copy(allowEmptyScopes = true)
        }

        return KommitConfig(types, scopes, options)
    }

    private fun Session.step(
        number: Int,
        title: String,
    ) = section { bold { textLine("\nStep $number/3: $title") } }.run()

    private fun Session.askTypes(): List<TypeEntry> {
        step(1, "Commit types")
        while (true) {
            val picked =
                readMultiSelect(
                    "Select the commit types to offer:",
                    standardTypes.map { "${it.type.padEnd(8)} ${it.description}" },
                    standardTypes.indices.filter { standardTypes[it].type in defaultTypeNames }.toSet(),
                )
            val types = picked.sorted().map { standardTypes[it] }.toMutableList()

            while (true) {
                val raw =
                    readInputRetry(
                        "\nAdd a custom type as 'name: description' (leave blank to continue):",
                        { it.isBlank() || parseCustomType(it, types) != null },
                        "Type names must be new, can't be 'all', and may only use letters, numbers, '.', '_', '/', and '-'.",
                    ).trim()
                if (raw.isEmpty()) break
                types += parseCustomType(raw, types)!!
            }

            if (types.isNotEmpty()) return types
            section { red { textLine("Choose or add at least one type.") } }.run()
        }
    }

    private fun parseCustomType(
        raw: String,
        existing: List<TypeEntry>,
    ): TypeEntry? {
        val parts = raw.split(":", limit = 2)
        val name = parts[0].trim()
        if (!name.matches(nameRegex) || name == "all" || existing.any { it.type == name }) return null
        return TypeEntry(name, parts.getOrNull(1)?.trim().orEmpty())
    }

    private fun Session.askScopes(typeNames: List<String>): Map<String, List<String>> {
        step(2, "Scopes")
        section { textLine("Scopes name the part of the project a commit touches, like with 'parser' in 'feat(parser)'.") }.run()

        val shared = mutableListOf<String>()
        val scopes = linkedMapOf<String, List<String>>()
        val historyScopes = scopesFromHistory()
        if (historyScopes.isNotEmpty()) {
            val owner = historyScopes.mapValues { (_, used) -> used.singleOrNull()?.takeIf { it in typeNames } }
            val names = historyScopes.keys.toList()
            val picked =
                readMultiSelect(
                    "\nThese scopes appear in this repo's history. Select the ones to keep:",
                    names.map { name -> owner[name]?.let { "${name.padEnd(12)} only for $it" } ?: name },
                )
            picked.sorted().map { names[it] }.forEach { name ->
                val type = owner[name]
                if (type == null) shared += name else scopes[type] = scopes[type].orEmpty() + name
            }
        }
        shared += readScopeList("\nEnter scopes to offer for every type, comma-separated (leave blank to skip):", shared)
        if (shared.isNotEmpty()) scopes["all"] = shared

        while (true) {
            val type =
                readInputRetry(
                    "\nAdd scopes that only apply to one type? Enter the type (leave blank to continue):",
                    { it.isBlank() || it.trim() in typeNames },
                    "Unknown type. Choose one of: ${typeNames.joinToString()}.",
                    *typeNames.toTypedArray(),
                ).trim()
            if (type.isEmpty()) break
            val existing = shared + scopes[type].orEmpty()
            val added = readScopeList("Enter scopes for '$type', comma-separated:", existing)
            if (added.isNotEmpty()) scopes[type] = scopes[type].orEmpty() + added
        }

        return scopes
    }

    private fun Session.readScopeList(
        prompt: String,
        exclude: List<String>,
    ): List<String> {
        fun split(raw: String) = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return split(
            readInputRetry(
                prompt,
                { raw -> split(raw).all { it.matches(nameRegex) } },
                "Scopes may only use letters, numbers, '.', '_', '/', and '-'.",
            ),
        ).distinct() - exclude.toSet()
    }

    private val headerRegex = Regex("""^([A-Za-z]+)\(([^)]+)\)!?:""")

    private fun scopesFromHistory(): Map<String, Set<String>> =
        git("log", "--format=%s", "-n", "500")
            .lines
            .mapNotNull { headerRegex.find(it)?.destructured }
            .map { (type, scope) -> scope.trim() to type }
            .filter { (scope, _) -> scope.matches(nameRegex) }
            .groupBy({ it.first }, { it.second })
            .entries
            .sortedByDescending { it.value.size }
            .associate { (scope, types) -> scope to types.toSet() }

    private fun Session.askOptions(): KommitOptions.ResolvedOptions {
        step(3, "Options")
        val defaults = KommitOptions.withDefaults()
        val modes = StageMode.entries.map { it.name.lowercase() }

        val stageMode =
            readInputRetry(
                "How should Kommit stage files? (default ${defaults.stageMode.name.lowercase()})\n" +
                    "  none    commit only what you've already staged\n" +
                    "  review  pick which changed files to commit\n" +
                    "  auto    stage everything when nothing is staged",
                { it.isBlank() || it.trim().lowercase() in modes },
                "Choose one of: ${modes.joinToString()}.",
                *modes.toTypedArray(),
            ).trim().lowercase().let { raw ->
                StageMode.entries.firstOrNull { it.name.lowercase() == raw } ?: defaults.stageMode
            }

        val autoPush = readConfirm("\nPush automatically after each commit?", defaults.autoPush)
        val allowBody = readConfirm("\nPrompt for a multi-line commit body?", defaults.allowBody)
        val allowBreakingChanges = readConfirm("\nAsk whether each commit is a breaking change?", defaults.allowBreakingChanges)
        val allowIssues = readConfirm("\nAsk for issue references?", defaults.allowIssues)
        val allowCustomScopes = readConfirm("\nAllow scopes that aren't in this config?", defaults.allowCustomScopes)
        val allowEmptyScopes = readConfirm("\nAllow commits without a scope?", defaults.allowEmptyScopes)

        var changesPrefix = defaults.changesPrefix
        var issuePrefix = defaults.issuePrefix
        if ((allowBreakingChanges || allowIssues) && readConfirm("\nCustomize the commit footer prefixes?", false)) {
            if (allowBreakingChanges) {
                changesPrefix =
                    readInput("Prefix for breaking changes (leave blank for '$changesPrefix'):").trim().ifEmpty { changesPrefix }
            }
            if (allowIssues) {
                issuePrefix = readInput("Prefix for issue references (leave blank for '$issuePrefix'):").trim().ifEmpty { issuePrefix }
            }
        }

        return defaults.copy(
            stageMode = stageMode,
            autoPush = autoPush,
            allowBreakingChanges = allowBreakingChanges,
            allowIssues = allowIssues,
            allowBody = allowBody,
            allowCustomScopes = allowCustomScopes,
            allowEmptyScopes = allowEmptyScopes,
            changesPrefix = changesPrefix,
            issuePrefix = issuePrefix,
        )
    }

    override fun MainRenderScope.showHelp() {
        textLine("Interactively create a configuration file for Kommit")
        textLine()
        textLine("Usage: kommit create [options]")
        textLine()
        textLine("Options:")
        textLine("  -c, --config PATH   Where to write the configuration file (default: .kommit.yaml)")
        textLine("  -d, --defaults      Write the default configuration without prompting")
        textLine("  -h, --help          Show this help message")
    }
}
