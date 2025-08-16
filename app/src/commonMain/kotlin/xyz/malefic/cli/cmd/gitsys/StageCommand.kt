package xyz.malefic.cli.cmd.gitsys

import xyz.malefic.cli.cmd.BaseCommand
import xyz.malefic.cli.cmd.util.git

/**
 * A command for staging files for a Git commit.
 */
class StageCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }

        val all = args.contains("-a") || args.contains("--all")
        val files = args.filter { !it.startsWith("-") }

        when {
            all -> stageAllFiles()
            files.isNotEmpty() -> stageFiles(files)
            else -> {
                echo("Error: Specify files to stage or use --all", err = true)
                showHelp()
            }
        }
    }

    override fun showHelp() {
        echo("Stage files for commit (git add)")
        echo("")
        echo("Usage: kommit stage [options] [files...]")
        echo("")
        echo("Options:")
        echo("  -a, --all       Stage all modified and untracked files")
        echo("  -h, --help      Show this help message")
        echo("")
        echo("Examples:")
        echo("  kommit stage file1.txt file2.txt  # Stage specific files")
        echo("  kommit stage --all                # Stage all changes")
    }

    private fun stageAllFiles() {
        try {
            val result = git("add", ".")

            if (result.exitCode == 0) {
                echo("All files staged successfully!")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to stage all files. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error staging all files: ${e.message}", err = true)
        }
    }

    private fun stageFiles(files: List<String>) {
        try {
            val result = git("add", *files.toTypedArray())

            if (result.exitCode == 0) {
                echo("Files staged successfully: ${files.joinToString(", ")}")
                if (result.output.isNotEmpty()) {
                    echo(result.output)
                }
            } else {
                echo("Failed to stage files. Exit code: ${result.exitCode}", err = true)
                if (result.error.isNotEmpty()) {
                    echo(result.error, err = true)
                }
            }
        } catch (e: Exception) {
            echo("Error staging files: ${e.message}", err = true)
        }
    }
}
