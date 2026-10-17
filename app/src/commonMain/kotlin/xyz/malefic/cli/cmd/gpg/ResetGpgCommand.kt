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
import xyz.malefic.cli.cmd.util.executeCommand

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

        try {
            var killed by liveVarOf<Boolean?>(null)
            var killResult by liveVarOf<ProcessResult?>(null)
            section {
                killed?.let {
                    if (it) {
                        green { textLine("GPG agent killed successfully") }
                    } else {
                        red {
                            textLine("Warning: Failed to kill GPG agent (it may not be running)")
                            if (killResult!! is ProcessResult.Error) {
                                textLine("Kill error: ${killResult!!.output}")
                            }
                        }
                    }
                } ?: run {
                    yellow { textLine("Killing GPG agent...") }
                }
            }.run {
                killResult = executeCommand("gpgconf", "--kill", "gpg-agent")
                killed = killResult!!.exitCode == 0
            }

            var started by liveVarOf<Boolean?>(null)
            var connectResult by liveVarOf<ProcessResult?>(null)
            section {
                started?.let {
                    if (it) {
                        green { textLine("GPG agent started successfully!") }
                        green { textLine("GPG agent reset complete") }
                    } else {
                        red {
                            textLine("Failed to start GPG agent")
                            if (connectResult!! is ProcessResult.Error) {
                                textLine("Connect error: ${connectResult!!.output}")
                            }
                        }
                    }
                } ?: run {
                    yellow { textLine("Starting new GPG agent...") }
                }
            }.run {
                connectResult = executeCommand("gpg-connect-agent", "/bye")
                started = connectResult!!.exitCode == 0
            }
        } catch (e: Exception) {
            section { red { textLine("Error resetting GPG agent: ${e.message}") } }.run()
        }
    }
}
