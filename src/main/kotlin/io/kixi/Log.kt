@file:Suppress("unused")
@file:JvmName("Log")

package io.kixi

/**
 * Ki standard logging. In the future, this will provide options for writing to the
 * filesystem and custom loggers.
 *
 * @param args Array<out Any?>
 * @return String
 */

/** Which of [log], [warn] and [err] produced a line, for [logHandler]. */
enum class LogLevel { LOG, WARN, ERROR }

/**
 * Where [log], [warn] and [err] send their lines, so a host application can put
 * them in front of the user in its own way: RevNexus routes a warning to its
 * orange Messages pane, which is how an unverified mass reaches a researcher who
 * is not watching a console.
 *
 * Null, the default, prints to stdout as Ki always has. The handler receives the
 * message without the `WARN> ` style prefix, which the return value still carries.
 * Set it once while the application starts; a handler that throws is ignored and
 * the line is printed instead.
 */
@Volatile
var logHandler: ((LogLevel, String) -> Unit)? = null

fun log(vararg args:Any?) = out("", LogLevel.LOG, *args)

fun err(vararg args:Any?) = out("ERROR> ", LogLevel.ERROR, *args)

fun warn(vararg args:Any?) = out("WARN> ", LogLevel.WARN, *args)

private fun out(prefix:String = "", level: LogLevel = LogLevel.LOG, vararg args:Any?) : String {
    val message = when {
        args.isEmpty() -> ""
        args.size == 1 -> args[0].toString()
        else -> args.toString(", ")
    }
    val printMe = prefix + message

    val handler = logHandler
    if (handler != null) {
        try {
            handler(level, message)
            return printMe
        } catch (e: Throwable) {
            // A broken handler must not swallow the line.
        }
    }

    // Hack until we can get System.err working properly
    println(printMe)

    return printMe
}
