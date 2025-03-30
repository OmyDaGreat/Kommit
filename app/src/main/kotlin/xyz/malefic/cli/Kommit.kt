package xyz.malefic.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import xyz.malefic.cli.cmd.AmendCommand
import xyz.malefic.cli.cmd.CommitCommand
import xyz.malefic.cli.cmd.CreateConfigCommand
import xyz.malefic.cli.cmd.PushCommand
import xyz.malefic.cli.cmd.ResetGpgAgentCommand

internal const val DEFAULT_CONFIG_PATH = ".kommit.yaml"

/**
 * A Kotlin command line tool for generating conventional commits with a YAML config file
 * inspired by cz-customizable but with a simpler format.
 */
class Kommit :
    NoOpCliktCommand(
        name = "kommit",
    ) {
    override val printHelpOnEmptyArgs = true

    init {
        subcommands(CommitCommand(), CreateConfigCommand(), ResetGpgAgentCommand(), PushCommand(), AmendCommand())
    }
}
