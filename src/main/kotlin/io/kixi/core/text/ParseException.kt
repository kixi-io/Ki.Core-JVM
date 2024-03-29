package io.kixi.core.text

/**
 * A ParseException represents an problem encountered while parsing text.
 *
 * @property line Int The line on which the error occurred. If the text is a single line
 *     no line number is displayed.
 * @property index Int The index within the line. In the Ki libraries, index is the
 *      column within a line. Position is used to indicate the distance from the
 *      beginning of the text (across lines.)
 * @property message String?
 */
@Suppress("unused")
open class ParseException : RuntimeException {

    var line = -1
    var index = -1

    /**
     * Used for multiline text parsing. Lines should be incremented for every newline
     * (\n).
     *
     * @param message String error message
     * @param line Int
     * @param index Int Index within the line
     * @param cause Throwable?
     * @constructor
     */
    @JvmOverloads
    constructor(message:String, line:Int = -1, index:Int = -1, cause:Throwable? = null) : super(message, cause) {
        this.line = line
        this.index = index
    }

    companion object {

        /**
         * Used for parsing one line strings. The line number is disregarded.
         *
         * @param message String
         * @param index Int Index at which the error occurs
         * @param cause Throwable?
         * @constructor
         */
        @JvmOverloads
        fun line(message:String, index:Int = -1, cause:Throwable? = null): ParseException {
            return ParseException(message, -1, index, cause)
        }
    }

    override val message: String get() {
        var msg : String = if(super.message.isNullOrEmpty()) this::class.simpleName!!
            else "${this::class.simpleName} \"${super.message}\""

        if(line!=-1) msg+= " line: $line"
        if(index!=-1) msg+= " index: $index"
        if(cause!=null) msg+= " cause: ${super.cause!!.message}"

        return msg
    }

    override fun toString() : String = message
}
