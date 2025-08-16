package xyz.malefic.cli.cmd.commit

import okio.FileSystem
import okio.Path.Companion.toPath
import xyz.malefic.cli.cmd.BaseCommand

/**
 * Command for creating a default configuration file for Kommit.
 */
class CreateConfigCommand : BaseCommand() {
    override fun run(args: Array<String>) {
        if (args.contains("-h") || args.contains("--help")) {
            showHelp()
            return
        }

        createConfigFile()
    }

    override fun showHelp() {
        echo("Create a default configuration file for Kommit")
        echo("")
        echo("Usage: kommit create [options]")
        echo("")
        echo("Options:")
        echo("  -h, --help      Show this help message")
    }

    private fun createConfigFile() {
        val configContent =
            """
            # Simple Conventional Commit Configuration
            
            types:
              - feat: A new feature
              - fix: A bug fix
              - docs: Documentation only changes
              - refactor: A code change that neither fixes a bug nor adds a feature
              - chore: Other changes that don't modify src or test files
            
            scopes:
              all:
                - core
                - ui
                - docs
            """.trimIndent()
        try {
            if (
                FileSystem.SYSTEM.read(".kommit.yaml".toPath()) {
                    readUtf8()
                } == ""
            ) {
                echo(".kommit.yaml already exists. Aborting to avoid overwrite.", err = true)
                return
            }
            echo("Creating default .kommit.yaml config file...")
            FileSystem.SYSTEM.write(".kommit.yaml".toPath()) {
                writeUtf8(configContent)
            }
            echo("Config file created successfully!")
            echo("You can customize the configuration by editing .kommit.yaml")
            echo("The rules are available at https://github.com/OmyDaGreat/Kommit")
        } catch (e: Exception) {
            echo("Error creating config file: ${e.message}", err = true)
        }
    }
}
