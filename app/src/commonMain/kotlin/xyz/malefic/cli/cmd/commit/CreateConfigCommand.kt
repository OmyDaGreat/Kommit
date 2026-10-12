package xyz.malefic.cli.cmd.commit

import com.varabyte.kotter.foundation.text.green
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import okio.FileSystem
import okio.Path.Companion.toPath
import xyz.malefic.cli.cmd.BaseCommand

/**
 * Command for creating a default configuration file for Kommit.
 */
class CreateConfigCommand : BaseCommand() {
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

    override fun Session.run(args: Array<String>) {
        try {
            val configPath = ".kommit.yaml".toPath()

            if (FileSystem.SYSTEM.exists(configPath)) {
                section {
                    yellow { textLine(".kommit.yaml already exists. Aborting to avoid overwrite.") }
                }.run()
                return
            }

            section {
                yellow { textLine("Creating default .kommit.yaml config file...") }
            }.run()
            FileSystem.SYSTEM.write(configPath) {
                writeUtf8(configContent)
            }
            section {
                green { textLine("Config file created successfully!") }
                textLine("You can customize the configuration by editing .kommit.yaml")
                textLine("The rules are available at https://github.com/OmyDaGreat/Kommit")
            }.run()
        } catch (e: Exception) {
            section { red { textLine("Error creating config file: ${e.message}") } }.run()
        }
    }

    override fun MainRenderScope.showHelp() {
        textLine("Create a default configuration file for Kommit")
        textLine()
        textLine("Usage: kommit create [options]")
        textLine()
        textLine("Options:")
        textLine("  -h, --help      Show this help message")
    }
}
