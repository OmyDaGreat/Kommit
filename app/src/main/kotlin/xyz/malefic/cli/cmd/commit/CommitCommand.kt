package xyz.malefic.cli.cmd.commit

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import org.yaml.snakeyaml.Yaml
import xyz.malefic.cli.DEFAULT_CONFIG_PATH
import xyz.malefic.cli.cmd.system.PushCommand
import xyz.malefic.cli.cmd.system.stageAllFiles
import xyz.malefic.cli.cmd.util.git
import xyz.malefic.cli.cmd.util.gitPipe
import xyz.malefic.cli.cmd.util.nullGet
import java.io.File
import kotlin.system.exitProcess

/**
 * Command for generating a commit message based on the configuration file.
 */
class CommitCommand :
    CliktCommand(
        name = "commit",
    ) {
    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context) = context.theme.info("Generate a commit message")

    /**
     * Path to the configuration file.
     */
    private val configPath by option(
        "-c",
        "--config",
        help = "Path to the configuration file",
    ).default(DEFAULT_CONFIG_PATH)

    private val types = mutableListOf<CommitType>()
    private val scopes = mutableMapOf<String, List<String>>()
    private val allowBreakingChanges = mutableListOf<String>()
    private val allowIssues = mutableListOf<String>()
    private var allowCustomScopes = true
    private var allowEmptyScopes = true
    private var issuePrefix = "ISSUES CLOSED:"
    private var changesPrefix = "BREAKING CHANGES:"
    private var remindToStageChanges = false
    private var autoStage = true
    private var autoPush = true

    /**
     * Executes the command to load the configuration and generate the commit message.
     */
    override fun run() {
        try {
            loadConfig()
            generateCommit()
        } catch (e: Exception) {
            echo("Error: ${e.message}", err = true)
            exitProcess(1)
        }
    }

    /**
     * Loads the configuration from the specified config file.
     * Parses the file and initializes commit types, scopes, and options.
     */
    private fun loadConfig() {
        val configFile = File(configPath)
        require(configFile.exists()) { "Config file not found at: $configPath" }

        val yaml = Yaml()
        val config = yaml.load<Map<String, Any>>(configFile.readText())

        parseTypes(config["types"] as List<Map<String, String>>)
        parseScopes(config["scopes"] as Map<String, List<String>>)
        parseOptions(config["options"] as? Map<String, Any>)
    }

    /**
     * Parses the list of commit types from the configuration.
     * @param typesList The list of commit types, each represented as a map with key-value pairs.
     */
    private fun parseTypes(typesList: List<Map<String, String>>) {
        typesList.forEach { type ->
            type.forEach { (key, value) ->
                types.add(CommitType(key, value))
            }
        }
    }

    /**
     * Parses the map of scopes from the configuration.
     * @param scopesMap The map of scopes.
     */
    private fun parseScopes(scopesMap: Map<String, List<String>>) {
        scopes.putAll(scopesMap)
    }

    /**
     * Parses the options from the configuration.
     * @param options The map of options with their corresponding values.
     */
    private fun parseOptions(options: Map<String, Any>?) {
        allowCustomScopes = options.nullGet("allowCustomScopes", true) as Boolean
        allowEmptyScopes = options.nullGet("allowEmptyScopes", true) as Boolean
        issuePrefix = options.nullGet("issuePrefix", "ISSUES CLOSED:") as String
        changesPrefix = options.nullGet("changesPrefix", "BREAKING CHANGES:") as String
        remindToStageChanges = options.nullGet("remindToStageChanges", false) as Boolean
        autoStage = options.nullGet("autoStage", true) as Boolean
        autoPush = options.nullGet("autoPush", true) as Boolean
        allowBreakingChanges.addAll(options.nullGet("allowBreakingChanges", emptyList<String>()) as List<String>)
        allowIssues.addAll(options.nullGet("allowIssues", emptyList<String>()) as List<String>)
    }

    /**
     * Generates a conventional commit message based on user input.
     * Prompts the user to select a commit type, enter a scope, description, and other details.
     * Displays the generated commit message and optionally commits the changes.
     */
    private fun generateCommit() {
        echo("Generating conventional commit...")

        if (!hasStagedChanges()) {
            if (autoStage) {
                echo("\nAuto-staging changes...")
                stageAllFiles()
            } else if (remindToStageChanges) {
                echo("\nPlease stage your changes before kommiting!")
            }
        }

        val type = promptSelection("Select the type of change", types.map { "${it.value} - ${it.name}" })
        val selectedType = types[type].value

        val scope = promptForScope(selectedType)

        val shortDescription = promptForInput("Enter a short description")

        val canBeBreaking = selectedType in allowBreakingChanges

        val longDescription = promptForLongDescription()

        val isBreaking =
            if (canBeBreaking) {
                promptYesNo("Are there any breaking changes?")
            } else {
                false
            }

        val breakingChanges = if (isBreaking) promptForInput("Describe the breaking changes") else ""

        val issues = promptForIssues()

        val commitMessage =
            buildCommitMessage(
                selectedType,
                scope,
                shortDescription,
                longDescription,
                isBreaking,
                breakingChanges,
                issues,
            )

        echo("\nGenerated Commit Message:")
        echo(commitMessage)

        if (promptYesNo("\nDo you want to commit with this message?", defaultNo = false)) {
            commitChanges(commitMessage)
        } else {
            echo("Commit aborted.")
        }
    }

    /**
     * Prompts the user to select an option from a list.
     * @param message The message to display to the user.
     * @param options The list of options to choose from.
     * @return The index of the selected option.
     */
    private fun promptSelection(
        message: String,
        options: List<String>,
    ): Int {
        echo("\n$message:")
        options.forEachIndexed { index, option ->
            echo("${index + 1}. $option")
        }

        var selection: Int
        while (true) {
            echo("Enter your choice (1-${options.size}): ", trailingNewline = false)
            try {
                selection = readLine()?.toInt()?.minus(1) ?: -1
                if (selection in options.indices) {
                    break
                } else {
                    echo("Invalid selection. Please try again.")
                }
            } catch (_: NumberFormatException) {
                echo("Please enter a valid number.")
            }
        }

        return selection
    }

    /**
     * Prompts the user to enter a scope.
     * Handles cases where scopes are empty or custom scopes are allowed.
     * @return The entered or selected scope.
     */
    private fun promptForScope(type: String): String {
        if (scopes[type]?.isEmpty() ?: scopes["all"].isNullOrEmpty()) {
            return handleEmptyScopes()
        }

        val options = buildScopeOptions(type)
        val selection = promptSelection("Select a scope", options)
        return handleScopeSelection(selection, options)
    }

    /**
     * Handles the case where scopes are empty.
     * @return An empty string if empty scopes are allowed, otherwise exits the process.
     */
    private fun handleEmptyScopes(): String =
        when {
            !allowCustomScopes && !allowEmptyScopes -> {
                echo("No scopes available and empty disabled. Exiting.")
                exitProcess(1)
            }
            !allowCustomScopes -> ""
            else -> promptForInput("Enter scope")
        }

    /**
     * Builds the list of scope options for the user to select from.
     * @return The list of scope options.
     */
    private fun buildScopeOptions(type: String): List<String> {
        val baseScopes = scopes[type] ?: scopes["all"] ?: emptyList()
        return baseScopes +
            listOfNotNull(
                if (allowCustomScopes) "Other (custom scope)" else null,
                if (allowEmptyScopes) "None (empty scope)" else null,
            )
    }

    /**
     * Handles the user's scope selection.
     * @param selection The index of the selected scope.
     * @param options The list of scope options.
     * @return The selected or entered scope.
     */
    private fun handleScopeSelection(
        selection: Int,
        options: List<String>,
    ): String =
        when {
            allowEmptyScopes && selection == options.size - 1 -> ""
            allowCustomScopes && selection == options.size - 2 -> promptForInput("Enter custom scope")
            else -> options[selection]
        }

    /**
     * Prompts the user to enter a value.
     * @param message The message to display to the user.
     * @return The entered value.
     */
    private fun promptForInput(message: String): String {
        echo("\n$message:")
        return readLine()?.trim() ?: ""
    }

    /**
     * Prompts the user to enter a longer description.
     * @return The entered longer description.
     */
    private fun promptForLongDescription(): String {
        if (!promptYesNo("Do you want to add a longer description?")) return ""

        echo("\nEnter a longer description (press Enter twice when finished):")
        return generateSequence { readLine() }
            .takeWhile { it.isNotBlank() }
            .joinToString("\n")
            .trim()
    }

    /**
     * Prompts the user to enter issue references.
     * @return The entered issue references.
     */
    private fun promptForIssues(): String {
        val hasIssues = promptYesNo("Does this commit close any issues?")
        if (!hasIssues) return ""

        return promptForInput("Enter issue references (e.g., #123, #456)")
    }

    /**
     * Prompts the user with a yes/no question.
     * @param message The message to display to the user.
     * @param defaultNo Whether the default answer is no.
     * @return True if the user answered yes, false otherwise.
     */
    private fun promptYesNo(
        message: String,
        defaultNo: Boolean = true,
    ): Boolean {
        echo("\n$message (y/n):")
        val response = readLine()?.lowercase() ?: ""
        return if (defaultNo) {
            response == "y" || response == "yes"
        } else {
            response != "n" && response != "no"
        }
    }

    /**
     * Builds the commit message based on the provided details.
     * @param type The type of the commit.
     * @param scope The scope of the commit.
     * @param shortDescription The short description of the commit.
     * @param longDescription The longer description of the commit.
     * @param isBreaking Whether the commit includes breaking changes.
     * @param breakingChanges The description of the breaking changes.
     * @param issues The issues closed by the commit.
     * @return The generated commit message.
     */
    private fun buildCommitMessage(
        type: String,
        scope: String,
        shortDescription: String,
        longDescription: String,
        isBreaking: Boolean,
        breakingChanges: String,
        issues: String,
    ) = buildString {
        append(type)
        if (scope.isNotBlank()) append("($scope)")
        if (isBreaking) append("!")
        append(": $shortDescription")

        if (longDescription.isNotBlank()) append("\n\n$longDescription")
        if (isBreaking && breakingChanges.isNotBlank()) append("\n\n$changesPrefix $breakingChanges")
        if (issues.isNotBlank()) append("\n\n$issuePrefix $issues")
    }

    /**
     * Commits the changes using the generated commit message.
     * @param message The commit message.
     */
    private fun commitChanges(message: String) {
        try {
            val process = git("commit", "-m", message)

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                echo("Commit created successfully!")

                if (autoPush) {
                    echo("Auto-pushing changes to remote...")
                    PushCommand().run()
                }
            } else {
                echo("Failed to create commit. Exit code: $exitCode")
            }
        } catch (e: Exception) {
            echo("Error creating commit: ${e.message}")
        }
    }

    /**
     * Checks if there are any staged changes in the Git repository.
     * @return True if there are staged changes, false otherwise.
     */
    private fun hasStagedChanges(): Boolean =
        try {
            val process = gitPipe("diff", "--cached", "--name-only")

            val output =
                process.inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
            process.waitFor() == 0 && output.isNotEmpty()
        } catch (e: Exception) {
            echo("Error checking staged changes: ${e.message}")
            false
        }

    /**
     * Data class representing a commit type.
     * @param value The value of the commit type.
     * @param name The name of the commit type.
     */
    data class CommitType(
        val value: String,
        val name: String,
    )
}
