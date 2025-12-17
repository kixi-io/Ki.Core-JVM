package io.kixi

import java.time.Duration

/**
 * Formats this Duration as a Ki literal string.
 *
 * @param zeroPad If true, zero-pad time components to 2 digits (e.g., "01:05:03")
 * @return The Ki literal representation of this duration
 * @see Ki.formatDuration
 */
fun Duration.kiFormat(zeroPad: Boolean = false): String {
    return Ki.formatDuration(this, zeroPad)
}