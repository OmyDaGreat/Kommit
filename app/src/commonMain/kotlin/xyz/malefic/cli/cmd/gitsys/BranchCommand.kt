package xyz.malefic.cli.cmd.gitsys

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.git

/**
 * Command for managing Git branches.
 */
class BranchCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }
        
        when {
            args.isEmpty() -> listBranches()
            args[0] == "-a" || args[0] == "--all" -> listBranches(showAll = true)
            args[0] == "-d" || args[0] == "--delete" -> {
                if (args.size > 1) {
                    deleteBranch(args[1])
                } else {
                    echo("Error: Branch name required for delete", err = true)
                    showHelp()
                }
            }
            args[0] == "-c" || args[0] == "--create" -> {
                if (args.size > 1) {
                    val checkout = args.contains("--checkout")
                    createBranch(args[1], checkout)
                } else {
                    echo("Error: Branch name required for create", err = true)
                    showHelp()
                }
            }
            else -> checkoutBranch(args[0])
        }
    }
    
    override fun showHelp() {
        echo("Manage Git branches")
        echo("")
        echo("Usage: kommit branch [options] [branch-name]")
        echo("")
        echo("Options:")
        echo("  -a, --all           List all branches (local and remote)")
        echo("  -c, --create NAME   Create a new branch")
        echo("  --checkout          Checkout after creating (use with -c)")
        echo("  -d, --delete NAME   Delete a branch")
        echo("  -h, --help          Show this help message")
        echo("")
        echo("Examples:")
        echo("  kommit branch                    # List local branches")
        echo("  kommit branch main               # Checkout main branch")
        echo("  kommit branch -c feature-branch  # Create new branch")
        echo("  kommit branch -c feature-branch --checkout  # Create and checkout")
        echo("  kommit branch -d old-branch      # Delete branch")
    }

    private fun listBranches(showAll: Boolean = false) {
        try {
            val command = if (showAll) arrayOf("branch", "-a") else arrayOf("branch")
            val result = git(*command)
            
            if (result.exitCode == 0) {
                if (result.output.isEmpty()) {
                    echo("No branches found")
                } else {
                    echo("Branches:")
                    echo(result.output)
                }
            } else {
                echo("Failed to list branches. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error listing branches: ${e.message}", err = true)
        }
    }

    private fun createBranch(branchName: String, checkout: Boolean = false) {
        try {
            val result = if (checkout) {
                git("checkout", "-b", branchName)
            } else {
                git("branch", branchName)
            }
            
            if (result.exitCode == 0) {
                val action = if (checkout) "created and checked out" else "created"
                echo("Branch '$branchName' $action successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to create branch '$branchName'. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error creating branch: ${e.message}", err = true)
        }
    }

    private fun deleteBranch(branchName: String) {
        try {
            val result = git("branch", "-d", branchName)
            
            if (result.exitCode == 0) {
                echo("Branch '$branchName' deleted successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to delete branch '$branchName'. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error deleting branch: ${e.message}", err = true)
        }
    }

    private fun checkoutBranch(branchName: String) {
        try {
            val result = git("checkout", branchName)
            
            if (result.exitCode == 0) {
                echo("Switched to branch '$branchName'")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to checkout branch '$branchName'. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error checking out branch: ${e.message}", err = true)
        }
    }
}
