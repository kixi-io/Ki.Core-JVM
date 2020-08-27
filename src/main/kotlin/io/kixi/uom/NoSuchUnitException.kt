package io.kixi.uom

import java.lang.RuntimeException

class NoSuchUnitException(symbol:String) :
    RuntimeException("Unit for symbol $symbol is not recognized.")
