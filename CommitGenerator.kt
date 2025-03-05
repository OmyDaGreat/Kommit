import java.io.File
import java.util.*
import kotlin.system.exitProcess

/**
 * A Kotlin script for generating conventional commits based on a YAML-inspired config file
 * inspired by cz-customizable but with a simpler format and no external dependencies.
 */
class CommitGenerator(private val configPath: String) {
    private val scanner = Scanner(System.`in`)
    private val types = mutableListOf<CommitType>()
    private val scopes = mutableListOf<String>()
    private val allowBreakingChanges = mutableListOf<String>()
    private var allowCustomScopes = true
    private var footerPrefix = "ISSUES CLOSED:"
    
    init {
        try {
            loadConfig()
        } catch (e: Exception) {
            println("Error loading configuration: ${e.message}")
            throw e
        }
    }
    
    private fun loadConfig() {
        val configFile = File(configPath)
        if (!configFile.exists()) {
            throw IllegalArgumentException("Config file not found at: $configPath")
        }

        val lines = configFile.readLines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                i++
                continue
            }
            
            when {
                line == "types:" -> {
                    i++
                    while (i < lines.size && lines[i].startsWith("  ")) {
                        val typeLine = lines[i].trim()
                        if (typeLine.startsWith("- ")) {
                            val typeParts = typeLine.substring(2).split(":", limit = 2)
                            if (typeParts.size == 2) {
                                val value = typeParts[0].trim()
                                val name = typeParts[1].trim()
                                types.add(CommitType(value, name))
                            }
                        }
                        i++
                    }
                }
                line == "scopes:" -> {
                    i++
                    while (i < lines.size && lines[i].startsWith("  ")) {
                        val scopeLine = lines[i].trim()
                        if (scopeLine.startsWith("- ")) {
                            val scope = scopeLine.substring(2).trim()
                            scopes.add(scope)
                        }
                        i++
                    }
                }
                line == "options:" -> {
                    i++
                    while (i < lines.size && lines[i].startsWith("  ")) {
                        val optionLine = lines[i].trim()
                        val optionParts = optionLine.split(":", limit = 2)
                        
                        if (optionParts.size == 2) {
                            val key = optionParts[0].trim()
                            val value = optionParts[1].trim()
                            
                            when (key) {
                                "allowCustomScopes" -> {
                                    allowCustomScopes = value.equals("true", ignoreCase = true)
                                }
                                "footerPrefix" -> {
                                    footerPrefix = value
                                }
                                "allowBreakingChanges" -> {
                                    // Check if next lines contain a list
                                    i++
                                    while (i < lines.size && lines[i].trim().startsWith("- ")) {
                                        val type = lines[i].trim().substring(2).trim()
                                        allowBreakingChanges.add(type)
                                        i++
                                    }
                                    continue  // Skip i++ at the end of the loop
                                }
                            }
                        }
                        i++
                    }
                }
                else -> i++
            }
        }
        
        if (types.isEmpty()) {
            throw IllegalStateException("No commit types found in configuration file")
        }
    }
    
    fun generateCommit() {
        println("Generating conventional commit...")
        
        // Step 1: Select type
        val type = promptSelection("Select the type of change", types.map { "${it.value} - ${it.name}" })
        val selectedType = types[type].value
        
        // Step 2: Enter scope
        val scope = promptForScope()
        
        // Step 3: Enter short description
        val shortDescription = promptForInput("Enter a short description")
        
        // Check if breaking changes are allowed for this type
        val canBeBreaking = selectedType in allowBreakingChanges
        
        // Step 4: Enter longer description (optional)
        val longDescription = promptForLongDescription()
        
        // Step 5: Breaking changes
        val isBreaking = if (canBeBreaking) {
            promptYesNo("Are there any breaking changes?")
        } else false
        
        val breakingChanges = if (isBreaking) promptForInput("Describe the breaking changes") else ""
        
        // Step 6: Issues closed
        val issues = promptForIssues()
        
        // Generate the commit message
        val commitMessage = buildCommitMessage(selectedType, scope, shortDescription, longDescription, 
                                               isBreaking, breakingChanges, issues)
        
        // Display the commit message
        println("\nGenerated Commit Message:")
        println(commitMessage)
        
        // Confirm and commit
        if (promptYesNo("\nDo you want to commit with this message?")) {
            commitChanges(commitMessage)
        } else {
            println("Commit aborted.")
        }
    }
    
    private fun promptSelection(message: String, options: List<String>): Int {
        println("\n$message:")
        options.forEachIndexed { index, option ->
            println("${index + 1}) $option")
        }
        
        var selection: Int
        while (true) {
            print("Enter your choice (1-${options.size}): ")
            try {
                selection = scanner.nextLine().toInt() - 1
                if (selection in 0 until options.size) {
                    break
                } else {
                    println("Invalid selection. Please try again.")
                }
            } catch (e: NumberFormatException) {
                println("Please enter a valid number.")
            }
        }
        
        return selection
    }
    
    private fun promptForScope(): String {
        if (scopes.isEmpty() && !allowCustomScopes) {
            return ""
        }
        
        val useScope = promptYesNo("Do you want to add a scope?")
        if (!useScope) return ""
        
        if (scopes.isEmpty()) {
            return promptForInput("Enter scope")
        } else {
            val options = mutableListOf<String>().apply {
                addAll(scopes)
                if (allowCustomScopes) {
                    add("Other (custom scope)")
                }
            }
            
            val selection = promptSelection("Select a scope", options)
            return if (allowCustomScopes && selection == scopes.size) {
                promptForInput("Enter custom scope")
            } else {
                options[selection]
            }
        }
    }
    
    private fun promptForInput(message: String): String {
        println("\n$message:")
        return scanner.nextLine().trim()
    }
    
    private fun promptForLongDescription(): String {
        val useLongDesc = promptYesNo("Do you want to add a longer description?")
        if (!useLongDesc) return ""
        
        println("\nEnter a longer description (press Enter twice when finished):")
        val description = StringBuilder()
        var line: String
        var emptyLineCount = 0
        
        while (emptyLineCount < 1) {
            line = scanner.nextLine()
            if (line.trim().isEmpty()) {
                emptyLineCount++
            } else {
                emptyLineCount = 0
                description.append(line).append("\n")
            }
        }
        
        return description.toString().trim()
    }
    
    private fun promptForIssues(): String {
        val hasIssues = promptYesNo("Does this commit close any issues?")
        if (!hasIssues) return ""
        
        return promptForInput("Enter issue references (e.g., #123, #456)")
    }
    
    private fun promptYesNo(message: String): Boolean {
        println("\n$message (y/n):")
        val response = scanner.nextLine().lowercase()
        return response == "y" || response == "yes"
    }
    
    private fun buildCommitMessage(
        type: String,
        scope: String,
        shortDescription: String,
        longDescription: String,
        isBreaking: Boolean,
        breakingChanges: String,
        issues: String
    ): String {
        val sb = StringBuilder()
        
        // Header: <type>[(scope)][!]: <description>
        sb.append(type)
        if (scope.isNotBlank()) {
            sb.append("(").append(scope).append(")")
        }
        if (isBreaking) {
            sb.append("!")
        }
        sb.append(": ").append(shortDescription)
        
        // Body
        if (longDescription.isNotBlank()) {
            sb.append("\n\n").append(longDescription)
        }
        
        // Breaking changes
        if (isBreaking && breakingChanges.isNotBlank()) {
            sb.append("\n\nBREAKING CHANGE: ").append(breakingChanges)
        }
        
        // Footer
        if (issues.isNotBlank()) {
            sb.append("\n\n").append(footerPrefix).append(" ").append(issues)
        }
        
        return sb.toString()
    }
    
    private fun commitChanges(message: String) {
        try {
            val process = ProcessBuilder("git", "commit", "-m", message)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                println("Commit created successfully!")
            } else {
                println("Failed to create commit. Exit code: $exitCode")
            }
        } catch (e: Exception) {
            println("Error creating commit: ${e.message}")
        }
    }
    
    data class CommitType(val value: String, val name: String)
}

fun main(args: Array<String>) {
    val configPath = if (args.isNotEmpty()) args[0] else ".kommit.yaml"
    try {
        val generator = CommitGenerator(configPath)
        generator.generateCommit()
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}
