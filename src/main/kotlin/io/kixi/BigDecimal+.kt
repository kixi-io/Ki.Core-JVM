package io.kixi

import java.math.BigDecimal

/**
 * Does this represent a whole number?
 */
val BigDecimal.whole: Boolean
    get() =
        this.signum() == 0 || this.scale() <= 0 || this.stripTrailingZeros().scale() <= 0
