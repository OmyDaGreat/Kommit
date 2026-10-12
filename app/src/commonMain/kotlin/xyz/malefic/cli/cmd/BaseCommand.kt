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
     * Shows help for the command.
     */
    open fun MainRenderScope.showHelp() {
        textLine("No help available for this command.")
    }
}
