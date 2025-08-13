package xyz.malefic.cli.cmd

/**
 * Platform-specific output function for proper stderr handling
 */
expect fun writeOutput(message: String, err: Boolean)

/**
 * Base class for all commands in the Kommit CLI application.
 */
abstract class BaseCommand {
    
    /**
     * Executes the command with the given arguments.
     */
    abstract fun run(args: Array<String>)
    
    /**
     * Prints a message to the console.
     * If err is true, writes to stderr, otherwise to stdout.
     */
    protected fun echo(message: String, err: Boolean = false) {
        writeOutput(message, err)
    }
    
    /**
     * Reads a line of input from the user.
     */
    protected fun readLine(): String? {
        return readlnOrNull()
    }
    
    /**
     * Shows help for the command.
     */
    open fun showHelp() {
        echo("No help available for this command.")
    }
}