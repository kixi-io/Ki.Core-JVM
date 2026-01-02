package io.kixi.uom

import io.kixi.KiException

/**
 * Thrown when attempting to look up a unit by symbol that does not exist
 * in the unit registry.
 *
 * @param symbol The unrecognized unit symbol
 * @param suggestion Optional suggestion to help resolve the error
 */
class NoSuchUnitException @JvmOverloads constructor(
    symbol: String,
    suggestion: String? = "Check the unit symbol spelling. Common units include: " +
            "m, cm, mm, km (length), g, kg (mass), s, min, h (time), " +
            "°C, K (temperature), USD, EUR, JPY (currency)."
) : KiException("Unit for symbol $symbol is not recognized.", suggestion)