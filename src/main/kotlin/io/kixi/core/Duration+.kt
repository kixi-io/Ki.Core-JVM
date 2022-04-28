package io.kixi.core

import java.time.Duration

fun Duration.kiFormat(zeroPad:Boolean = false): String {
    return Ki.formatDuration(this, zeroPad)
}
