package xyz.malefic.cli

import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.bold
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.Session
import xyz.malefic.cli.cmd.commit.CommitCommand
import xyz.malefic.cli.cmd.create.CreateCommand
import xyz.malefic.cli.cmd.gpg.GpgCommand

/**
 * The entry point of the application.
 *
 * This function processes the command-line arguments and invokes the appropriate command handler.
 * If no arguments are provided, it defaults to the 'commit' subcommand.
 *
 * @param args The command-line arguments passed to the application.
 */
fun main(args: Array<String>) {
    val name = args.getOrNull(0) ?: "commit"
    val commandArgs = if (args.isNotEmpty()) args.drop(1).toTypedArray() else emptyArray()

    val command =
        when (name) {
            "commit" -> CommitCommand()
            "create" -> CreateCommand()
            "gpg" -> GpgCommand()
            else -> null
        }

    session {
        when {
            command != null -> {
                with(command) { this@session.execute(commandArgs) }
            }

            name in listOf("help", "--help", "-h") -> {
                showHelp()
            }

            else -> {
                println("Unknown command: $name")
                println("Use 'kommit help' for available commands")
            }
        }
    }
    command?.runDeferred()
}

private fun Session.showHelp() {
    section {
        bold { textLine("Kommit - A Kotlin-based conventional commit message generator") }
        textLine()
        textLine("Usage: kommit [<options>] <command> [<args>]...")
        textLine()
        textLine("Commands:")
        textLine("  commit  Generate a commit message")
        textLine("  create  Interactively create a configuration file for Kommit")
        textLine("  gpg     GPG-related commands for managing and testing GPG signing")
        textLine("  help    Show this help message")
    }.run()
}
