package ki

import ki.text.ParseException
import ki.text.countDigits
import ki.text.isKiIdentifier

/**
 * Versions have four components:
 *
 *  1. Major version: A positive integer
 *  2. Minor version: A positive integer
 *  3. Micro version: A positive integer
 *  4. Qualifier: A string such as "alpha" or "beta", allows alphanum, '_' and '-'
 *
 *  // TODO: Line this up with Maven Versions
 */
data class Version (var major: Int, var minor: Int = 0, var micro: Int = 0, var qualifier: String = "") :
    Comparable<Any?> {

    override fun toString(): String {
        val base = "$major.$minor.$micro"
        return if (qualifier.isEmpty()) base else "$base.$qualifier"
    }

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
        } else qualifier.compareTo(other.qualifier)
    }

    companion object {
        val EMPTY = Version(0)
        val MIN = Version(Int.MIN_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, "A")
        val MAX = Version(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, "Z")
        private var FORMAT_ERROR_STRING = "Use: major.minor.micro.qualifier. Only 'major' is required."

        /**
         * Create a version from a string with the format:
         *      major('.'minor('.'micro('.'qualifier)?)?)?
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

            var qualifier = ""

            var comps = version.split(delim)

            if(comps.size == 0) {
                throw ParseException("Invalid Version format. " + FORMAT_ERROR_STRING);
            } else if(comps.size > 4) {
                throw ParseException("Too many components. " + FORMAT_ERROR_STRING);
            }

            val majorText = comps[0];
            if(majorText.startsWith("-")) {
                throw ParseException("'major' component of Version cannot be negative.");
            } else if(majorText.countDigits() < majorText.length) {
                throw ParseException("Non-digit char in 'major' component of Version.");
            } else if(majorText.isEmpty()) {
                throw ParseException("'major' component of Version cannot be empty.");
            }

            major = Integer.parseInt(majorText)

            if(comps.size > 1) {
                val minorText = comps[1];
                if(minorText.startsWith("-")) {
                    throw ParseException("'minor' component of Version cannot be negative.")
                } else if(minorText.countDigits() < minorText.length) {
                    throw ParseException("Non-digit char in 'minor' component of Version.")
                } else if(minorText.isEmpty()) {
                    throw ParseException("'minor' component of Version cannot be empty.")
                }
                minor = Integer.parseInt(minorText)
            }

            if(comps.size > 2) {
                val microText = comps[2];
                if(microText.startsWith("-")) {
                    throw ParseException("'micro' component of Version cannot be negative.")
                } else if(microText.countDigits() < microText.length) {
                    throw ParseException("Non-digit char in 'micro' component of Version.")
                } else if(microText.isEmpty()) {
                    throw ParseException("'micro' component of Version cannot be empty.")
                }
                micro = Integer.parseInt(microText)
            }

            if(comps.size > 3) {
                val qualifierText = comps[3];
                if(qualifierText.isEmpty()) {
                    throw ParseException("'qualifier' component of Version cannot be empty.")
                } else if(!qualifierText.isKiIdentifier()) {
                    throw ParseException("'qualifier' component is not a valid KiID.")
                }
                qualifier = qualifierText
            }

            return Version(major, minor, micro, qualifier)
        }
    }
}
