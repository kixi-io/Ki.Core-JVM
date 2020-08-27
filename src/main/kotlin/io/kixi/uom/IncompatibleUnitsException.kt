package io.kixi.uom

import java.lang.RuntimeException

class IncompatibleUnitsException(from:Unit, to:Unit) :
    RuntimeException("Can't convert from ${from.type} to ${to.type}")