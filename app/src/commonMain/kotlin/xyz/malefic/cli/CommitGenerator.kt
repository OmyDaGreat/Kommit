package xyz.malefic.cli

/**
 * The entry point of the application.
 *
 * This function processes the command-line arguments and invokes the appropriate command handler.
 * If no arguments are provided, it defaults to the 'commit' subcommand.
 *
 * @param args The command-line arguments passed to the application.
 */
fun main(args: Array<String>) {
    val command = args.getOrNull(0) ?: "commit"
    val commandArgs = if (args.isNotEmpty()) args.drop(1).toTypedArray() else emptyArray()
    
    when (command) {
        "commit" -> xyz.malefic.cli.cmd.commit.CommitCommand().run(commandArgs)
        "amend" -> xyz.malefic.cli.cmd.commit.AmendCommand().run(commandArgs)
        "branch" -> xyz.malefic.cli.cmd.gitsys.BranchCommand().run(commandArgs)
        "create" -> xyz.malefic.cli.cmd.commit.CreateConfigCommand().run(commandArgs)
        "fetch" -> xyz.malefic.cli.cmd.gitsys.FetchCommand().run(commandArgs)
        "init" -> xyz.malefic.cli.cmd.gitsys.InitCommand().run(commandArgs)
        "log" -> xyz.malefic.cli.cmd.commit.LogCommand().run(commandArgs)
        "pull" -> xyz.malefic.cli.cmd.gitsys.PullCommand().run(commandArgs)
        "push" -> xyz.malefic.cli.cmd.gitsys.PushCommand().run(commandArgs)
        "gpg" -> xyz.malefic.cli.cmd.gpg.GpgCommand().run(commandArgs)
        "stage" -> xyz.malefic.cli.cmd.gitsys.StageCommand().run(commandArgs)
        "status" -> xyz.malefic.cli.cmd.gitsys.StatusCommand().run(commandArgs)
        "tag" -> xyz.malefic.cli.cmd.gitsys.TagCommand().run(commandArgs)
        "help", "--help", "-h" -> showHelp()
        else -> {
            println("Unknown command: $command")
            println("Use 'kommit help' for available commands")
        }
    }
}

private fun showHelp() {
    println("Kommit - A Kotlin-based conventional commit message generator")
    println()
    println("Usage: kommit [<options>] <command> [<args>]...")
    println()
    println("Commands:")
    println("  amend   Amend the last commit")
    println("  branch  Interactive branch management")
    println("  commit  Generate a commit message")
    println("  create  Create a default configuration file for Kommit")
    println("  fetch   Fetch from a remote repository")
    println("  init    Initialize a Git repository with optional Kommit setup")
    println("  log     Display Git logs or generate a changelog")
    println("  pull    Pull changes from the remote repository")
    println("  push    Push commits to the remote repository")
    println("  gpg     GPG-related commands for managing and testing GPG signing")
    println("  stage   Stage files for commit (git add)")
    println("  status  Show the working tree status")
    println("  tag     Create or list tags")
    println("  help    Show this help message")
}
