package ki.text

import ki.log

class StringMatcher : Matcher {

    var string = ""

    constructor(string:String) { this.string = string }

    /**
     * @return The index of the end of the match or -1 if there is no match.
     */
    override fun match(text: CharSequence, start: Int): Int {
        return if(text.startsWith(string, start)) start + string.size else -1
    }
}
/*
fun main() {
    log(StringMatcher("abc").match("abc123"))
}
*/


