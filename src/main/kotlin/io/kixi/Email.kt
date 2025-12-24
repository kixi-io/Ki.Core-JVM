package io.kixi

import io.kixi.text.ParseException

/**
 * An email address as defined by RFC 5322, with practical validation.
 *
 * ## Ki Literal Format
 * Email literals are written as plain email addresses without any wrapper:
 * ```
 * user@domain.com
 * dan@leuck.org
 * dan+spam@leuck.org
 * terada.mika@rakuten.co.jp
 * ```
 *
 * ## Structure
 * An email address consists of:
 * - **Local part**: The portion before the `@` symbol (e.g., `dan+spam`)
 * - **Domain**: The portion after the `@` symbol (e.g., `leuck.org`)
 *
 * ## Validation
 * This implementation follows RFC 5322 with practical constraints:
 *
 * **Local part** allows:
 * - Letters (a-z, A-Z)
 * - Digits (0-9)
 * - Special characters: `.` `_` `%` `+` `-`
 * - Quoted strings for special characters (e.g., `"john doe"@example.com`)
 *
 * **Domain** allows:
 * - Letters (a-z, A-Z)
 * - Digits (0-9)
 * - Hyphens (not at start or end of labels)
 * - Dots as label separators
 * - TLD must be at least 2 characters
 *
 * ## Usage
 * ```kotlin
 * // Create from string
 * val email = Email.of("dan@leuck.org")
 *
 * // Parse Ki literal
 * val email = Email.parseLiteral("terada.mika@rakuten.co.jp")
 *
 * // Access components
 * val local = email.localPart  // "terada.mika"
 * val domain = email.domain    // "rakuten.co.jp"
 *
 * // Format as Ki literal
 * println(email)  // terada.mika@rakuten.co.jp
 * ```
 *
 * @property address The full email address string
 *
 * @see Ki.parse
 * @see Ki.format
 */
