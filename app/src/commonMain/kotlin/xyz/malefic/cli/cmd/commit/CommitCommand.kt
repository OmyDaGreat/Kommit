package xyz.malefic.cli.cmd.commit

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path.Companion.toPath
import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.executeCommand

internal const val DEFAULT_CONFIG_PATH = ".kommit.yaml"

/**
 * Command for generating a commit message based on the configuration file.
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
    }

    private fun extractConfigPath(args: Array<String>): String {
        val configIndex = args.indexOfFirst { it == "-c" || it == "--config" }
        return if (configIndex >= 0 && configIndex + 1 < args.size) {
            args[configIndex + 1]
        } else {
            DEFAULT_CONFIG_PATH
        }
    }

    @Serializable
    data class KommitConfig(
        val types: List<String> = emptyList(),
        val scopes: Map<String, List<String>> = emptyMap(),
    )

    private fun parseConfig(configPath: String): KommitConfig {
        val configContent = FileSystem.SYSTEM.read(configPath.toPath()) { readUtf8() }
        return Yaml.default.decodeFromString(KommitConfig.serializer(), configContent)
    }

    private fun generateCommit(configPath: String) {
        echo("Generating conventional commit...")

        val config = parseConfig(configPath)
        val types =
            config.types.map {
                val entry = it.split(":")
                entry[0].trim() to (entry.getOrNull(1)?.trim() ?: "")
            }

        echo("\nSelect the type of change:")
        types.forEachIndexed { index, (type, description) ->
            echo("${index + 1}. $type - $description")
        }

        print("Enter your choice (1-${types.size}): ")
        val choice = readLine()?.toIntOrNull()?.minus(1)
        if (choice == null || choice !in types.indices) {
            echo("Invalid selection.", err = true)
            return
        }
        val selectedType = types[choice].first

        val scopes = config.scopes["all"] ?: emptyList()
        var scope: String
        if (scopes.isNotEmpty()) {
            echo("\nSelect a scope:")
            scopes.forEachIndexed { index, scope ->
                echo("${index + 1}. $scope")
            }
            print("Enter your choice (1-${scopes.size}, or press Enter to skip): ")
            val scopeChoice = readLine()?.toIntOrNull()?.minus(1)
            scope = if (scopeChoice != null && scopeChoice in scopes.indices) scopes[scopeChoice] else ""
        } else {
            // fallback if no scopes
            print("\nEnter a scope (optional, press Enter to skip): ")
            scope = readLine()?.trim() ?: ""
        }
        print("\nEnter a short description: ")
        val description = readLine()?.trim() ?: ""
        if (description.isEmpty()) {
            echo("Description cannot be empty.", err = true)
            return
        }
        val commitMessage =
            buildString {
                append(selectedType)
                if (scope.isNotEmpty()) {
                    append("($scope)")
                }
                append(": $description")
            }
        echo("\nGenerated Commit Message:")
        echo(commitMessage)
        print("\nDo you want to commit with this message? (y/N): ")
        val response = readLine()?.lowercase()
        if (response == "y" || response == "yes") {
            commitChanges(commitMessage)
        } else {
            echo("Commit aborted.")
        }
    }

    private fun commitChanges(message: String) {
        try {
            val result = executeCommand("git", "commit", "-m", message)
            if (result.exitCode == 0) {
                echo("Commit created successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to create commit. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error creating commit: ${e.message}", err = true)
        }
    }
}
