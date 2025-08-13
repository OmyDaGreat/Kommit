package xyz.malefic.cli.cmd

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.fprintf
import platform.posix.stderr

/**
 * Windows-specific implementation for output with proper stderr handling
 */
@OptIn(ExperimentalForeignApi::class)
actual fun writeOutput(message: String, err: Boolean) {
    if (err) {
        fprintf(stderr, "%s\n", message)
    } else {
        println(message)
    }
}