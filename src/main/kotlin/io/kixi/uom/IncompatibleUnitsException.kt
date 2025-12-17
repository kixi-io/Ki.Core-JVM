package io.kixi.uom

import java.lang.RuntimeException

/**
 * Thrown when attempting to convert or compare units of different dimensions.
 *
 * For example, converting from meters (Length) to kilograms (Mass) is not
 * possible and will throw this exception.
 *
 * @param from The source unit
 * @param to The target unit that is incompatible with the source
 */
class IncompatibleUnitsException(from: Unit, to: Unit) :
    RuntimeException("Can't convert from ${from::class.java.simpleName} to ${to::class.java.simpleName}")
