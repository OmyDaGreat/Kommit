package xyz.malefic.cli.cmd.commit

import xyz.malefic.cli.cmd.util.runCommand
import xyz.malefic.cli.cmd.util.runInteractive

/**
 * Execute a git command with the specified arguments, capturing its output.
 * Pathspecs are literal, so a file named like a glob (`*.kt`) or pathspec magic (`:foo`) only matches itself.
 */
fun git(
    vararg args: String,
    input: String? = null,
) = runCommand("git", "--literal-pathspecs", *args, input = input)

object Git {
    /**
     * A file with changes in the index and/or working tree.
     *
     * @param status The status of the file.
     * @param paths The paths of the file, if it's a rename.
     * @param staged Whether the file is staged in the index.
     */
    data class ChangedFile(
        val status: String,
        val paths: List<String>,
        val staged: Boolean,
    ) {
        val display get() = "$status ${paths.joinToString(" <- ")}"
    }

    fun getStagedFiles() = git("diff", "--cached", "--name-only").lines

    fun stageAll() = git("add", ".")

    fun stage(paths: List<String>) = git("add", "-A", "--pathspec-from-file=-", input = paths.joinToString("\n"))

    fun unstage(paths: List<String>) = git("reset", "-q", "--pathspec-from-file=-", input = paths.joinToString("\n"))

    fun getChangedFiles(): List<ChangedFile> =
        git("-c", "core.quotePath=false", "status", "--porcelain=v2", "--untracked-files=all").lines.mapNotNull { line ->
            val fieldCount =
                when (line[0]) {
                    '1' -> 9
                    '2' -> 10
                    'u' -> 11
                    '?' -> return@mapNotNull ChangedFile("??", listOf(line.drop(2)), false)
                    else -> return@mapNotNull null
                }
            val fields = line.split(' ', limit = fieldCount)
            val xy = fields[1].replace('.', ' ')
            ChangedFile(xy, fields.last().split('\t'), xy[0] != ' ')
        }

    /** Commit attached to the terminal, so GPG signing can prompt for a passphrase. Run outside the Kotter session. */
    fun commit(message: String) = runInteractive("git", "commit", "-m", message)

    /** Push attached to the terminal, so git can prompt for credentials. Run outside the Kotter session. */
    fun push() = runInteractive("git", "push")
}
