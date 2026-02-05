package io.kixi

/**
 * Base error class for all Ki-related errors.
 *
 * Parallels [KiException] for the `*Error`-named hierarchy, keeping the JVM naming
 * convention clean: recoverable parsing/processing conditions extend [KiException],
 * while runtime violations extend [KiError]. Both provide the [suggestion] property
 * for actionable fix hints.
 *
 * Despite the name, [KiError] extends [RuntimeException] (not [java.lang.Error])
 * because KS errors are catchable — the REPL continues after them, `try`/`catch`
 * in user code can handle them, etc. The `Error` naming follows KS's own taxonomy
 * (TypeError, RuntimeError, ConstraintError, …) rather than JVM's
 * unrecoverable-vs-recoverable distinction.
 *
 * ## Hierarchy
 *
 * ```
 * RuntimeException (JVM)
 * ├── KiException          (recoverable conditions: parse failures, etc.)
 * │   └── ParseException
 * │       └── ParseError
 * └── KiError              (runtime violations: type errors, constraint errors, etc.)
 *     └── RuntimeError
 *         ├── TypeError
 *         ├── ConstraintError
 *         └── ...
 * ```
 *
 * @property suggestion Optional suggestion text to help resolve the error.
 *     If provided, it will be appended to the message with "Suggestion: " prefix.
 * @param message The error message
 * @param cause The underlying cause of this error
 */
open class KiError @JvmOverloads constructor(
    message: String,
    val suggestion: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    /**
     * Returns the full message including the suggestion if present.
     */
    override val message: String
        get() {
            val baseMessage = super.message ?: this::class.simpleName ?: "KiError"
            return if (suggestion != null) {
                "$baseMessage Suggestion: $suggestion"
            } else {
                baseMessage
            }
        }

    override fun toString(): String = message
}