@file:Suppress("unused")

package io.kixi.uom

import java.math.BigDecimal as Dec

/**
 * A currency unit representing a monetary denomination.
 *
 * Unlike physical units (length, mass, etc.), currencies are **not inter-convertible**
 * within this system. Each currency is its own base unit. Attempting to convert
 * between different currencies (e.g., USD to EUR) will throw [IncompatibleUnitsException].
 *
 * This design decision reflects that exchange rates are external, volatile data that
 * shouldn't be embedded in a type system. Same-currency arithmetic works normally:
 *
 * ```kotlin
 * val a = Quantity(100, Currency.USD)
 * val b = Quantity(50, Currency.USD)
 * val sum = a + b  // 150USD ✔
 *
 * val c = Quantity(100, Currency.EUR)
 * val mixed = a + c  // throws IncompatibleUnitsException ✗
 * ```
 *
 * ## Supported Currencies
 *
 * ### Fiat Currencies (Top 12 by usage)
 * - USD (US Dollar) - prefix: $
 * - EUR (Euro) - prefix: €
 * - JPY (Japanese Yen) - prefix: ¥
 * - GBP (British Pound) - prefix: £
 * - CNY (Chinese Yuan)
 * - AUD (Australian Dollar)
 * - CAD (Canadian Dollar)
 * - CHF (Swiss Franc)
 * - HKD (Hong Kong Dollar)
 * - SGD (Singapore Dollar)
 * - INR (Indian Rupee)
 * - KRW (South Korean Won)
 *
 * ### Cryptocurrencies
 * - BTC (Bitcoin) - prefix: ₿
 * - ETH (Ether) - prefix: Ξ
 *
 * ## Syntax
 *
 * Currencies support both suffix notation (standard for quantities) and prefix
 * notation (natural for money):
 *
 * ```
 * // Suffix notation (like other quantities)
 * 100USD
 * 50.25EUR
 * 0.00045BTC
 *
 * // Prefix notation (6 currencies only)
 * $100
 * €50.25
 * ¥10000
 * £75.50
 * ₿0.5
 * Ξ2.5
 * ```
 *
 * Both forms parse to identical `Quantity<Currency>` objects. The canonical
 * output form uses suffix notation for consistency with other units.
 *
 * @property symbol The three-letter ISO 4217 code (e.g., "USD", "EUR")
 * @property prefixSymbol Optional Unicode prefix symbol (e.g., '$', '€')
 * @property currencyName The full currency name for display purposes
 * @see Unit
 * @see Quantity
 */
class Currency(
    symbol: String,
    val prefixSymbol: Char? = null,
    val currencyName: String = symbol,
    unicode: String = symbol
) : Unit(symbol, Dec.ONE, Dec.ZERO, unicode) {

    /**
     * Each currency is its own base unit - no cross-currency conversion.
     */
    override val baseUnit: Unit get() = this

    /**
     * Currencies can only convert to themselves.
     *
     * @throws IncompatibleUnitsException if target is a different currency
     */
    override fun factorTo(target: Unit): Dec {
        if (this != target) {
            throw IncompatibleUnitsException(this, target)
        }
        return Dec.ONE
    }

    /**
     * Currencies can only convert values to themselves.
     *
     * @throws IncompatibleUnitsException if target is a different currency
     */
    override fun convertValue(value: Dec, target: Unit): Dec {
        if (this != target) {
            throw IncompatibleUnitsException(this, target)
        }
        return value
    }

    /**
     * Returns true if this currency has a prefix symbol for shorthand notation.
     */
    val hasPrefixSymbol: Boolean get() = prefixSymbol != null

    /**
     * Returns true if this is a cryptocurrency (BTC or ETH).
     */
    val isCrypto: Boolean get() = symbol == "BTC" || symbol == "ETH"

    /**
     * Returns true if this is a fiat currency.
     */
    val isFiat: Boolean get() = !isCrypto

    override fun toString(): String = symbol

    companion object {
        /**
         * Map of prefix symbols to their corresponding currencies.
         * Used for parsing prefix notation like $100, €50, etc.
         *
         * This map is populated lazily in [ensurePrefixesInitialized] to avoid
         * circular initialization issues with the [Unit] companion object.
         */
        private val PREFIX_MAP = mutableMapOf<Char, Currency>()

        /**
         * Currency prefix parsing depends on currencies being registered in [Unit].
         * In some scenarios (e.g., parsing prefix literals before anything has referenced
         * [Unit]), the [Unit] companion may not have been initialized yet, leaving this
         * map empty.
         *
         * To make prefix parsing reliable regardless of init order, we lazily force
         * initialization of the built-in currencies on first lookup.
         */
        @Volatile
        private var PREFIXES_INITIALIZED: Boolean = false

        private fun ensurePrefixesInitialized() {
            if (PREFIXES_INITIALIZED) return
            synchronized(PREFIX_MAP) {
                if (PREFIXES_INITIALIZED) return
                try {
                    // Touch standard currencies to trigger Unit companion init,
                    // then register their prefixes. This order is important:
                    // we must NOT call registerPrefix from Unit's addCurrency
                    // to avoid circular initialization issues.
                    val currenciesWithPrefixes = listOf(
                        Unit.USD,
                        Unit.EUR,
                        Unit.JPY,
                        Unit.GBP,
                        Unit.BTC,
                        Unit.ETH
                    )

                    // Now that Unit is initialized, populate PREFIX_MAP
                    for (currency in currenciesWithPrefixes) {
                        currency.prefixSymbol?.let { PREFIX_MAP[it] = currency }
                    }
                } finally {
                    PREFIXES_INITIALIZED = true
                }
            }
        }

        /**
         * Gets a currency by its prefix symbol.
         *
         * @param prefix The prefix character (e.g., '$', '€')
         * @return The corresponding Currency, or null if not found
         */
        @JvmStatic
        fun fromPrefix(prefix: Char): Currency? {
            ensurePrefixesInitialized()
            return PREFIX_MAP[prefix]
        }

        /**
         * Checks if a character is a known currency prefix symbol.
         */
        @JvmStatic
        fun isPrefixSymbol(ch: Char): Boolean {
            ensurePrefixesInitialized()
            return PREFIX_MAP.containsKey(ch)
        }

        /**
         * Returns all registered prefix symbols.
         */
        @JvmStatic
        fun allPrefixSymbols(): Set<Char> {
            ensurePrefixesInitialized()
            return PREFIX_MAP.keys.toSet()
        }

        // Symbol constants for convenience
        /** US Dollar prefix symbol */
        const val DOLLAR = '$'

        /** Euro prefix symbol */
        const val EURO = '€'

        /** Japanese Yen prefix symbol */
        const val YEN = '¥'

        /** British Pound prefix symbol */
        const val POUND = '£'

        /** Bitcoin prefix symbol */
        const val BITCOIN = '₿'

        /** Ether prefix symbol */
        const val ETHER = 'Ξ'
    }
}