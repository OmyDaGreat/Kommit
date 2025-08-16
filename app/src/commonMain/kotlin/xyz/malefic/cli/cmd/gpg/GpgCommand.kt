package xyz.malefic.cli.cmd.gpg

import xyz.malefic.cli.cmd.BaseCommand

/**
 * Parent command for GPG-related operations.
 */
class GpgCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help") || args.isEmpty()) {
            showHelp()
            return
        }

        when (args[0]) {
            "test" -> TestGpgCommand().run(args.drop(1).toTypedArray())
            "reset" -> ResetGpgCommand().run(args.drop(1).toTypedArray())
            else -> {
                echo("Error: Unknown GPG subcommand '${args[0]}'", err = true)
                showHelp()
            }
        }
    }

    override fun showHelp() {
        echo("GPG-related commands for managing and testing GPG signing")
        echo("")
        echo("Usage: kommit gpg <subcommand> [options]")
        echo("")
        echo("Subcommands:")
        echo("  test        Test GPG signing setup")
        echo("  reset       Reset GPG configuration")
        echo("")
        echo("Options:")
        echo("  -h, --help  Show this help message")
        echo("")
        echo("Examples:")
        echo("  kommit gpg test     # Test GPG signing")
        echo("  kommit gpg reset    # Reset GPG config")
    }
}
