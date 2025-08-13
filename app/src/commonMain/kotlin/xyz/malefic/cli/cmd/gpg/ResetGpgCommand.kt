package xyz.malefic.cli.cmd.gpg

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.executeCommand

/**
 * Command for resetting the GPG agent.
 */
class ResetGpgCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }

        echo("Resetting GPG agent...")
        resetGpgAgent()
    }

    override fun showHelp() {
        echo("Reset the GPG agent to fix signing issues")
        echo("")
        echo("Usage: kommit gpg reset [options]")
        echo("")
        echo("Options:")
        echo("  -h, --help      Show this help message")
        echo("")
        echo("This command kills the current GPG agent and restarts it.")
    }

    /**
     * Resets the GPG agent by killing the current agent and starting a new one.
     */
    private fun resetGpgAgent() {
        try {
            echo("Killing GPG agent...")
            val killResult = executeCommand("gpgconf", "--kill", "gpg-agent")

            if (killResult.exitCode == 0) {
                echo("GPG agent killed successfully")
            } else {
                echo("Warning: Failed to kill GPG agent (it may not be running)")
                if (killResult.error.isNotEmpty()) {
                    echo("Kill error: ${killResult.error}")
                }
            }

            echo("Starting new GPG agent...")
            val connectResult = executeCommand("gpg-connect-agent", "/bye")

            if (connectResult.exitCode == 0) {
                echo("GPG agent started successfully!")
                echo("GPG agent reset complete.")
            } else {
                echo("Warning: Failed to start GPG agent", err = true)
                if (connectResult.error.isNotEmpty()) {
                    echo("Connect error: ${connectResult.error}", err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error resetting GPG agent: ${e.message}", err = true)
        }
    }
}
