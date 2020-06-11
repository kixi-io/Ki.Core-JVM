package ki.text

import ki.log

class MultiMatcher : Matcher {

    var matchers:Array<Matcher>

    constructor(vararg matchers:Matcher) { this.matchers = matchers as Array<Matcher> }

    /**
     * @return The index of the end of the match or -1 if there is no match.
     */
    override fun match(text: CharSequence, start: Int): Int {
        for(matcher in matchers) {
            var endIndex = matcher.match(text, start)
            if(endIndex>0)
                return endIndex;
        }
        return -1;
    }
}

fun main() {
    var multi = MultiMatcher(
        StringMatcher("hello"),
        RegexMatcher("[\\d]+"),
        CharMatcher("qwe")
    );
    log(multi.match("123456789"))
    log(multi.match("hello you"))
    log(multi.match("ewqqq"))
    log(multi.match("a123"))
}



