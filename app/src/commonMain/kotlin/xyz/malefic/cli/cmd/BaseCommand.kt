package xyz.malefic.cli.cmd

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
     */
    protected fun echo(message: String, err: Boolean = false) {
        // For now, just use println. In a full implementation, we would use
        // platform-specific error output for err=true
        println(message)
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