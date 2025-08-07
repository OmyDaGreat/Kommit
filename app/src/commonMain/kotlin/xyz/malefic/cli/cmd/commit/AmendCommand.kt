package xyz.malefic.cli.cmd.commit

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.executeCommand

/**
 * Command for amending the last commit.
 */
class AmendCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        val noEdit = args.contains("--no-edit")
        
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }
        
        amendCommit(noEdit)
    }
    
    override fun showHelp() {
        echo("Amend the last commit")
        echo("")
        echo("Usage: kommit amend [options]")
        echo("")
        echo("Options:")
        echo("  --no-edit       Keep the same commit message")
        echo("  -h, --help      Show this help message")
    }

    /**
     * Amends the last commit.
     * @param noEdit If true, keeps the same commit message.
     */
    private fun amendCommit(noEdit: Boolean) {
        try {
            val command = mutableListOf("git", "commit", "--amend")
            if (noEdit) {
                command.add("--no-edit")
            }

            val result = executeCommand(*command.toTypedArray())
            if (result.exitCode == 0) {
                echo("Commit amended successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to amend commit. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error amending commit: ${e.message}", err = true)
        }
    }
}
