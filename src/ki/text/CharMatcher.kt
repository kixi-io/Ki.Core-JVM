package ki.text;

import ki.log

class CharMatcher : Matcher {

    var chars = ""

    constructor(chars:String) { this.chars = chars }

    /**
     * @return The index of the end of the match or -1 if there is no match.
     */
    override fun match(text: CharSequence, start: Int): Int {
        // log("start $start")

        var position = start.apply {}

        while(position < text.size) {
            var c = text[position]
            if(!chars.contains(c)) {
                break
            }
            position++
        }

        return if(position==0) -1 else position
    }
}
/*
fun main() {
    log(CharMatcher("abcd").match("!bcd"))
}
 */

