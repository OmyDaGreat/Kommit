package xyz.malefic.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import java.lang.ProcessBuilder.Redirect.INHERIT

/**
 * Command for resetting the GPG agent.
 * This can be useful when experiencing issues with GPG signing for commits.
 */
class ResetGpgAgentCommand :
    CliktCommand(
        name = "reset-gpg",
    ) {
    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context): String = context.theme.info("Reset the GPG agent to fix signing issues")

    /**
     * Executes the command to reset the GPG agent.
     */
    override fun run() {
        echo("Resetting GPG agent...")
        resetGpgAgent()
    }

    /**
     * Resets the GPG agent by killing the current agent and starting a new one.
     * Executes 'gpgconf --kill gpg-agent' followed by 'gpg-connect-agent'.
     */
    private fun resetGpgAgent() {
        try {
            killGpgAgent()
            connectGpgAgent()
        } catch (e: Exception) {
            echo("Error resetting GPG agent: ${e.message}", err = true)
        }
    }

    /**
     * Starts a new GPG agent by executing 'gpg-connect-agent'.
     * Redirects the output and error streams to the console.
     */
    private fun connectGpgAgent() {
        val connectProcess =
            ProcessBuilder("gpg-connect-agent")
                .redirectOutput(INHERIT)
                .redirectError(INHERIT)
                .start()

        val connectExitCode = connectProcess.waitFor()
        if (connectExitCode == 0) {
            echo("GPG agent has been reset successfully!")
        } else {
            echo("Failed to start new GPG agent. Exit code: $connectExitCode", err = true)
        }
    }

    /**
     * Kills the current GPG agent by executing 'gpgconf --kill gpg-agent'.
     * Redirects the output and error streams to the console.
     */
    private fun killGpgAgent() {
        val killProcess =
            ProcessBuilder("gpgconf", "--kill", "gpg-agent")
                .redirectOutput(INHERIT)
                .redirectError(INHERIT)
                .start()

        val killExitCode = killProcess.waitFor()
        if (killExitCode != 0) {
            echo("Warning: Failed to kill GPG agent. Exit code: $killExitCode", err = true)
        }
    }
}
