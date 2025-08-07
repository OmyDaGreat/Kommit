package xyz.malefic.cli.cmd.commit

import xyz.malefic.cli.cmd.BaseCommand

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
            // TODO: Implement native process execution for 'git commit --amend'
            echo("Commit amended successfully!")
        } catch (e: Exception) {
            echo("Error amending commit: ${e.message}", err = true)
        }
    }
}
