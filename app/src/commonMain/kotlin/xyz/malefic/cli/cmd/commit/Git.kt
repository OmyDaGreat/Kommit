package xyz.malefic.cli.cmd.commit

import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import xyz.malefic.cli.cmd.util.ProcessResult
import xyz.malefic.cli.cmd.util.executeCommand

/**
 * Execute a git command with the specified arguments
 */
fun git(vararg args: String) = executeCommand("git", *args)

object Git {
    fun Session.commitChanges(message: String): Boolean =
        try {
            val result = git("commit", "-m", message)
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
                        if (result is ProcessResult.Error) {
                            textLine(result.output)
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
            val result = git("push")
            if (result is ProcessResult.Success) {
                section {
                    textLine("Push successful.")
                    yellow { if (result.output.isNotEmpty()) textLine(result.output) }
                }.run()
            } else {
                section {
                    red {
                        textLine("Push failed. Exit code: ${result.exitCode}")
                        if (result.output.isNotEmpty()) textLine(result.output)
                    }
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
            val result = git("diff", "--cached", "--name-only")
            if (result is ProcessResult.Success) {
                result.output.lines().mapNotNull { it.trim().ifEmpty { null } }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

    fun MainRenderScope.stageAll(onError: () -> Unit) {
        try {
            val result = git("add", ".")
            if (result is ProcessResult.Error) {
                red {
                    textLine("Failed to stage changes automatically.")
                    if (result.output.isNotEmpty()) textLine(result.output)
                }
                onError()
            }
        } catch (e: Exception) {
            red { textLine("Error staging changes: ${e.message}") }
            onError()
        }
    }
}
