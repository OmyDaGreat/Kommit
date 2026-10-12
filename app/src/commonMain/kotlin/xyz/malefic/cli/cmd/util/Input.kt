package xyz.malefic.cli.cmd.util

import com.varabyte.kotter.foundation.input.Completions
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.liveVarOf
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
