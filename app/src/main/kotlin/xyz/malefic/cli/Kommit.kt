package xyz.malefic.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.yaml.snakeyaml.Yaml
import java.io.File
import kotlin.system.exitProcess

private const val DEFAULT_CONFIG_PATH = ".kommit.yaml"

/**
 * A Kotlin command line tool for generating conventional commits based on a YAML-inspired config file
 * inspired by cz-customizable but with a simpler format.
 */
class Kommit :
    NoOpCliktCommand(
        name = "kommit",
    ) {
    override val printHelpOnEmptyArgs = true

    init {
        subcommands(CommitCommand(), CreateConfigCommand())
    }
}

class CommitCommand :
    CliktCommand(
        name = "commit",
    ) {
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
    private var changesPrefix = "BREAKING CHANGE:"
    private var remindToStageChanges = true

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
     * @throws IllegalArgumentException if the config file is not found or no commit types are defined.
     */
    private fun loadConfig() {
        val configFile = File(configPath)
        require(configFile.exists()) { "Config file not found at: $configPath" }

        val yaml = Yaml()
        val config = yaml.load<Map<String, Any>>(configFile.readText())

        parseTypes(config["types"] as List<Map<String, String>>)
        parseScopes(config["scopes"] as Map<String, List<String>>)
        parseOptions(config["options"] as Map<String, Any>)
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
    private fun parseOptions(options: Map<String, Any>) {
        allowCustomScopes = options["allowCustomScopes"] as Boolean? != false
        allowEmptyScopes = options["allowEmptyScopes"] as Boolean? != false
        issuePrefix = options["issuePrefix"] as String? ?: "ISSUES CLOSED:"
        changesPrefix = options["changesPrefix"] as String? ?: "BREAKING CHANGE:"
        remindToStageChanges = options["remindToStageChanges"] as Boolean? != false
        allowBreakingChanges.addAll(options["allowBreakingChanges"] as List<String>? ?: emptyList())
        allowIssues.addAll(options["allowIssues"] as List<String>? ?: emptyList())
    }

    /**
     * Generates a conventional commit message based on user input.
     * Prompts the user to select a commit type, enter a scope, description, and other details.
     * Displays the generated commit message and optionally commits the changes.
     */
    private fun generateCommit() {
        echo("Generating conventional commit...")

        if (remindToStageChanges && !hasStagedChanges()) {
            echo("\nPlease stage your changes before kommiting!")
        }

        // Step 1: Select type
        val type = promptSelection("Select the type of change", types.map { "${it.value} - ${it.name}" })
        val selectedType = types[type].value

        // Step 2: Enter scope
        val scope = promptForScope(selectedType)

        // Step 3: Enter short description
        val shortDescription = promptForInput("Enter a short description")

        // Check if breaking changes are allowed for this type
        val canBeBreaking = selectedType in allowBreakingChanges

        // Step 4: Enter longer description (optional)
        val longDescription = promptForLongDescription()

        // Step 5: Breaking changes
        val isBreaking =
            if (canBeBreaking) {
                promptYesNo("Are there any breaking changes?")
            } else {
                false
            }

        val breakingChanges = if (isBreaking) promptForInput("Describe the breaking changes") else ""

        // Step 6: Issues closed
        val issues = promptForIssues()

        // Generate the commit message
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

        // Display the commit message
        echo("\nGenerated Commit Message:")
        echo(commitMessage)

        // Confirm and commit
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
        if (!allowCustomScopes) {
            if (!allowEmptyScopes) {
                echo("No scopes available and empty disabled. Exiting.")
                exitProcess(1)
            } else {
                ""
            }
        } else {
            promptForInput("Enter scope")
        }

    /**
     * Builds the list of scope options for the user to select from.
     * @return The list of scope options.
     */
    private fun buildScopeOptions(type: String): List<String> =
        (scopes[type] ?: scopes["all"] ?: emptyList())
            .let {
                if (allowCustomScopes) it + "Other (custom scope)" else it
            }.let {
                if (allowEmptyScopes) it + "None (empty scope)" else it
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
        val useLongDesc = promptYesNo("Do you want to add a longer description?")
        if (!useLongDesc) return ""

        echo("\nEnter a longer description (press Enter twice when finished):")
        val description = StringBuilder()
        var line: String?
        var emptyLineCount = 0

        while (emptyLineCount < 1) {
            line = readLine()
            if (line?.trim()?.isEmpty() == true) {
                emptyLineCount++
            } else {
                emptyLineCount = 0
                description.append(line).append("\n")
            }
        }

        return description.toString().trim()
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
    ): String {
        val sb = StringBuilder()

        // Header: <type>[(scope)][!]: <description>
        sb.append(type)
        if (scope.isNotBlank()) {
            sb.append("(").append(scope).append(")")
        }
        if (isBreaking) {
            sb.append("!")
        }
        sb.append(": ").append(shortDescription)

        // Body
        if (longDescription.isNotBlank()) {
            sb.append("\n\n").append(longDescription)
        }

        // Breaking changes
        if (isBreaking && breakingChanges.isNotBlank()) {
            sb.append("\n\n$changesPrefix ").append(breakingChanges)
        }

        // Footer
        if (issues.isNotBlank()) {
            sb
                .append("\n\n")
                .append(issuePrefix)
                .append(" ")
                .append(issues)
        }

        return sb.toString()
    }

    /**
     * Commits the changes using the generated commit message.
     * @param message The commit message.
     */
    private fun commitChanges(message: String) {
        try {
            val process =
                ProcessBuilder("git", "commit", "-m", message)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                echo("Commit created successfully!")
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
            val process =
                ProcessBuilder("git", "diff", "--cached", "--name-only")
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

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

class CreateConfigCommand :
    CliktCommand(
        name = "create-config",
    ) {
    private val force by option(
        "-f",
        "--force",
        help = "Overwrite existing configuration file",
    ).flag()

    override fun run() {
        generateDefaultConfig()
    }

    /**
     * Generates a default .kommit.yaml file in the current directory.
     */
    private fun generateDefaultConfig() {
        val defaultConfig =
            """
            # Conventional Commit Configuration

            types:
              - feat: A new feature
              - fix: A bug fix
              - docs: Documentation only changes
              - style: Changes that do not affect the meaning of the code
              - refactor: A code change that neither fixes a bug nor adds a feature
              - perf: A code change that improves performance
              - test: Adding missing tests or correcting existing tests
              - build: Changes that affect the build system or external dependencies
              - ci: Changes to our CI configuration files and scripts
              - chore: Other changes that don't modify src or test files
              - revert: Reverts a previous commit

            scopes:
              all:
                - core
                - ui
                - api
              chore:
                - github
                - deps

            options:
              allowCustomScopes: true
              allowEmptyScopes: true
              issuePrefix: "ISSUES CLOSED:"
              changesPrefix: "BREAKING CHANGE:"
              remindToStageChanges: true
              allowBreakingChanges:
                - feat
                - fix
                - refactor
              allowIssues:
                - feat
                - fix
                - docs
            """.trimIndent()

        val configFile = File(DEFAULT_CONFIG_PATH)
        if (configFile.exists() && !force) {
            echo("Error: .kommit.yaml already exists in the current directory. Use --force to overwrite.", err = true)
            exitProcess(1)
        }

        configFile.writeText(defaultConfig)
        echo(".kommit.yaml has been created successfully.")
    }
}
