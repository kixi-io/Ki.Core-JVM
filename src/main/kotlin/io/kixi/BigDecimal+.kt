package io.kixi

import java.math.BigDecimal

val BigDecimal.isWhole: Boolean
    get() =
        this.signum() == 0 || this.scale() <= 0 || this.stripTrailingZeros().scale() <= 0
