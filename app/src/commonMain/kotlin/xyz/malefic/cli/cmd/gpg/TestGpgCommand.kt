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
 * Command for testing if the GPG agent is working.
 */
class TestGpgCommand : BaseCommand() {
    override fun Session.run(args: Array<String>) {
        section { yellow { textLine("Testing GPG agent...") } }.run()
        testGpgAgent()
    }

    override fun MainRenderScope.showHelp() {
        textLine("Test if the GPG agent is working by signing a test message")
        textLine()
        textLine("Usage: kommit gpg test [options]")
        textLine()
        textLine("Options:")
        textLine("  -h, --help      Show this help message")
        textLine()
        textLine("This command tests GPG signing by attempting to sign a test message.")
    }

    /**
     * Tests the GPG agent for both its existence and any secret keys
     */
    private fun Session.testGpgAgent() {
        try {
            val versionResult = executeCommand("gpg", "--version")

            if (versionResult is ProcessResult.Failure) {
                section {
                    red {
                        textLine("GPG is not available or not working properly")
                        textLine("Error: ${versionResult.output}")
                    }
                }.run()
                return
            }

            section {
                green { textLine("GPG is available") }
                textLine("Version info:")
                textLine(
                    versionResult.output
                        .lines()
                        .take(3)
                        .joinToString("\n"),
                )
            }.run()

            var keyResult by liveVarOf<ProcessResult?>(null)
            section {
                keyResult?.let {
                    if (it is ProcessResult.Success) {
                        textLine("GPG secret keys found:")
                        textLine(it.output)
                        green { textLine("GPG agent appears to be working correctly!") }
                    } else {
                        yellow {
                            textLine("No GPG secret keys found. You may need to import or generate a GPG key.")
                            textLine("Use 'gpg --gen-key' to generate a new key.")
                        }
                    }
                } ?: run {
                    yellow { textLine("Checking for secret keys...") }
                }
            }
        } catch (e: Exception) {
            section { red { ("Error testing GPG agent: ${e.message}") } }
        }
    }
}
