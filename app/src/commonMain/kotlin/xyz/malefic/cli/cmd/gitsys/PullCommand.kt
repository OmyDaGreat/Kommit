package xyz.malefic.cli.cmd.gitsys

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.git

/**
 * Command for pulling changes from the remote repository.
 */
class PullCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }
        
        val rebase = args.contains("--rebase")
        val noCommit = args.contains("--no-commit")
        val fastForwardOnly = args.contains("--ff-only")
        
        val nonFlagArgs = args.filter { !it.startsWith("-") }
        val remote = nonFlagArgs.getOrNull(0)
        val branch = nonFlagArgs.getOrNull(1)
        
        pullChanges(remote, branch, rebase, noCommit, fastForwardOnly)
    }
    
    override fun showHelp() {
        echo("Pull changes from the remote repository")
        echo("")
        echo("Usage: kommit pull [options] [remote] [branch]")
        echo("")
        echo("Arguments:")
        echo("  remote          Remote repository to pull from (default: origin)")
        echo("  branch          Branch to pull (default: current branch)")
        echo("")
        echo("Options:")
        echo("  --rebase        Rebase instead of merge")
        echo("  --no-commit     Don't commit the merge")
        echo("  --ff-only       Fast-forward only")
        echo("  -h, --help      Show this help message")
        echo("")
        echo("Examples:")
        echo("  kommit pull                     # Pull current branch from origin")
        echo("  kommit pull origin main         # Pull main branch from origin")
        echo("  kommit pull --rebase            # Pull with rebase")
    }

    private fun pullChanges(remote: String?, branch: String?, rebase: Boolean = false, noCommit: Boolean = false, fastForwardOnly: Boolean = false) {
        try {
            val command = mutableListOf("pull")
            
            if (rebase) {
                command.add("--rebase")
            }
            
            if (noCommit) {
                command.add("--no-commit")
            }
            
            if (fastForwardOnly) {
                command.add("--ff-only")
            }
            
            remote?.let { command.add(it) }
            branch?.let { command.add(it) }
            
            echo("Pulling changes...")
            val result = git(*command.toTypedArray())
            
            if (result.exitCode == 0) {
                echo("Pull completed successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Pull failed. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error pulling changes: ${e.message}", err = true)
        }
    }
}
