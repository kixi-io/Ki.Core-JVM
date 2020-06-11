package ki.text;

import ki.log

class RegexMatcher : Matcher {

    var regex = Regex(".*")

    /**
     * Creates a regex with "^" + pattern to match the beginning of a string or the portion from "start" if provided.
     */
    constructor(pattern:String) { regex = Regex("^" + pattern) }

    /**
     * @return The index of the end of the match or -1 if there is no match.
     */
    override fun match(text: CharSequence, start: Int): Int {
        var match = regex.find(text, start);

        if(match == null)
            return -1

        return start + match.range.count();
    }
}
