package xyz.malefic.cli.cmd.system

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors
import xyz.malefic.cli.cmd.commit.CreateConfigCommand

/**
 * A command that initializes a Git repository with optional Kommit setup.
 *
 * This command runs the `git init` command to initialize a new Git repository.
 * It also provides an option to create a default Kommit configuration file.
 */
class InitCommand :
    CliktCommand(
        name = "init",
    ) {
    /**
     * A flag indicating whether to create a default Kommit configuration file.
     */
    private val createConfig by option(
        "-c",
        "--config",
        help = "Create a default Kommit configuration file",
    ).flag()

    /**
     * Provides a custom help message for the `init` command.
     *
     * @param context The Clikt command context.
     * @return A string containing the help message.
     */
    override fun help(context: Context): String =
        context.theme.info(TextColors.brightBlue("Initialize a Git repository with optional Kommit setup"))

    /**
     * Executes the `init` command.
     *
     * This method initializes a Git repository and optionally creates a
     * default Kommit configuration file if the `--config` flag is set.
     */
    override fun run() {
        initRepo()
        if (createConfig) {
            createConfigFile()
        }
    }

    /**
     * Initializes a Git repository by running the `git init` command.
     *
     * This method uses a `ProcessBuilder` to execute the `git init` command.
     * It redirects the output and error streams to the console and waits for
     * the process to complete. If the process exits with a non-zero code,
     * an error message is displayed.
     *
     * @throws Exception If an error occurs while executing the `git init` command.
     */
    private fun initRepo() {
        try {
            val initProcess =
                ProcessBuilder("git", "init")
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()

            val exitCode = initProcess.waitFor()
            if (exitCode == 0) {
                echo(TextColors.green("Git repository initialized successfully!"))
            } else {
                echo(TextColors.red("Failed to initialize Git repository. Exit code: $exitCode"), err = true)
            }
        } catch (e: Exception) {
            echo(TextColors.red("Error initializing Git repository: ${e.message}"), err = true)
        }
    }

    /**
     * Creates a default Kommit configuration file.
     *
     * This method invokes the `CreateConfigCommand` to generate the configuration file.
     */
    private fun createConfigFile() {
        val createConfigCommand = CreateConfigCommand()
        createConfigCommand.run()
    }
}
