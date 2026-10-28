package xyz.malefic.cli.cmd.commit

import xyz.malefic.cli.cmd.util.runCommand
import xyz.malefic.cli.cmd.util.runInteractive

/**
 * Execute a git command with the specified arguments, capturing its output
 */
fun git(vararg args: String) = runCommand("git", *args)

object Git {
    /**
     * A file with changes in the index and/or working tree. [paths] holds both sides of a rename.
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

    fun stage(paths: List<String>) = git("add", "-A", "--", *paths.toTypedArray())

    fun unstage(paths: List<String>) = git("reset", "-q", "--", *paths.toTypedArray())

    fun getChangedFiles(): List<ChangedFile> =
        git("-c", "core.quotePath=false", "status", "--porcelain=v1", "--untracked-files=all").lines.map { line ->
            val status = line.take(2)
            val (first, rest) = parsePorcelainPath(line, 3)
            val paths = if (rest.startsWith(" -> ")) listOf(parsePorcelainPath(rest, 4).first, first) else listOf(first)
            ChangedFile(status, paths, status[0] != ' ' && status[0] != '?')
        }

    /** Commit attached to the terminal, so GPG signing can prompt for a passphrase. Run outside the Kotter session. */
    fun commit(message: String) = runInteractive("git", "commit", "-m", message)

    /** Push attached to the terminal, so git can prompt for credentials. Run outside the Kotter session. */
    fun push() = runInteractive("git", "push")

    /**
     * Read one path from porcelain output starting at [start], undoing git's C-style quoting.
     * Returns the path and whatever text follows it.
     */
    private fun parsePorcelainPath(
        text: String,
        start: Int,
    ): Pair<String, String> {
        if (text.getOrNull(start) != '"') {
            val end = text.indexOf(" -> ", start).takeIf { it >= 0 } ?: text.length
            return text.substring(start, end) to text.substring(end)
        }
        val bytes = mutableListOf<Byte>()
        var i = start + 1
        while (i < text.length && text[i] != '"') {
            if (text[i] == '\\' && i + 1 < text.length) {
                val c = text[++i]
                if (c in '0'..'7') {
                    val octal = text.substring(i, minOf(i + 3, text.length))
                    bytes += octal.toInt(8).toByte()
                    i += 3
                    continue
                }
                bytes +=
                    when (c) {
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        'a' -> '\u0007'
                        'b' -> '\b'
                        'f' -> '\u000C'
                        'v' -> '\u000B'
                        else -> c
                    }.code.toByte()
            } else {
                val end = (i until text.length).firstOrNull { text[it] == '\\' || text[it] == '"' } ?: text.length
                bytes += text.substring(i, end).encodeToByteArray().toList()
                i = end
                continue
            }
            i++
        }
        return bytes.toByteArray().decodeToString() to text.substring(minOf(i + 1, text.length))
    }
}