class Email private constructor(
    val address: String
) : Comparable<Email> {

    /**
     * The local part of the email (before the @).
     */
    val localPart: String = address.substringBefore('@')

    /**
     * The domain part of the email (after the @).
     */
    val domain: String = address.substringAfter('@')

    /**
     * The top-level domain (e.g., "com", "org", "co.jp").
     * For multi-part TLDs like "co.jp", returns the last segment only ("jp").
     */
    val tld: String = domain.substringAfterLast('.')

    /**
     * Returns true if this email uses a plus-addressed tag (e.g., user+tag@domain.com).
     */
    val hasTag: Boolean = localPart.contains('+')

    /**
     * Returns the tag portion if plus-addressing is used, or null otherwise.
     * For "dan+spam@leuck.org", returns "spam".
     */
    val tag: String? = if (hasTag) localPart.substringAfter('+') else null

    /**
     * Returns the base local part without any plus-address tag.
     * For "dan+spam@leuck.org", returns "dan".
     */
    val baseLocalPart: String = localPart.substringBefore('+')

    /**
     * Returns a normalized version of the email with the tag removed.
     * For "dan+spam@leuck.org", returns Email("dan@leuck.org").
     */
    fun withoutTag(): Email = if (hasTag) Email("$baseLocalPart@$domain") else this

    /**
     * Returns a new Email with the specified tag added or replaced.
     * ```kotlin
     * Email.of("dan@leuck.org").withTag("newsletter")  // dan+newsletter@leuck.org
     * ```
     */
    fun withTag(newTag: String): Email = Email("$baseLocalPart+$newTag@$domain")

    /**
     * Returns the Ki literal representation (the email address itself).
     */
    override fun toString(): String = address

    /**
     * Emails are equal if their addresses match (case-insensitive for domain,
     * case-sensitive for local part per RFC 5321).
     *
     * Note: While RFC 5321 specifies that the local part is case-sensitive,
     * this implementation performs exact string comparison for simplicity.
     * In practice, most mail systems treat the local part as case-insensitive.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Email) return false
        return address == other.address
    }

    override fun hashCode(): Int = address.hashCode()

    /**
     * Compares emails lexicographically by address.
     */
    override fun compareTo(other: Email): Int = address.compareTo(other.address)

    /**
     * Compares emails with case-insensitive domain comparison.
     * Local parts are compared case-sensitively per RFC 5321.
     */
    fun equalsIgnoreDomainCase(other: Email): Boolean =
        localPart == other.localPart && domain.equals(other.domain, ignoreCase = true)

    companion object : Parseable<Email> {

        /**
         * Regex for validating the local part of an email.
         *
         * Allows:
         * - Alphanumeric characters
         * - Dots (not at start/end, not consecutive)
         * - Special characters: _ % + -
         * - Or a quoted string for special characters
         */
        private val LOCAL_PART_REGEX = Regex(
            """^(?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")$"""
        )

        /**
         * Simplified local part regex for common email patterns.
         */
        private val SIMPLE_LOCAL_REGEX = Regex("""^[a-zA-Z0-9._%+-]+$""")

        /**
         * Regex for validating the domain part of an email.
         *
         * Allows:
         * - Alphanumeric characters
         * - Hyphens (not at start/end of labels)
         * - Dots as label separators
         * - TLD must be at least 2 characters (letters only)
         */
        private val DOMAIN_REGEX = Regex(
            """^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}$"""
        )

        /**
         * Maximum length for an email address per RFC 5321.
         */
        private const val MAX_EMAIL_LENGTH = 254

        /**
         * Maximum length for the local part per RFC 5321.
         */
        private const val MAX_LOCAL_LENGTH = 64

        /**
         * Maximum length for the domain part per RFC 5321.
         */
        private const val MAX_DOMAIN_LENGTH = 255

        /**
         * Creates an Email from a string, validating the format.
         *
         * @param address The email address string
         * @return The validated Email
         * @throws IllegalArgumentException if the email format is invalid
         */
        @JvmStatic
        fun of(address: String): Email {
            val trimmed = address.trim()
            validate(trimmed)
            return Email(trimmed)
        }

        /**
         * Creates an Email from local part and domain components.
         *
         * @param localPart The local part (before @)
         * @param domain The domain part (after @)
         * @return The validated Email
         * @throws IllegalArgumentException if the resulting email format is invalid
         */
        @JvmStatic
        fun of(localPart: String, domain: String): Email {
            val address = "${localPart.trim()}@${domain.trim()}"
            validate(address)
            return Email(address)
        }

        /**
         * Creates an Email if valid, or returns null if invalid.
         *
         * @param address The email address string
         * @return The validated Email, or null if invalid
         */
        @JvmStatic
        fun ofOrNull(address: String): Email? = try {
            of(address)
        } catch (e: Exception) {
            null
        }

        /**
         * Validates an email address string.
         *
         * @param address The email address to validate
         * @throws IllegalArgumentException if the email is invalid
         */
        @JvmStatic
        fun validate(address: String) {
            if (address.isEmpty()) {
                throw IllegalArgumentException("Email address cannot be empty")
            }

            if (address.length > MAX_EMAIL_LENGTH) {
                throw IllegalArgumentException(
                    "Email address exceeds maximum length of $MAX_EMAIL_LENGTH characters"
                )
            }

            val atIndex = address.indexOf('@')
            if (atIndex == -1) {
                throw IllegalArgumentException("Email address must contain '@' symbol")
            }

            if (address.indexOf('@', atIndex + 1) != -1) {
                throw IllegalArgumentException("Email address cannot contain multiple '@' symbols")
            }

            val localPart = address.substring(0, atIndex)
            val domain = address.substring(atIndex + 1)

            // Validate local part
            if (localPart.isEmpty()) {
                throw IllegalArgumentException("Local part (before @) cannot be empty")
            }

            if (localPart.length > MAX_LOCAL_LENGTH) {
                throw IllegalArgumentException(
                    "Local part exceeds maximum length of $MAX_LOCAL_LENGTH characters"
                )
            }

            if (localPart.startsWith(".") || localPart.endsWith(".")) {
                throw IllegalArgumentException("Local part cannot start or end with a dot")
            }

            if (localPart.contains("..")) {
                throw IllegalArgumentException("Local part cannot contain consecutive dots")
            }

            // Check if it's a quoted local part
            val isQuoted = localPart.startsWith("\"") && localPart.endsWith("\"")
            if (!isQuoted && !SIMPLE_LOCAL_REGEX.matches(localPart)) {
                throw IllegalArgumentException(
                    "Local part contains invalid characters: $localPart"
                )
            }

            // Validate domain
            if (domain.isEmpty()) {
                throw IllegalArgumentException("Domain (after @) cannot be empty")
            }

            if (domain.length > MAX_DOMAIN_LENGTH) {
                throw IllegalArgumentException(
                    "Domain exceeds maximum length of $MAX_DOMAIN_LENGTH characters"
                )
            }

            if (domain.startsWith(".") || domain.endsWith(".")) {
                throw IllegalArgumentException("Domain cannot start or end with a dot")
            }

            if (domain.startsWith("-") || domain.endsWith("-")) {
                throw IllegalArgumentException("Domain cannot start or end with a hyphen")
            }

            if (!DOMAIN_REGEX.matches(domain)) {
                throw IllegalArgumentException("Invalid domain format: $domain")
            }

            // Check each domain label
            val labels = domain.split('.')
            for (label in labels) {
                if (label.isEmpty()) {
                    throw IllegalArgumentException("Domain contains empty label (consecutive dots)")
                }
                if (label.length > 63) {
                    throw IllegalArgumentException(
                        "Domain label '$label' exceeds maximum length of 63 characters"
                    )
                }
                if (label.startsWith("-") || label.endsWith("-")) {
                    throw IllegalArgumentException(
                        "Domain label '$label' cannot start or end with a hyphen"
                    )
                }
            }

            // TLD must be at least 2 characters and contain only letters
            val tld = labels.last()
            if (tld.length < 2) {
                throw IllegalArgumentException(
                    "Top-level domain must be at least 2 characters"
                )
            }
            if (!tld.all { it.isLetter() }) {
                throw IllegalArgumentException(
                    "Top-level domain must contain only letters: $tld"
                )
            }
        }

        /**
         * Checks if a string is a valid email address format.
         *
         * @param address The string to check
         * @return true if the string is a valid email format
         */
        @JvmStatic
        fun isValid(address: String): Boolean = try {
            validate(address)
            true
        } catch (e: IllegalArgumentException) {
            false
        }

        /**
         * Parses a Ki email literal string into an Email instance.
         *
         * The literal format is simply the email address itself:
         * ```
         * user@domain.com
         * dan+spam@leuck.org
         * ```
         *
         * @param text The Ki email literal string to parse
         * @return The parsed Email
         * @throws ParseException if the text cannot be parsed as a valid email
         */
        override fun parseLiteral(text: String): Email {
            val trimmed = text.trim()

            if (trimmed.isEmpty()) {
                throw ParseException("Email literal cannot be empty", index = 0)
            }

            try {
                return of(trimmed)
            } catch (e: IllegalArgumentException) {
                throw ParseException(
                    "Invalid email format: ${e.message}",
                    index = 0,
                    cause = e
                )
            }
        }

        /**
         * Parses an email literal, returning null on failure instead of throwing.
         *
         * @param text The email literal string
         * @return The parsed Email, or null if parsing fails
         */
        @JvmStatic
        fun parseOrNull(text: String): Email? = try {
            parseLiteral(text)
        } catch (e: Exception) {
            null
        }

        /**
         * Checks if a string appears to be an email address.
         * This is a quick structural check, not a full validation.
         *
         * @param text The string to check
         * @return true if the string looks like an email address
         */
        @JvmStatic
        fun isLiteral(text: String): Boolean {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return false

            val atIndex = trimmed.indexOf('@')
            if (atIndex <= 0 || atIndex >= trimmed.length - 1) return false

            // Quick check: no whitespace, has something before and after @
            if (trimmed.any { it.isWhitespace() }) return false

            val domain = trimmed.substring(atIndex + 1)
            return domain.contains('.') && !domain.endsWith('.')
        }
    }
}