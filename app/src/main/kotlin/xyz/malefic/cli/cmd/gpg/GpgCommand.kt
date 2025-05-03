package xyz.malefic.cli.cmd.gpg

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.mordant.rendering.TextColors.brightBlue

/**
 * Parent command for GPG-related operations.
 * This command groups together various GPG-related subcommands.
 */
class GpgCommand :
    CliktCommand(
        name = "gpg",
    ) {
    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context) = context.theme.info(brightBlue("GPG-related commands for managing and testing GPG signing"))

    init {
        subcommands(
            TestGpgCommand(),
            ResetGpgCommand(),
        )
    }

    /**
     * Executes the command. Since this is a parent command with subcommands,
     * it doesn't perform any action on its own.
     */
    override fun run() {
        // This is a parent command with subcommands, so no action is needed here
    }
}
