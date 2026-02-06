package io.kixi.text

import io.kixi.KiException

/**
 * A ParseException represents a problem encountered while parsing text.
 *
 * @property line Int The line on which the error occurred. If the text is a single line
 *     no line number is displayed.
 * @property index Int The index within the line. In the Ki libraries, index is the
 *      column within a line. Position is used to indicate the distance from the
 *      beginning of the text (across lines.)
 * @property message String?
 */
@Suppress("unused")
open class ParseException
/**
 * Used for multiline text parsing. Lines should be incremented for every newline
 * (\n).
 *
 * @param message String error message
 * @param line Int
 * @param index Int Index within the line
 * @param cause Throwable?
 * @param suggestion String? Optional suggestion to help resolve the error
 * @constructor
 */ @JvmOverloads constructor(
    message: String,
    private var line: Int = -1,
    private var index: Int = -1,
    cause: Throwable? = null,
    suggestion: String? = null,
) : KiException(message, suggestion, cause) {

    /**
     * The raw error message as provided to the constructor, stored separately
     * to avoid the double-suggestion bug.
     *
     * [KiException.message] appends "Suggestion: ..." to its output. If this
     * class's [message] override called `super.message` and then appended the
     * suggestion again, it would appear twice. By storing the raw message here
     * and building our formatted output from it directly, each piece of
     * information appears exactly once.
     */
    private val rawMessage: String = message

    companion object {

        /**
         * Used for parsing one line strings. The line number is disregarded.
         *
         * @param message String
         * @param index Int Index at which the error occurs
         * @param cause Throwable?
         * @param suggestion String? Optional suggestion to help resolve the error
         * @constructor
         */
        @JvmOverloads
        fun line(
            message: String,
            index: Int = -1,
            cause: Throwable? = null,
            suggestion: String? = null
        ): ParseException {
            return ParseException(message, -1, index, cause, suggestion)
        }
    }

    override val message: String get() {
        var msg: String = if (rawMessage.isEmpty()) this::class.simpleName!!
        else "${this::class.simpleName} \"$rawMessage\""

        if (line != -1) msg += " line: $line"
        if (index != -1) msg += " index: $index"
        if (cause != null) msg += " cause: ${cause!!.message}"
        if (suggestion != null) msg += " Suggestion: $suggestion"

        return msg
    }

    override fun toString(): String = message
}