package xyz.malefic.cli.cmd.gitsys

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Text
import xyz.malefic.cli.cmd.util.git
import xyz.malefic.cli.cmd.util.gitStream
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Command for interactively managing Git branches.
 */
class BranchCommand : CliktCommand(name = "branch") {
    private val terminal = Terminal()
    private var branches = listOf<String>()
    private var currentBranch = ""

    /**
     * Provides help information for the command.
     * @param context The context in which the command is executed.
     * @return A string containing the help information.
     */
    override fun help(context: Context): String = context.theme.info("Interactive branch management")

    /**
     * Executes the command to manage branches interactively.
     */
    override fun run() {
        branches = getAllBranches()
        if (branches.isEmpty()) {
            terminal.println(TextColors.red("No branches found"))
            return
        }

        currentBranch = getCurrentBranch()

        while (true) {
            terminal.println(Text(TextColors.brightBlue("Git Branch Manager"), whitespace = Whitespace.NORMAL))
            terminal.println(Text("Current branch: ${TextColors.green(currentBranch)}", whitespace = Whitespace.NORMAL))

            val options = listOf("Create new branch", "List all branches") + branches + "Exit"
            val selected = showMenu("Select an option:", options)

            when (selected) {
                "Create new branch" -> createBranchFlow()
                "List all branches" -> listAllBranches(branches, currentBranch)
                "Exit" -> return
                in branches -> handleBranchActions(selected, selected == currentBranch)
                else -> terminal.println(TextColors.red("Invalid selection"))
            }
            branches = getAllBranches()
            currentBranch = getCurrentBranch()
        }
    }

    /**
     * Handles the flow for creating a new branch.
     */
    private fun createBranchFlow() {
        terminal.println(Text("Enter new branch name:", whitespace = Whitespace.NORMAL))
        val branchName = readLine() ?: return

        if (branchName.isBlank()) {
            terminal.println(TextColors.red("Branch name cannot be empty"))
            return
        }

        val shouldCheckout = showMenu("Would you like to checkout this branch?", listOf("Yes", "No")) == "Yes"

        if (shouldCheckout) {
            createAndCheckoutBranch(branchName)
        } else {
            createBranch(branchName)
        }
    }

    /**
     * Handles actions for a selected branch.
     * @param branch The name of the selected branch.
     * @param isCurrentBranch Whether the selected branch is the current branch.
     */
    private fun handleBranchActions(
        branch: String,
        isCurrentBranch: Boolean,
    ) {
        val actions = mutableListOf<String>()

        if (!isCurrentBranch) {
            actions.add("Checkout")
            actions.add("Merge into current branch")
            actions.add("Rebase onto this branch")
            actions.add("Delete branch")
        }

        actions.add("Back")

        val action = showMenu("What would you like to do with branch '$branch'?", actions)

        when (action) {
            "Checkout" -> checkoutBranch(branch)
            "Merge into current branch" -> mergeCurrentIntoBranch(branch)
            "Rebase onto this branch" -> rebaseCurrentOntoBranch(branch)
            "Delete branch" -> deleteBranch(branch)
            "Back" -> return
        }
    }

