package xyz.malefic.cli.cmd.gitsys

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.commit.CreateConfigCommand
import xyz.malefic.cli.cmd.util.executeCommand

/**
 * A command that initializes a Git repository with optional Kommit setup.
 *
 * This command runs the `git init` command to initialize a new Git repository.
 * It also provides an option to create a default Kommit configuration file.
 */
class InitCommand : BaseCommand() {
    /**
     * Executes the `init` command.
     *
     * This method initializes a Git repository and optionally creates a
     * default Kommit configuration file if the `--config` flag is set.
     */
    override fun run(args: Array<String>) {
        val createConfig = args.contains("-c") || args.contains("--config")

        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }

        initRepo()
        if (createConfig) {
            createConfigFile()
        }
    }

    /**
     * Shows help for the init command.
     */
    override fun showHelp() {
        echo("Initialize a Git repository with optional Kommit setup")
        echo("")
        echo("Usage: kommit init [options]")
        echo("")
        echo("Options:")
        echo("  -c, --config    Create a default Kommit configuration file")
        echo("  -h, --help      Show this help message")
    }

    /**
     * Initializes a Git repository by running the `git init` command.
     */
    private fun initRepo() {
        try {
            val result = executeCommand("git", "init")
            if (result.exitCode == 0) {
                echo("Git repository initialized successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to initialize Git repository. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error initializing Git repository: ${e.message}", err = true)
        }
    }

    /**
     * Creates a default Kommit configuration file.
     *
     * This method invokes the `CreateConfigCommand` to generate the configuration file.
     */
    private fun createConfigFile() {
        val createConfigCommand = CreateConfigCommand()
        createConfigCommand.run(emptyArray())
    }
}
