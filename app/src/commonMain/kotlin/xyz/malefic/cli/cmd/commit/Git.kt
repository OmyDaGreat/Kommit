package xyz.malefic.cli.cmd.commit

import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import xyz.malefic.cli.cmd.util.executeCommand

object Git {
    fun Session.commitChanges(message: String): Boolean =
        try {
            val result = executeCommand("git", "commit", "-m", message)
            if (result.exitCode == 0) {
                section {
                    textLine("Commit created successfully!")
                    yellow { if (result.output.isNotEmpty()) textLine(result.output) }
                }.run()
                true
            } else {
                section {
                    red {
                        textLine("Failed to create commit. Exit code: ${result.exitCode}")
                        if (result.error.isNotEmpty()) {
                            textLine(result.error)
                        }
                    }
                }.run()
                false
            }
        } catch (e: Exception) {
            section {
                red { textLine("Error creating commit: ${e.message}") }
            }.run()
            false
        }

    fun Session.pushChanges() {
        try {
            section {
                textLine("Pushing changes (autoPush enabled)...")
            }.run()
            val result = executeCommand("git", "push")
            if (result.exitCode == 0) {
                section {
                    textLine("Push successful.")
                    yellow { if (result.output.isNotEmpty()) textLine(result.output) }
                }.run()
            } else {
                section {
                    red { textLine("Push failed. Exit code: ${result.exitCode}") }
                    yellow { if (result.error.isNotEmpty()) textLine(result.error) }
                }.run()
            }
        } catch (e: Exception) {
            section {
                red { textLine("Error pushing changes: ${e.message}") }
            }.run()
        }
    }

    fun getStagedFiles(): List<String> =
        try {
            val result = executeCommand("git", "diff", "--cached", "--name-only")
            if (result.exitCode == 0) {
                result.output.lines().mapNotNull { it.trim().ifEmpty { null } }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

    fun MainRenderScope.stageAll(onError: () -> Unit) {
        try {
            val result = executeCommand("git", "add", ".")
            if (result.exitCode != 0) {
                red { textLine("Failed to stage changes automatically.") }
                yellow { if (result.error.isNotEmpty()) textLine(result.error) }
                onError()
            }
        } catch (e: Exception) {
            red { textLine("Error staging changes: ${e.message}") }
            onError()
        }
    }
}
