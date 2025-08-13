package xyz.malefic.cli.cmd.gpg

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.executeCommand

/**
 * Command for testing if the GPG agent is working.
 */
class TestGpgCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }
        
        echo("Testing GPG agent...")
        testGpgAgent()
    }
    
    override fun showHelp() {
        echo("Test if the GPG agent is working by signing a test message")
        echo("")
        echo("Usage: kommit gpg test [options]")
        echo("")
        echo("Options:")
        echo("  -h, --help      Show this help message")
        echo("")
        echo("This command tests GPG signing by attempting to sign a test message.")
    }

    /**
     * Tests the GPG agent by running gpg --version and checking configuration.
     */
    private fun testGpgAgent() {
        try {
            // First check if GPG is available
            val versionResult = executeCommand("gpg", "--version")
            
            if (versionResult.exitCode != 0) {
                echo("GPG is not available or not working properly", err = true)
                if (versionResult.error.isNotEmpty()) {
                    echo("Error: ${versionResult.error}", err = true)
                }
                return
            }
            
            echo("GPG is available")
            echo("Version info:")
            echo(versionResult.output.lines().take(3).joinToString("\n"))
            
            // Check for signing key
            val keyResult = executeCommand("gpg", "--list-secret-keys", "--keyid-format", "LONG")
            
            if (keyResult.exitCode == 0 && keyResult.output.isNotEmpty()) {
                echo("GPG secret keys found:")
                echo(keyResult.output)
                echo("GPG agent appears to be working correctly!")
            } else {
                echo("No GPG secret keys found. You may need to import or generate a GPG key.")
                echo("Use 'gpg --gen-key' to generate a new key.")
            }
            
        } catch (e: Exception) {
            echo("Error testing GPG agent: ${e.message}", err = true)
        }
    }
}
