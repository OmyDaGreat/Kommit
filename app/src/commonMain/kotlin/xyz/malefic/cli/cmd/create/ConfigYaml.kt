package xyz.malefic.cli.cmd.create

import xyz.malefic.cli.cmd.commit.Parsing.KommitConfig
import xyz.malefic.cli.cmd.commit.Parsing.KommitOptions
import xyz.malefic.cli.cmd.commit.Parsing.TypeEntry

object ConfigYaml {
    val standardTypes =
        listOf(
            TypeEntry("feat", "A new feature"),
            TypeEntry("fix", "A bug fix"),
            TypeEntry("docs", "Documentation only changes"),
            TypeEntry("style", "Changes that do not affect the meaning of the code"),
            TypeEntry("refactor", "A code change that neither fixes a bug nor adds a feature"),
            TypeEntry("perf", "A code change that improves performance"),
            TypeEntry("test", "Adding missing tests or correcting existing tests"),
            TypeEntry("build", "Changes that affect the build system or external dependencies"),
            TypeEntry("ci", "Changes to CI configuration files and scripts"),
            TypeEntry("chore", "Other changes that don't modify src or test files"),
            TypeEntry("revert", "Reverts a previous commit"),
        )
    val defaultTypeNames = setOf("feat", "fix", "docs", "refactor", "chore")

    val defaultConfig =
        KommitConfig(
            types = standardTypes.filter { it.type in defaultTypeNames },
            scopes = mapOf("all" to listOf("core", "ui", "docs")),
            options = KommitOptions.withDefaults(),
        )

    val nameRegex = Regex("[A-Za-z0-9][A-Za-z0-9._/-]*")

    fun KommitConfig.toYaml(): String =
        buildString {
            appendLine("# Conventional Commit configuration for Kommit")
            appendLine("# Rules: https://github.com/OmyDaGreat/Kommit")
            appendLine()
            appendLine("types:")
            types.forEach { (type, description) ->
                if (description.isEmpty()) appendLine("  - $type") else appendLine("  - $type: ${quote(description)}")
            }
            val nonEmptyScopes = scopes.filterValues { it.isNotEmpty() }.entries.sortedBy { it.key != "all" }
            if (nonEmptyScopes.isNotEmpty()) {
                appendLine()
                appendLine("scopes:")
                nonEmptyScopes.forEach { (key, list) ->
                    appendLine("  $key:")
                    list.forEach { appendLine("    - $it") }
                }
            }
            appendLine()
            appendLine("options:")
            with(options) {
                appendLine("  stageMode: ${stageMode.name.lowercase()}")
                appendLine("  autoPush: $autoPush")
                appendLine("  allowBreakingChanges: $allowBreakingChanges")
                appendLine("  allowIssues: $allowIssues")
                appendLine("  allowBody: $allowBody")
                appendLine("  allowCustomScopes: $allowCustomScopes")
                appendLine("  allowEmptyScopes: $allowEmptyScopes")
                if (allowBreakingChanges) appendLine("  changesPrefix: ${quote(changesPrefix)}")
                if (allowIssues) appendLine("  issuePrefix: ${quote(issuePrefix)}")
            }
        }

    /**
     * Double-quote [value] when writing it bare would change its meaning in YAML.
     */
    private fun quote(value: String): String {
        val needsQuotes =
            value.isEmpty() ||
                value != value.trim() ||
                value.first() in "-?:,[]{}#&*!|>'\"%@`" ||
                ": " in value ||
                " #" in value ||
                value.endsWith(":") ||
                value.lowercase() in listOf("true", "false", "null", "yes", "no", "~")
        return if (needsQuotes) "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\"" else value
    }
}
