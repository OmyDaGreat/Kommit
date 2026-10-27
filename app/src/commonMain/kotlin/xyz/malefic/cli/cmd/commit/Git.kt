package xyz.malefic.cli.cmd.commit

import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.MainRenderScope
import com.varabyte.kotter.runtime.Session
import xyz.malefic.cli.cmd.util.ProcessResult
import xyz.malefic.cli.cmd.util.executeCommand
import xyz.malefic.cli.cmd.util.readMultiSelect

/**
 * Execute a git command with the specified arguments
 */
fun git(vararg args: String) = executeCommand("git", *args)

object Git {
    /**
     * A file with changes in the index and/or working tree. [paths] holds both sides of a rename.
     */
    data class ChangedFile(
        val status: String,
        val paths: List<String>,
        val staged: Boolean,
    ) {
        val display get() = "$status ${paths.joinToString(" <- ")}"
    }

    fun Session.commitChanges(message: String): Boolean =
        try {
            val result = git("commit", "-m", message)
            if (result is ProcessResult.Success) {
                section {
                    textLine("Commit created successfully!")
                    yellow { if (result.output.isNotEmpty()) textLine(result.output) }
                }.run()
                true
            } else {
                section {
                    red {
                        textLine("Failed to create commit.")
                        textLine(result.output)
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
                        textLine("Push failed")
                        textLine(result.output)
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
            if (result is ProcessResult.Failure) {
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

    fun getChangedFiles(): List<ChangedFile> =
        try {
            val result = git("status", "--porcelain=v1", "-z", "--untracked-files=all")
            if (result is ProcessResult.Success) {
                val entries = result.output.split('\u0000').iterator()
                buildList {
                    while (entries.hasNext()) {
                        val entry = entries.next()
                        if (entry.length < 4) continue
                        val status = entry.take(2)
                        val paths = mutableListOf(entry.drop(3))
                        if ('R' in status || 'C' in status) {
                            if (entries.hasNext()) paths += entries.next()
                        }
                        add(ChangedFile(status, paths, status[0] != ' ' && status[0] != '?'))
                    }
                }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

    /**
     * Let the user pick which changed files to commit, then stage the picked ones and unstage the rest.
     * Returns false if nothing was selected or git failed.
     */
    fun Session.reviewStaging(): Boolean {
        val files = getChangedFiles()
        if (files.isEmpty()) {
            section { red { textLine("No changes to commit.") } }.run()
            return false
        }
        val initiallySelected =
            files.indices
                .filter { files[it].staged }
                .toSet()
                .ifEmpty { files.indices.toSet() }
        val selected = readMultiSelect("\nSelect the files to stage and commit:", files.map { it.display }, initiallySelected)
        if (selected.isEmpty()) {
            section { yellow { textLine("No files selected.") } }.run()
            return false
        }

        val toStage = files.filterIndexed { i, _ -> i in selected }.flatMap { it.paths }
        val toUnstage = files.filterIndexed { i, f -> i !in selected && f.staged }.flatMap { it.paths }
        val results =
            listOfNotNull(
                if (toStage.isNotEmpty()) git("add", "-A", "--", *toStage.toTypedArray()) else null,
                if (toUnstage.isNotEmpty()) git("reset", "-q", "--", *toUnstage.toTypedArray()) else null,
            )
        val failure = results.firstOrNull { it is ProcessResult.Failure } ?: return true
        section {
            red {
                textLine("Failed to update staged files.")
                if (failure.output.isNotEmpty()) textLine(failure.output)
            }
        }.run()
        return false
    }
}
