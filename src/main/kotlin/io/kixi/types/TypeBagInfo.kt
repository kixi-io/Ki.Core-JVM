package io.kixi.types

/**
 * A bag as [TypeBags.describe] reports it: whether and how it was loaded, and its
 * names (types, the literals that write their values, functions, constants), read
 * from the bag when it is active and from its manifest when it is not.
 */
class TypeBagInfo(
    val name: String,
    val status: Status,
    val requires: List<String>,
    val types: List<String>,
    val literals: List<String>,
    val functions: List<String>,
    val constants: List<String>,
) {
    /** How a bag came to be loaded, or that it is not loaded yet (type bags design, 13). */
    enum class Status {
        /** Discovered and loaded at startup: its manifest says preload=true. */
        PRELOADED,
        /** Discovered and loaded the first time something referred to it (or a bag that requires it). */
        LOADED_ON_REFERENCE,
        /** Added by the host with [TypeBags.register]. */
        REGISTERED,
        /** Discovered, not loaded yet: it loads when something first refers to it. */
        AVAILABLE;

        val loaded: Boolean get() = this != AVAILABLE
    }

    override fun toString(): String = "$name ($status)"
}
