package xyz.malefic.cli.cmd.gpg

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import xyz.malefic.cli.cmd.util.process

/**
 * Command for resetting the GPG agent.
 * This can be useful when experiencing issues with GPG signing for commits.
 */
class ResetGpgCommand :
    CliktCommand(
        name = "reset",
    ) {
    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context) = context.theme.info(brightBlue("Reset the GPG agent to fix signing issues"))

    /**
     * Executes the command to reset the GPG agent.
     */
    override fun run() {
        echo(brightBlue("Resetting GPG agent..."))
        resetGpgAgent()
    }
}

/**
 * Resets the GPG agent by killing the current agent and starting a new one.
 * Executes 'gpgconf --kill gpg-agent' followed by 'gpg-connect-agent'.
 */
fun CliktCommand.resetGpgAgent() =
    try {
        killGpgAgent()
        connectGpgAgent()
    } catch (e: Exception) {
        echo(red("Error resetting GPG agent: ${e.message}"), err = true)
    }

/**
 * Starts a new GPG agent by executing 'gpg-connect-agent'.
 * Redirects the output and error streams to the console.
 */
fun CliktCommand.connectGpgAgent() {
    val connectProcess = process("gpg-connect-agent", "/bye")

    val connectExitCode = connectProcess.waitFor()
    echo(
        when (connectExitCode) {
            0 -> green("GPG agent started successfully!")
            else -> red("Failed to start GPG agent. Exit code: $connectExitCode")
        },
    )
}

/**
 * Kills the current GPG agent by executing 'gpgconf --kill gpg-agent'.
 * Redirects the output and error streams to the console.
 */
fun CliktCommand.killGpgAgent() {
    val killProcess = process("gpgconf", "--kill", "gpg-agent")

    val killExitCode = killProcess.waitFor()
    if (killExitCode != 0) {
        echo(red("Warning: Failed to kill GPG agent. Exit code: $killExitCode"), err = true)
    }
}
