package xyz.malefic.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import xyz.malefic.cli.cmd.commit.AmendCommand
import xyz.malefic.cli.cmd.commit.CommitCommand
import xyz.malefic.cli.cmd.commit.CreateConfigCommand
import xyz.malefic.cli.cmd.commit.LogCommand
import xyz.malefic.cli.cmd.gitsys.BranchCommand
import xyz.malefic.cli.cmd.gitsys.FetchCommand
import xyz.malefic.cli.cmd.gitsys.InitCommand
import xyz.malefic.cli.cmd.gitsys.PullCommand
import xyz.malefic.cli.cmd.gitsys.PushCommand
import xyz.malefic.cli.cmd.gitsys.StageCommand
import xyz.malefic.cli.cmd.gitsys.StatusCommand
import xyz.malefic.cli.cmd.gitsys.TagCommand
import xyz.malefic.cli.cmd.gpg.GpgCommand

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
            AmendCommand(),
            BranchCommand(),
            CommitCommand(),
            CreateConfigCommand(),
            FetchCommand(),
            InitCommand(),
            LogCommand(),
            PullCommand(),
            PushCommand(),
            GpgCommand(),
            StageCommand(),
            StatusCommand(),
            TagCommand(),
        )
    }
}
