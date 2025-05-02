package xyz.malefic.cli.cmd.commit

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
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
    private val terminal = Terminal()

    /**
     * Option to force overwrite of existing configuration file.
     */
    private val force by option(
        "-f",
        "--force",
        help = "Overwrite existing configuration file",
    ).flag()

    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context): String =
        context.theme.info(
            """
            Create a default configuration file for Kommit.

            This command generates a .kommit.yaml file in the current directory with default settings.
            """.trimIndent(),
        )

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
            # Simple Conventional Commit Configuration

            types:
              - feat: A new feature
              - fix: A bug fix
              - docs: Documentation only changes
              - refactor: A code change that neither fixes a bug nor adds a feature
              - chore: Other changes that don't modify src or test files

            scopes:
              all:
                - core
                - ui
                - api
                - docs

            options:
              allowBreakingChanges:
                - feat
                - fix
              allowIssues:
                - feat
                - fix
            """.trimIndent()

        val configFile = File(DEFAULT_CONFIG_PATH)
        if (configFile.exists() && !force) {
            terminal.println(red("Error: .kommit.yaml already exists in the current directory. Use --force to overwrite."))
            exitProcess(1)
        }

        configFile.writeText(defaultConfig)
        terminal.println(green(".kommit.yaml has been created successfully."))
    }
}
