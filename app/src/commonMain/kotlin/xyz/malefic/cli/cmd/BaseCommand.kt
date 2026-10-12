package xyz.malefic.cli.cmd

import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session

/**
 * Base class for all commands in the Kommit CLI application.
 */
abstract class BaseCommand {
    /**
     * Executes the command with the given arguments.
     */
    abstract fun Session.run(args: Array<String>)

    /**
     * Helper function for Main, wrapping helpChecker w/ run
     */
    fun Session.execute(args: Array<String>) {
        if (helpChecker(args)) return
        run(args)
    }

    /**
     * Checks for help arguments when executing.
     */
    fun Session.helpChecker(args: Array<String>): Boolean {
        var help = false
        section {
            if (args.contains("-h") || args.contains("--help") || args.contains("help")) {
                showHelp()
                help = true
            }
        }.run()
        return help
    }

    /**
     * Shows help for the command.
     */
    open fun MainRenderScope.showHelp() {
        textLine("No help available for this command.")
    }
}
