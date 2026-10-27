package xyz.malefic.cli.cmd.util

import com.varabyte.kotter.foundation.collections.liveListOf
import com.varabyte.kotter.foundation.input.Completions
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.red
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
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
