package xyz.malefic.cli.cmd.gitsys

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.theme
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors
import xyz.malefic.cli.cmd.util.git

/**
 * A command for staging files for a Git commit.
 *
 * This command allows users to stage all modified and untracked files or
 * specific files for a commit using the `git add` command.
 */
class StageCommand :
    CliktCommand(
        name = "stage",
    ) {
    /**
     * A flag indicating whether to stage all modified and untracked files.
     */
    private val all by option("-a", "--all", help = "Stage all modified and untracked files").flag()

    /**
     * A list of file paths to stage.
     */
    private val files by argument().multiple()

    /**
     * Provides a custom help message for the `stage` command.
     *
     * @param context The Clikt command context.
     * @return A string containing the help message.
     */
    override fun help(context: Context): String = context.theme.info(TextColors.brightBlue("Stage files for commit (git add)"))

    /**
     * Executes the `stage` command.
     *
     * If the `--all` flag is set, the command stages all changes. If file paths are provided, it stages only those files. If neither is specified, it displays an error message.
     */
    override fun run() {
        if (all) {
            stageAllFiles()
        } else if (files.isNotEmpty()) {
            stageFiles(files)
        } else {
            echo(TextColors.red("No files specified. Use --all to stage all files or provide file paths."), err = true)
        }
    }
}

/**
 * Stages specific files for a commit.
 *
 * This method runs the `git add` command with the provided file paths.
 * If the command fails, an error message is displayed.
 *
 * @param filePaths A list of file paths to stage.
 * @throws Exception If an error occurs while staging files.
 */
fun CliktCommand.stageFiles(filePaths: List<String>) {
    try {
        val command = mutableListOf("add")
        command.addAll(filePaths)

        val addProcess = git(*command.toTypedArray())

        val exitCode = addProcess.waitFor()
        if (exitCode == 0) {
            echo(TextColors.green("Files staged successfully!"))
        } else {
            echo(TextColors.red("Failed to stage files. Exit code: $exitCode"), err = true)
        }
    } catch (e: Exception) {
        echo(TextColors.red("Error staging files: ${e.message}"), err = true)
    }
}

/**
 * Stages all modified and untracked files.
 *
 * This method runs the `git add .` command to stage all changes. If the
 * command fails, an error message is displayed.
 *
 * @throws Exception If an error occurs while staging files.
 */
fun CliktCommand.stageAllFiles() {
    try {
        val addProcess = git("add", ".")

        val exitCode = addProcess.waitFor()
        if (exitCode == 0) {
            echo(TextColors.green("All changes staged successfully!"))
        } else {
            echo(TextColors.red("Failed to stage changes. Exit code: $exitCode"), err = true)
        }
    } catch (e: Exception) {
        echo(TextColors.red("Error staging changes: ${e.message}"), err = true)
    }
}
