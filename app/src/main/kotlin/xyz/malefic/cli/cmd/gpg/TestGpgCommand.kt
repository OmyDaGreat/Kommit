package xyz.malefic.cli.cmd.gpg

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import xyz.malefic.cli.cmd.util.processPipe

/**
 * Command for testing if the GPG agent is working.
 * This command runs "echo "test" | gpg --clearsign" to verify GPG signing functionality.
 */
class TestGpgCommand :
    CliktCommand(
        name = "test",
    ) {
    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context) = context.theme.info(brightBlue("Test if the GPG agent is working by signing a test message"))

    /**
     * Executes the command to test the GPG agent.
     */
    override fun run() {
        echo(brightBlue("Testing GPG agent..."))
        testGpgAgent()
    }

    /**
     * Tests the GPG agent by running "echo "test" | gpg --clearsign".
     * Displays the result of the signing operation.
     */
    private fun testGpgAgent() =
        try {
            val process = processPipe("gpg", "--clearsign")

            process.outputStream.bufferedWriter().use { writer ->
                writer.write("test")
                writer.flush()
            }

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                echo(green("GPG agent is working correctly!"))
                echo("\nSigned output:")
                echo(output)
            } else {
                val errorOutput = process.errorStream.bufferedReader().readText()
                echo(red("GPG agent test failed. Exit code: $exitCode"))
                if (errorOutput.isNotEmpty()) {
                    echo(red("Error: $errorOutput"))
                } else {
                    echo(red("No error output available"))
                }
            }
        } catch (e: Exception) {
            echo(red("Error testing GPG agent: ${e.message}"), err = true)
        }
}
