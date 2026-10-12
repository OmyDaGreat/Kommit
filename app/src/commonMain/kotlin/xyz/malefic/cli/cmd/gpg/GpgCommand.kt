package xyz.malefic.cli.cmd.gpg

import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import xyz.malefic.cli.cmd.BaseCommand

/**
 * Parent command for GPG-related operations.
 */
class GpgCommand : BaseCommand() {
    override fun Session.run(args: Array<String>) {
        when (args[0]) {
            "test" -> {
                with(TestGpgCommand()) { run(args.drop(1).toTypedArray()) }
            }

            "reset" -> {
                with(ResetGpgCommand()) { run(args.drop(1).toTypedArray()) }
            }

            "" -> {
                section { showHelp() }.run()
            }

            else -> {
                section {
                    red { textLine("Error: Unknown GPG subcommand '${args[0]}'") }
                    showHelp()
                }.run()
            }
        }
    }

    override fun MainRenderScope.showHelp() {
        textLine("GPG-related commands for managing and testing GPG signing")
        textLine()
        textLine("Usage: kommit gpg <subcommand> [options]")
        textLine()
        textLine("Subcommands:")
        textLine("  test        Test GPG signing setup")
        textLine("  reset       Reset GPG configuration")
        textLine()
        textLine("Options:")
        textLine("  -h, --help  Show this help message")
        textLine()
        textLine("Examples:")
        textLine("  kommit gpg test     # Test GPG signing")
        textLine("  kommit gpg reset    # Reset GPG config")
    }
}
