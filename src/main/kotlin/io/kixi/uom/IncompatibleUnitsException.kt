package io.kixi.uom

import io.kixi.KiException

/**
 * Thrown when attempting to convert or compare units of different dimensions.
 *
 * For example, converting from meters (Length) to kilograms (Mass) is not
 * possible and will throw this exception.
 *
 * @param from The source unit
 * @param to The target unit that is incompatible with the source
 * @param suggestion Optional suggestion to help resolve the error
 */
class IncompatibleUnitsException @JvmOverloads constructor(
    from: Unit,
    to: Unit,
    suggestion: String? = "Only units of the same dimension can be converted. " +
            "For example, meters to kilometers (both Length), or grams to kilograms (both Mass)."
) : KiException(
    "Can't convert from ${from::class.java.simpleName} to ${to::class.java.simpleName}",
    suggestion
)