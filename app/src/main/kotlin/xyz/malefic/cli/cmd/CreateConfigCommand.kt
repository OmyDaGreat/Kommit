package xyz.malefic.cli.cmd

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import xyz.malefic.cli.DEFAULT_CONFIG_PATH
import java.io.File
import kotlin.system.exitProcess

/**
 * Command for creating a default configuration file.
 */
class CreateConfigCommand :
    CliktCommand(
        name = "create",
    ) {
    /**
     * Option to force overwrite of existing configuration file.
     */
    private val force by option(
        "-f",
        "--force",
        help = "Overwrite existing configuration file",
    ).flag()

    /**
     * Executes the command to generate the default configuration file.
     */
    override fun run() {
        generateDefaultConfig()
    }

    /**
     * Generates a default .kommit.yaml file in the current directory.
     * If the file already exists and the force option is not set, an error is displayed.
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
