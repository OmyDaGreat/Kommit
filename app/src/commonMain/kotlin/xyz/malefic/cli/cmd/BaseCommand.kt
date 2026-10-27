package xyz.malefic.cli.cmd

import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session

internal const val DEFAULT_CONFIG_PATH = ".kommit.yaml"

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
     * Returns the path passed with -c/--config, or [DEFAULT_CONFIG_PATH].
     */
    protected fun extractConfigPath(args: Array<String>): String {
        val configIndex = args.indexOfFirst { it == "-c" || it == "--config" }
        return if (configIndex >= 0 && configIndex + 1 < args.size) {
            args[configIndex + 1]
        } else {
            DEFAULT_CONFIG_PATH
        }
    }

    /**
     * Shows help for the command.
     */
    open fun MainRenderScope.showHelp() {
        textLine("No help available for this command.")
    }
}
