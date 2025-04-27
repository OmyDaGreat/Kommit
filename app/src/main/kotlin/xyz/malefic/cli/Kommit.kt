package xyz.malefic.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import xyz.malefic.cli.cmd.commit.AmendCommand
import xyz.malefic.cli.cmd.commit.CommitCommand
import xyz.malefic.cli.cmd.commit.CreateConfigCommand
import xyz.malefic.cli.cmd.commit.LogCommand
import xyz.malefic.cli.cmd.misc.ResetGpgCommand
import xyz.malefic.cli.cmd.system.BranchCommand
import xyz.malefic.cli.cmd.system.FetchCommand
import xyz.malefic.cli.cmd.system.InitCommand
import xyz.malefic.cli.cmd.system.PullCommand
import xyz.malefic.cli.cmd.system.PushCommand
import xyz.malefic.cli.cmd.system.StageCommand
import xyz.malefic.cli.cmd.system.StatusCommand
import xyz.malefic.cli.cmd.system.TagCommand

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
        subcommands(
            CommitCommand(),
            AmendCommand(),
            CreateConfigCommand(),
            ResetGpgCommand(),
            PushCommand(),
            LogCommand(),
            BranchCommand(),
            PullCommand(),
            FetchCommand(),
            InitCommand(),
            StageCommand(),
            StatusCommand(),
            TagCommand(),
        )
    }
}
