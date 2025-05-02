package xyz.malefic.cli

import com.github.ajalt.clikt.core.main

/**
 * The entry point of the application.
 *
 * This function processes the command-line arguments and invokes the `Kommit` command-line interface.
 * If no arguments are provided, it defaults to the 'commit' subcommand.
 *
 * @param args The command-line arguments passed to the application.
 */
fun main(args: Array<String>) =
    Kommit().main(
        args.takeUnless { it.isEmpty() } ?: arrayOf("commit"),
    )