    /**
     * Retrieves all branches in the repository.
     * @return A list of branch names.
     */
    private fun getAllBranches(): List<String> {
        try {
            val process = gitStream("branch")

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val branches = mutableListOf<String>()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val branch = line?.trim()?.replace("* ", "") ?: continue
                branches.add(branch)
            }

            process.waitFor()
            return branches
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error listing branches: ${e.message}"))
            return emptyList()
        }
    }

    /**
     * Retrieves the name of the current branch.
     * @return The name of the current branch.
     */
    private fun getCurrentBranch(): String {
        try {
            val process = gitStream("branch", "--show-current")

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val currentBranch = reader.readLine()?.trim() ?: "unknown"

            process.waitFor()
            return currentBranch
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error getting current branch: ${e.message}"))
            return "unknown"
        }
    }

    /**
     * Displays a menu with options and prompts the user to select one.
     * @param prompt The prompt message to display.
     * @param options The list of options to display.
     * @return The selected option.
     */
    private fun showMenu(
        prompt: String,
        options: List<String>,
    ): String {
        terminal.println(Text(prompt, whitespace = Whitespace.NORMAL))

        for ((index, option) in options.withIndex()) {
            terminal.println("${index + 1}) $option")
        }

        terminal.println(Text("Enter selection:", whitespace = Whitespace.NORMAL))

        while (true) {
            val input = readLine()?.trim() ?: "0"
            val selection = input.toIntOrNull()

            if (selection != null && selection in 1..options.size) {
                return options[selection - 1]
            }

            terminal.println(TextColors.red("Invalid selection. Please enter a number between 1 and ${options.size}"))
        }
    }

    /**
     * Lists all branches and highlights the current branch.
     * @param branches The list of all branches.
     * @param currentBranch The name of the current branch.
     */
    private fun listAllBranches(
        branches: List<String>,
        currentBranch: String,
    ) {
        terminal.println(Text("All branches:", whitespace = Whitespace.NORMAL))

        for (branch in branches) {
            if (branch == currentBranch) {
                terminal.println("* ${TextColors.green(branch)}")
            } else {
                terminal.println("  $branch")
            }
        }

        terminal.println(Text("Press Enter to continue", whitespace = Whitespace.NORMAL))
        readLine()
    }

    /**
     * Deletes the specified branch.
     * @param branch The name of the branch to delete.
     */
    private fun deleteBranch(branch: String) {
        try {
            val process = git("branch", "-d", branch)

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                terminal.println(TextColors.green("Branch '$branch' deleted successfully!"))
            } else {
                terminal.println(TextColors.red("Failed to delete branch. Exit code: $exitCode"))
            }
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error deleting branch: ${e.message}"))
        }
    }

    /**
     * Checks out the specified branch.
     * @param branch The name of the branch to checkout.
     */
    private fun checkoutBranch(branch: String) {
        try {
            val process = git("checkout", branch)

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                terminal.println(TextColors.green("Switched to branch '$branch'"))
            } else {
                terminal.println(TextColors.red("Failed to checkout branch. Exit code: $exitCode"))
            }
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error checking out branch: ${e.message}"))
        }
    }

    /**
     * Creates a new branch with the specified name.
     * @param name The name of the branch to create.
     */
    private fun createBranch(name: String) {
        try {
            val process = git("branch", name)

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                terminal.println(TextColors.green("Branch '$name' created successfully!"))
            } else {
                terminal.println(TextColors.red("Failed to create branch. Exit code: $exitCode"))
            }
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error creating branch: ${e.message}"))
        }
    }

    /**
     * Creates and checks out a new branch with the specified name.
     * @param name The name of the branch to create and checkout.
     */
    private fun createAndCheckoutBranch(name: String) {
        try {
            val process = git("checkout", "-b", name)

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                terminal.println(TextColors.green("Branch '$name' created and checked out successfully!"))
            } else {
                terminal.println(TextColors.red("Failed to create and checkout branch. Exit code: $exitCode"))
            }
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error creating and checking out branch: ${e.message}"))
        }
    }

    /**
     * Merges the specified branch into the current branch.
     * @param branch The name of the branch to merge.
     */
    private fun mergeCurrentIntoBranch(branch: String) {
        try {
            val process = git("merge", branch)

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                terminal.println(TextColors.green("Merged branch '$branch' into current branch"))
            } else {
                terminal.println(TextColors.red("Failed to merge branch. Exit code: $exitCode"))
            }
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error merging branch: ${e.message}"))
        }
    }

    /**
     * Rebases the current branch onto the specified branch.
     * @param branch The name of the branch to rebase onto.
     */
    private fun rebaseCurrentOntoBranch(branch: String) {
        try {
            val process = git("rebase", branch)

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                terminal.println(TextColors.green("Rebased current branch onto '$branch'"))
            } else {
                terminal.println(TextColors.red("Failed to rebase branch. Exit code: $exitCode"))
            }
        } catch (e: Exception) {
            terminal.println(TextColors.red("Error rebasing branch: ${e.message}"))
        }
    }
}
