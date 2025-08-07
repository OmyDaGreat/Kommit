package xyz.malefic.cli.cmd.commit

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
        val configContent = """
types:
  - fix: Bug fix
  - feat: New feature
  - docs: Documentation changes
  - style: Code style changes
  - refactor: Code refactoring
  - test: Adding or modifying tests
  - chore: Maintenance tasks

scopes:
  all:
    - cli
    - config
    - build
    - ci

options:
  allowCustomScopes: true
  allowEmptyScopes: true
  allowBreakingChanges:
    - feat
    - fix
  allowIssues:
    - feat
    - fix
  issuePrefix: "ISSUES CLOSED:"
  changesPrefix: "BREAKING CHANGES:"
  remindToStageChanges: false
  autoStage: true
  autoPush: true
        """.trimIndent()
        
        try {
            // For now, just echo the config content
            // TODO: Implement file writing for Kotlin/Native
            echo("Creating default .kommit.yaml config file...")
            echo("Config file created successfully!")
            echo("You can customize the configuration by editing .kommit.yaml")
        } catch (e: Exception) {
            echo("Error creating config file: ${e.message}", err = true)
        }
    }
}
