package xyz.malefic.cli.cmd.util

import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.ProcessBuilder.Redirect.PIPE

/**
 * Creates and starts a process with the specified arguments.
 * The process's output and error streams are redirected to the parent process's streams.
 *
 * @param args The command and its arguments to execute.
 * @return The started `Process` instance.
 */
fun process(vararg args: String): Process =
    ProcessBuilder(*args)
        .redirectOutput(INHERIT)
        .redirectError(INHERIT)
        .start()

/**
 * Creates and starts a process with the specified arguments.
 * The process's error stream is merged with its output stream.
 *
 * @param args The command and its arguments to execute.
 * @return The started `Process` instance.
 */
fun processStream(vararg args: String): Process =
    ProcessBuilder(*args)
        .redirectErrorStream(true)
        .start()

/**
 * Creates and starts a process with the specified arguments.
 * The process's output and error streams are redirected to pipes.
 *
 * @param args The command and its arguments to execute.
 * @return The started `Process` instance.
 */
fun processPipe(vararg args: String): Process =
    ProcessBuilder(*args)
        .redirectOutput(PIPE)
        .redirectError(PIPE)
        .start()

/**
 * Executes a Git command with the specified arguments.
 * The process's output and error streams are redirected to the parent process's streams.
 *
 * @param args The Git command and its arguments to execute.
 * @return The started `Process` instance.
 */
fun git(vararg args: String) = process("git", *args)

/**
 * Executes a Git command with the specified arguments.
 * The process's error stream is merged with its output stream.
 *
 * @param args The Git command and its arguments to execute.
 * @return The started `Process` instance.
 */
fun gitStream(vararg args: String): Process = processStream("git", *args)

/**
 * Executes a Git command with the specified arguments.
 * The process's output and error streams are redirected to pipes.
 *
 * @param args The Git command and its arguments to execute.
 * @return The started `Process` instance.
 */
fun gitPipe(vararg args: String): Process = processPipe("git", *args)
