package ki

import ki.text.ParseException
import ki.text.countDigits
import ki.text.isKiIdentifier
import ki.text.size

/**
 * Versions have four components:
 *
 *  1. Major version: A positive integer
 *  2. Minor version: A positive integer
 *  3. Micro version: A positive integer
 *  4. Qualifier: A string such as "alpha" or "beta", allows alphanum, '_' and '-'
 */
class Version : Comparable<Any?> {

    var major = 0
    var minor = 0
    var micro = 0

    var qualifier = ""

    constructor(major: Int = 0, minor: Int = 0, micro: Int = 0, qualifier: String = "") {
        this.major = major
        this.minor = minor
        this.micro = micro
        this.qualifier = qualifier
    }

    override fun toString(): String {
        val base = major.toString() + '.' + minor + '.'+ micro
        return if (qualifier.length == 0) base else base + '.' + qualifier
    }

    override fun hashCode(): Int = ((major shl 24) + (minor shl 16) + (micro shl 8) + qualifier.hashCode())

    override fun equals(other: Any?): Boolean {
        if (other === this) { // quicktest
            return true
        }
        if(other == null) return false;

        if (other !is Version) {
            return false
        }

        return (major == other.major && minor == other.minor && micro == other.micro && qualifier == other.qualifier)
    }

    override operator fun compareTo(other: Any?): Int {
        if (other === this) { // quicktest
            return 0
        }

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
        var FORMAT_ERROR_STRING = "Use: major.minor.micro.qualifier. Only 'major' is required."

        /**
         * Create a version from a string with the format: major('.'minor('.'micro('.'qualifier)?)?)?
         *
         * All number components must be positive and the qualifier chars must be alphanum, '_' or '-'.
         *
         * @throws ParseException If `version` is improperly formatted.
         */
        fun parse(version: String, delim:Char = '.') : Version {

            var major: Int
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
            } else if(majorText.countDigits() < majorText.size) {
                throw ParseException("Non-digit char in 'major' component of Version.");
            } else if(majorText.isEmpty()) {
                throw ParseException("'major' component of Version cannot be empty.");
            }

            major = Integer.parseInt(majorText)

            if(comps.size > 1) {
                val minorText = comps[1];
                if(minorText.startsWith("-")) {
                    throw ParseException("'minor' component of Version cannot be negative.");
                } else if(minorText.countDigits() < minorText.size) {
                    throw ParseException("Non-digit char in 'minor' component of Version.");
                } else if(minorText.isEmpty()) {
                    throw ParseException("'minor' component of Version cannot be empty.");
                }
                minor = Integer.parseInt(minorText)
            }

            if(comps.size > 2) {
                val microText = comps[2];
                if(microText.startsWith("-")) {
                    throw ParseException("'micro' component of Version cannot be negative.");
                } else if(microText.countDigits() < microText.size) {
                    throw ParseException("Non-digit char in 'micro' component of Version.");
                } else if(microText.isEmpty()) {
                    throw ParseException("'micro' component of Version cannot be empty.");
                }
                micro = Integer.parseInt(microText)
            }

            if(comps.size > 3) {
                val qualifierText = comps[3];
                if(qualifierText.isEmpty()) {
                    throw ParseException("'qualifier' component of Version cannot be empty.");
                } else if(!qualifierText.isKiIdentifier(allowDash = true)) {
                    throw ParseException("'qualifier' component is not a valid KiID. Format: [alpha|_][alphanum|_|-]*");
                }
                qualifier = qualifierText
            }

            return Version(major,minor,micro,qualifier)
        }
    }
}

/*
fun main() {
    log(Version(2,0,1,"beta"))
    log(Version(2))
    log(Version.parse("5.2.3.alpha"))
    log(Version.parse("5"))
    try {
        log(Version.parse("5.2a"))
    } catch(pe:ParseException) {
        log(pe.message)
    }
}
 */