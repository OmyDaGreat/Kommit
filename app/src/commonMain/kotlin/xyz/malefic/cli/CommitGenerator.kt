package xyz.malefic.cli

import com.varabyte.kotter.foundation.session
import xyz.malefic.cli.cmd.commit.CommitCommand
import xyz.malefic.cli.cmd.commit.CreateConfigCommand

/**
 * The entry point of the application.
 *
 * This function processes the command-line arguments and invokes the appropriate command handler.
 * If no arguments are provided, it defaults to the 'commit' subcommand.
 *
 * @param args The command-line arguments passed to the application.
 */
fun main(args: Array<String>) =
    session {
        val command = args.getOrNull(0) ?: "commit"
        val commandArgs = if (args.isNotEmpty()) args.drop(1).toTypedArray() else emptyArray()

        when (command) {
            "commit" -> {
                with(CommitCommand()) {
                    this@session.execute(commandArgs)
                }
            }

            "create" -> {
                with(CreateConfigCommand()) {
                    this@session.execute(commandArgs)
                }
            }

            "gpg" -> {
                xyz.malefic.cli.cmd.gpg
                    .GpgCommand()
                    .run(commandArgs)
            }

            "help", "--help", "-h" -> {
                showHelp()
            }

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
    println("  commit  Generate a commit message")
    println("  create  Create a default configuration file for Kommit")
    println("  gpg     GPG-related commands for managing and testing GPG signing")
    println("  help    Show this help message")
}
