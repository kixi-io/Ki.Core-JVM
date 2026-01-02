package io.kixi

/**
 * Base exception class for all Ki-related exceptions.
 *
 * KiException provides a standardized way to handle errors across the Ki ecosystem,
 * with optional suggestion text to help users resolve issues.
 *
 * @property suggestion Optional suggestion text to help resolve the error.
 *     If provided, it will be appended to the message with "Suggestion: " prefix.
 * @param message The error message
 * @param cause The underlying cause of this exception
 */
open class KiException @JvmOverloads constructor(
    message: String,
    val suggestion: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    /**
     * Returns the full message including the suggestion if present.
     */
    override val message: String
        get() {
            val baseMessage = super.message ?: this::class.simpleName ?: "KiException"
            return if (suggestion != null) {
                "$baseMessage Suggestion: $suggestion"
            } else {
                baseMessage
            }
        }

    override fun toString(): String = message
}