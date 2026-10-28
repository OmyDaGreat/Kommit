package xyz.malefic.cli.cmd.gpg

import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.ProcessResult
import xyz.malefic.cli.cmd.util.runCommand

/**
 * Command for resetting the GPG agent.
 */
class ResetGpgCommand : BaseCommand() {
    override fun Session.run(args: Array<String>) {
        resetGpgAgent()
    }

    override fun MainRenderScope.showHelp() {
        textLine("Reset the GPG agent to fix signing issues")
        textLine()
        textLine("Usage: kommit gpg reset [options]")
        textLine()
        textLine("Options:")
        textLine("  -h, --help      Show this help message")
        textLine()
        textLine("This command kills the current GPG agent and restarts it.")
    }

    /**
     * Resets the GPG agent by killing the current agent and starting a new one.
     */
    private fun Session.resetGpgAgent() {
        section {
            textLine("Resetting the GPG agent...")
        }.run()

        var killResult by liveVarOf<ProcessResult?>(null)
        section {
            killResult?.let {
                if (it.success) {
                    green { textLine("GPG agent killed successfully") }
                } else {
                    red {
                        textLine("Warning: Failed to kill GPG agent (it may not be running)")
                        if (it.message.isNotEmpty()) textLine("Kill error: ${it.message}")
                    }
                }
            } ?: run {
                yellow { textLine("Killing GPG agent...") }
            }
        }.run {
            killResult = runCommand("gpgconf", "--kill", "gpg-agent")
        }

        var connectResult by liveVarOf<ProcessResult?>(null)
        section {
            connectResult?.let {
                if (it.success) {
                    green { textLine("GPG agent started successfully!") }
                    green { textLine("GPG agent reset complete") }
                } else {
                    red {
                        textLine("Failed to start GPG agent")
                        if (it.message.isNotEmpty()) textLine("Connect error: ${it.message}")
                    }
                }
            } ?: run {
                yellow { textLine("Starting new GPG agent...") }
            }
        }.run {
            connectResult = runCommand("gpg-connect-agent", "/bye")
        }
    }
}
