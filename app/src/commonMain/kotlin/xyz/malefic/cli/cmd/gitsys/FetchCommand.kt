package xyz.malefic.cli.cmd.gitsys

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.git

/**
 * Command for fetching changes from the remote repository.
 */
class FetchCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }
        
        val all = args.contains("--all")
        val prune = args.contains("--prune")
        val tags = args.contains("--tags")
        
        val nonFlagArgs = args.filter { !it.startsWith("-") }
        val remote = nonFlagArgs.getOrNull(0)
        val branch = nonFlagArgs.getOrNull(1)
        
        fetchChanges(remote, branch, all, prune, tags)
    }
    
    override fun showHelp() {
        echo("Fetch changes from the remote repository")
        echo("")
        echo("Usage: kommit fetch [options] [remote] [branch]")
        echo("")
        echo("Arguments:")
        echo("  remote          Remote repository to fetch from (default: origin)")
        echo("  branch          Branch to fetch (default: all branches)")
        echo("")
        echo("Options:")
        echo("  --all           Fetch all remotes")
        echo("  --prune         Remove remote-tracking branches that no longer exist")
        echo("  --tags          Fetch all tags")
        echo("  -h, --help      Show this help message")
        echo("")
        echo("Examples:")
        echo("  kommit fetch                    # Fetch from origin")
        echo("  kommit fetch --all              # Fetch from all remotes")
        echo("  kommit fetch origin main        # Fetch main branch from origin")
    }

    private fun fetchChanges(remote: String?, branch: String?, all: Boolean = false, prune: Boolean = false, tags: Boolean = false) {
        try {
            val command = mutableListOf("fetch")
            
            if (all) {
                command.add("--all")
            }
            
            if (prune) {
                command.add("--prune")
            }
            
            if (tags) {
                command.add("--tags")
            }
            
            if (!all) {
                remote?.let { command.add(it) }
                branch?.let { command.add(it) }
            }
            
            echo("Fetching changes...")
            val result = git(*command.toTypedArray())
            
            if (result.exitCode == 0) {
                echo("Fetch completed successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
                if (result.error.isNotEmpty()) {
                    // Git often outputs progress info to stderr, which is normal
                    echo(result.error)
                }
            } else {
                echo("Fetch failed. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error fetching changes: ${e.message}", err = true)
        }
    }
}
