package io.kixi.uom

import java.lang.RuntimeException

/**
 * Thrown when attempting to look up a unit by symbol that does not exist
 * in the unit registry.
 *
 * @param symbol The unrecognized unit symbol
 */
class NoSuchUnitException(symbol: String) :
    RuntimeException("Unit for symbol $symbol is not recognized.")
