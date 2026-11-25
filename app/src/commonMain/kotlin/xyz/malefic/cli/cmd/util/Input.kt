package xyz.malefic.cli.cmd.util

import com.varabyte.kotter.foundation.collections.liveListOf
import com.varabyte.kotter.foundation.input.CharKey
import com.varabyte.kotter.foundation.input.Completions
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.text.black
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import com.varabyte.kotter.runtime.Session

fun Session.readInput(
    prompt: String,
    vararg completions: String,
): String {
    var value by liveVarOf("")
    section {
        textLine(prompt)
        text("> ")
        input(Completions(*completions, ignoreCase = false))
    }.runUntilInputEntered {
        onInputEntered { value = input }
    }
    return value
}

fun Session.readInputRetry(
    prompt: String,
    condition: (String) -> Boolean,
    retryPrompt: String = "Invalid input, please try again.",
    vararg completions: String,
): String {
    var value by liveVarOf("")
    var retryCounter by liveVarOf(0)
    section {
        textLine(prompt)
        if (retryCounter > 0) {
            red {
                textLine(retryPrompt)
            }
        }
        text("> ")
        input(Completions(*completions, ignoreCase = false))
    }.runUntilSignal {
        onInputEntered {
            if (!condition(input)) {
                retryCounter++
                clearInput()
            } else {
                value = input
                signal()
            }
        }
    }
    return value
}

fun Session.readMultilineInput(prompt: String): String {
    val lines = liveListOf<String>()
    section {
        textLine(prompt)
        lines.forEach { textLine("> $it") }
        text("> ")
        input()
    }.runUntilSignal {
        onInputEntered {
            if (input.isBlank()) {
                signal()
            } else {
                lines += input.trimEnd()
                clearInput()
            }
        }
    }
    return lines.joinToString("\n")
}

fun Session.readInputOptions(
    prompt: String,
    vararg completions: String,
    retryPrompt: String = "Invalid input, please choose from the available options: ${completions.joinToString(", ")}.",
) = readInputRetry(
    prompt,
    { it in completions },
    retryPrompt,
    *completions,
)

/**
 * Ask a yes/no question, returning [default] when the user just presses enter.
 */
fun Session.readConfirm(
    prompt: String,
    default: Boolean,
): Boolean {
    val answer =
        readInputRetry(
            "$prompt ${if (default) "(Y/n)" else "(y/N)"}:",
            { it.trim().lowercase() in listOf("", "y", "n", "yes", "no") },
            "Please answer y or n.",
            "y",
            "n",
            "yes",
            "no",
        ).trim().lowercase()
    return if (answer.isEmpty()) default else answer in listOf("y", "yes")
}

/**
 * Show a checklist of [options] and return the indices the user left selected.
 * Up/down arrows to move, space to toggle, 'a' to toggle everything, & enter to confirm.
 */
fun Session.readMultiSelect(
    prompt: String,
    options: List<String>,
    initiallySelected: Set<Int> = options.indices.toSet(),
): Set<Int> {
    var selected by liveVarOf(initiallySelected)
    var highlighted by liveVarOf(0)
    var confirmed by liveVarOf(false)
    section {
        textLine(prompt)
        if (!confirmed) textLine("(up/down to move, space to toggle, a to toggle all, enter to confirm)")
        options.forEachIndexed { i, option ->
            val line = "${if (i in selected) "[x]" else "[ ]"} $option"
            if (i == highlighted && !confirmed) cyan { textLine("> $line") } else textLine("  $line")
        }
    }.runUntilSignal {
        onKeyPressed {
            when (key) {
                Keys.Up -> {
                    highlighted = (highlighted - 1).mod(options.size)
                }

                Keys.Down -> {
                    highlighted = (highlighted + 1).mod(options.size)
                }

                Keys.Space -> {
                    selected = if (highlighted in selected) selected - highlighted else selected + highlighted
                }

                Keys.A -> {
                    selected = if (selected.size == options.size) emptySet() else options.indices.toSet()
                }

                Keys.Enter -> {
                    confirmed = true
                    signal()
                }
            }
        }
    }
    return selected
}

/**
 * One choice in [readSelect].
 *
 * @param value What [readSelect] returns when this option is picked.
 * @param label What the list shows; defaults to [value].
 * @param description Shown dimmed next to the label.
 */
data class SelectOption(
    val value: String,
    val label: String = value,
    val description: String = "",
)

private const val MAX_VISIBLE_OPTIONS = 10

/**
 * Show a list of [options] and return the value of the one picked.
 * Typing filters the list (case-insensitive, prefix matches first), up/down moves, backspace edits the filter,
 * escape clears it, & enter picks the highlighted option.
 * With [allowCustom], whatever was typed is also offered as an option when it doesn't exactly match one.
 */
fun Session.readSelect(
    prompt: String,
    options: List<SelectOption>,
    allowCustom: Boolean = false,
): String {
    var query by liveVarOf("")
    var highlighted by liveVarOf(0)
    var picked by liveVarOf<SelectOption?>(null)

    fun filtered(): List<SelectOption> {
        val q = query.lowercase()
        val matches =
            options
                .filter { q.isEmpty() || q in it.value.lowercase() }
                .sortedBy { !it.value.lowercase().startsWith(q) }
        val custom =
            if (allowCustom && q.isNotEmpty() && options.none { it.value.equals(query, ignoreCase = true) }) {
                listOf(SelectOption(query, "$query (custom)"))
            } else {
                emptyList()
            }
        return matches + custom
    }

    val labelWidth = (options.map { it.label } + if (allowCustom) listOf("(custom)") else emptyList()).maxOfOrNull { it.length } ?: 0

    section {
        textLine(prompt)
        val choice = picked
        if (choice != null) {
            textLine("> ${choice.label}")
            return@section
        }
        textLine("(type to filter, up/down to move, enter to select)")
        text("> ")
        cyan { textLine(query) }
        val shown = filtered()
        if (shown.isEmpty()) {
            yellow { textLine("  (no matches)") }
            return@section
        }
        val start = (highlighted - MAX_VISIBLE_OPTIONS + 1).coerceAtLeast(0)
        shown.drop(start).take(MAX_VISIBLE_OPTIONS).forEachIndexed { offset, option ->
            val isHighlighted = start + offset == highlighted
            val line = "${if (isHighlighted) "> " else "  "}${option.label.padEnd(labelWidth)}"
            if (isHighlighted) cyan { text("  $line") } else text("  $line")
            if (option.description.isNotEmpty()) black(isBright = true) { text("  ${option.description}") }
            textLine()
        }
        val hidden = shown.size - start - MAX_VISIBLE_OPTIONS
        if (hidden > 0) black(isBright = true) { textLine("    ... $hidden more") }
    }.runUntilSignal {
        onKeyPressed {
            val shown = filtered()
            when (val k = key) {
                Keys.Up -> {
                    if (shown.isNotEmpty()) highlighted = (highlighted - 1).mod(shown.size)
                }

                Keys.Down -> {
                    if (shown.isNotEmpty()) highlighted = (highlighted + 1).mod(shown.size)
                }

                Keys.Backspace -> {
                    query = query.dropLast(1)
                    highlighted = 0
                }

                Keys.Escape -> {
                    query = ""
                    highlighted = 0
                }

                Keys.Enter -> {
                    shown.getOrNull(highlighted)?.let {
                        picked = it
                        signal()
                    }
                }

                is CharKey -> {
                    if (!k.char.isWhitespace()) {
                        query += k.char
                        highlighted = 0
                    }
                }
            }
        }
    }
    return picked!!.value
}
