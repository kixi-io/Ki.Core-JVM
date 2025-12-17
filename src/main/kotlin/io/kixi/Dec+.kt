package io.kixi

import java.math.BigDecimal as Dec

/**
 * Returns true if this BigDecimal represents a whole number (has no fractional component).
 *
 * A number is considered whole if its value is zero, its scale is non-positive,
 * or after stripping trailing zeros its scale is non-positive.
 */
val Dec.whole: Boolean
    get() =
        this.signum() == 0 || this.scale() <= 0 || this.stripTrailingZeros().scale() <= 0