package io.kixi.ki

import io.kixi.ki.text.ParseException
import io.kixi.ki.text.countDigits
import java.lang.IllegalArgumentException

/**
 * The Ki Version type is based on [Semantic Versioning 2](https://semver.org).
 * They use the format major('.'minor('.'micro)?)?('-'qualifier(('-')?qualiferNumber)?)?
 *
 * See {@link #compareTo(Any?)} for comparison logic.
 *
 *  **Version components**
 *  1. Major version: A positive integer
 *  2. Minor version: A positive integer
 *  3. Micro version: A positive integer
 *  4. Qualifier: A string of letters such as "alpha", "beta" or RC
 *  5. QualifierNumber: A positive integer (requires a qualifier)
 *
 *  **Examples**
 *  1. 5
 *  2. 5.2
 *  3. 5.2.7
 *  4. 5-beta
 *  5. 5.2-alpha
 *  6. 5.2.7-rc
 *  7. 5-beta-1
 *  8. 5-beta1 # same as above (dash is optional)
 *  9. 5.2-alpha-3
 *  10. 5.2.7-rc-5
 *
 *  @throws IllegalArgumentException If any numeric component is negative, or a
 *    non-zero qualifierNumber is provided without a qualifier string
 */
class Version (var major: Int, var minor: Int = 0, var micro: Int = 0, var qualifier: String = "",
               var qualifierNumber: Int = 0) : Comparable<Any?> {

    init {
        if(major<0 || minor<0 || micro<0)
            throw IllegalArgumentException("Version components can't be negative.")
        if(qualifier.isEmpty() && qualifierNumber!=0) {
            throw IllegalArgumentException("Qualifier number is only allowed when "
                + "a qualifier is provided.")
        }
    }

    override fun equals(other: Any?): Boolean = if (other==null) false else toString() == other.toString()
    override fun hashCode() : Int = toString().hashCode() or 31

    override fun toString(): String {
        var text = "$major.$minor.$micro"
        if(!qualifier.isEmpty()) {
            text+="-$qualifier"
            if(qualifierNumber!=0) {
                text+="-$qualifierNumber"
            }
        }
        return text
    }

    /**
     * Compares numeric components and qualifier, if present, ignoring case. Versions that
     * have qualifiers are sorted below versions that are otherwise equal without a
     * qualifier (e.g. 5.2-alpha is lower than 5.2).
     *
     * @param other Any?
     * @return Int
     */
    override operator fun compareTo(other: Any?): Int {
        if (other === this) return 0

        other as Version

        var result: Int = major - other.major
        if (result != 0) {
            return result
        }
        result = minor - other.minor
        if (result != 0) {
            return result
        }
        result = micro - other.micro

        return if (result != 0) {
            result
        } else {

            // Deal with our qualifier or the other qualifier being empty
            if(qualifier.isEmpty()) {
                return if (other.qualifier.isEmpty()) 0 else 1
            } else if(other.qualifier.isEmpty()) {
                return -1
            }

            result = qualifier.compareTo(other.qualifier, ignoreCase = true)

            // Compare qualifierNumber if qualifiers are equal
            return if(result==0) qualifierNumber.compareTo(other.qualifierNumber)
                else result
        }
    }

    companion object {
        val EMPTY = Version(0)

        val MIN = Version(0, 0, 0, "AAA")
        val MAX = Version(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        private var FORMAT_ERROR_STRING = "Use: major.minor.micro.qualifier. Only 'major' is required."

        /**
         * Create a version from a string with the format:
         *      major('.'minor('.'micro)?)?('-'qualifier((-)?qualifierNumber)?)?
         *
         * All number components must be positive and the qualifier chars must be
         * alphanum, '_' or '-'.
         *
         * @throws ParseException If `version` is improperly formatted.
         */
        @JvmStatic fun parse(version: String, delim:Char = '.') : Version {

            val major: Int
            var minor = 0
            var micro = 0

            val numberPortion = version
            var qualifier = ""
            var qualifierNumber = 0

            var comps = numberPortion.split(delim)

            if(comps.isEmpty()) {
                throw ParseException("Invalid Version format. $FORMAT_ERROR_STRING")
            } else if(comps.size > 3) {
                throw ParseException("Too many components. $FORMAT_ERROR_STRING")
            }

            val lastSegment = comps.last()

            val qualifierDashIndex = lastSegment.indexOf("-")
            if(qualifierDashIndex!=-1) {
                qualifier = lastSegment.substring(qualifierDashIndex+1)


                if(qualifier.isEmpty()) {
                    throw ParseException("Trailing dash. Qualifiers can't be empty.")
                } else if(qualifierDashIndex==0) {
                    throw ParseException("Version components cannot be negative.")
                }

                // Extract qualifierNumber
                val firstQualNumIndex = qualifier.indexOfFirst { it.isDigit() }
                if(firstQualNumIndex!=-1) {
                    qualifierNumber = qualifier.substring(firstQualNumIndex).toInt()
                    qualifier = qualifier.substring(0, firstQualNumIndex).removeSuffix("-")
                }

                val lastNumber = lastSegment.substring(0, qualifierDashIndex)
                comps = comps.toMutableList()
                comps[comps.size-1] = lastNumber
            }

            val majorText = comps[0]

            when {
                majorText.startsWith("-") -> throw ParseException(
                    "'major' component of Version cannot be negative."
                )
                majorText.countDigits() < majorText.length ->
                    throw ParseException("Non-digit char in 'major' component of Version.")
                majorText.isEmpty() ->
                    throw ParseException("'major' component of Version cannot be empty.")
                else -> {
                    major = Integer.parseInt(majorText)

                    if (comps.size > 1) {
                        val minorText = comps[1]
                        minor = when {
                            minorText.startsWith("-") -> throw
                            ParseException("'minor' component of Version cannot be negative.")
                            minorText.countDigits() < minorText.length -> throw
                            ParseException("Non-digit char in 'minor' component of Version.")
                            minorText.isEmpty() -> throw
                            ParseException("'minor' component of Version cannot be empty.")
                            else -> Integer.parseInt(minorText)
                        }
                    }

                    if (comps.size > 2) {
                        val microText = comps[2]
                        when {
                            microText.startsWith("-") -> throw
                            ParseException("'micro' component of Version cannot be negative.")
                            microText.countDigits() < microText.length -> throw
                            ParseException("Non-digit char in 'micro' component of Version.")
                            microText.isEmpty() -> throw
                            ParseException("'micro' component of Version cannot be empty.")
                            else -> micro = Integer.parseInt(microText)
                        }
                    }

                    return Version(major, minor, micro, qualifier, qualifierNumber)
                }
            }
        }
    }
}
