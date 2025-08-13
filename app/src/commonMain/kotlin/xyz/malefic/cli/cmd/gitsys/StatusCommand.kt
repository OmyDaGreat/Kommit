package xyz.malefic.cli.cmd.gitsys

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.git

/**
 * A command for displaying the working tree status of a Git repository.
 */
class StatusCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }

        echo("Checking git status...")
        showStatus()
    }

    override fun showHelp() {
        echo("Show the working tree status")
        echo("")
        echo("Usage: kommit status")
        echo("")
        echo("Options:")
        echo("  -h, --help      Show this help message")
    }

    /**
     * Runs the git status command to display the working tree status.
     */
    private fun showStatus() {
        try {
            val result = git("status")

            if (result.exitCode == 0) {
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to show status. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error showing status: ${e.message}", err = true)
        }
    }
}
