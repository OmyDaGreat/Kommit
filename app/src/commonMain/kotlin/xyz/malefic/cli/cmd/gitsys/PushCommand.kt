package xyz.malefic.cli.cmd.gitsys

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.git

/**
 * Command for pushing commits to the remote repository.
 */
class PushCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }
        
        val force = args.contains("-f") || args.contains("--force")
        val setUpstream = args.contains("-u") || args.contains("--set-upstream")
        
        val nonFlagArgs = args.filter { !it.startsWith("-") }
        val remote = nonFlagArgs.getOrNull(0)
        val branch = nonFlagArgs.getOrNull(1)
        
        pushCommits(remote, branch, force, setUpstream)
    }
    
    override fun showHelp() {
        echo("Push commits to the remote repository")
        echo("")
        echo("Usage: kommit push [options] [remote] [branch]")
        echo("")
        echo("Arguments:")
        echo("  remote          Remote repository to push to (default: origin)")
        echo("  branch          Branch to push (default: current branch)")
        echo("")
        echo("Options:")
        echo("  -f, --force         Force push")
        echo("  -u, --set-upstream  Set the upstream for the current branch")
        echo("  -h, --help          Show this help message")
        echo("")
        echo("Examples:")
        echo("  kommit push                    # Push current branch to origin")
        echo("  kommit push origin main        # Push main branch to origin")
        echo("  kommit push -u origin feature  # Push and set upstream")
    }

    private fun pushCommits(remote: String?, branch: String?, force: Boolean = false, setUpstream: Boolean = false) {
        try {
            val command = mutableListOf("push")
            
            if (force) {
                command.add("--force")
            }
            
            if (setUpstream) {
                command.add("--set-upstream")
            }
            
            remote?.let { command.add(it) }
            branch?.let { command.add(it) }
            
            echo("Pushing commits...")
            val result = git(*command.toTypedArray())
            
            if (result.exitCode == 0) {
                echo("Push completed successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Push failed. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error pushing commits: ${e.message}", err = true)
        }
    }
}
