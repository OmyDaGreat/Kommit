package xyz.malefic.cli.cmd.commit

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path.Companion.toPath

object Parsing {
    data class TypeEntry(
        val type: String,
        val description: String = "",
    )

    @Serializable
    data class KommitOptions(
        val autoStage: Boolean? = null,
        val autoPush: Boolean? = null,
        val allowBreakingChanges: Boolean? = null,
        val allowIssues: Boolean? = null,
        val allowBody: Boolean? = null,
        val allowCustomScopes: Boolean? = null,
        val allowEmptyScopes: Boolean? = null,
        val changesPrefix: String? = null,
        val issuePrefix: String? = null,
    ) {
        companion object {
            data class ResolvedOptions(
                val autoStage: Boolean,
                val autoPush: Boolean,
                val allowBreakingChanges: Boolean,
                val allowIssues: Boolean,
                val allowBody: Boolean,
                val allowCustomScopes: Boolean,
                val allowEmptyScopes: Boolean,
                val changesPrefix: String,
                val issuePrefix: String,
            )

            fun withDefaults(raw: KommitOptions? = null): ResolvedOptions {
                val r = raw ?: KommitOptions()
                return ResolvedOptions(
                    autoStage = r.autoStage ?: false,
                    autoPush = r.autoPush ?: false,
                    allowBreakingChanges = r.allowBreakingChanges ?: true,
                    allowIssues = r.allowIssues ?: true,
                    allowBody = r.allowBody ?: true,
                    allowCustomScopes = r.allowCustomScopes ?: true,
                    allowEmptyScopes = r.allowEmptyScopes ?: true,
                    changesPrefix = r.changesPrefix ?: "BREAKING CHANGE:",
                    issuePrefix = r.issuePrefix ?: "ISSUES CLOSED:",
                )
            }
        }
    }

    data class KommitConfig(
        val types: List<TypeEntry>,
        val scopes: Map<String, List<String>>,
        val options: KommitOptions.Companion.ResolvedOptions,
    ) {
        companion object {
            val empty = KommitConfig(emptyList(), emptyMap(), KommitOptions.withDefaults())
        }
    }

    fun parseConfig(configPath: String): KommitConfig {
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

    fun parseTypes(root: YamlMap): List<TypeEntry> {
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

    fun parseScopes(root: YamlMap): Map<String, List<String>> {
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

    fun parseOptions(root: YamlMap): KommitOptions? {
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

        fun stringOrNull(n: YamlNode?): String? = (n as? YamlScalar)?.content?.trim()

        return KommitOptions(
            autoStage = boolOrNull(node["autoStage"]),
            autoPush = boolOrNull(node["autoPush"]),
            allowBreakingChanges = boolOrNull(node["allowBreakingChanges"]),
            allowIssues = boolOrNull(node["allowIssues"]),
            allowBody = boolOrNull(node["allowBody"]),
            allowCustomScopes = boolOrNull(node["allowCustomScopes"]),
            allowEmptyScopes = boolOrNull(node["allowEmptyScopes"]),
            changesPrefix = stringOrNull(node["changesPrefix"]),
            issuePrefix = stringOrNull(node["issuePrefix"]),
        )
    }
}
