package io.kixi.core.uom

import java.lang.RuntimeException

class IncompatibleUnitsException(from:Unit, to:Unit) :
    RuntimeException("Can't convert from ${from::class.java.simpleName} to ${to::class.java.simpleName}")
