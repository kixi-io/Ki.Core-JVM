package ki.text

interface Matcher {

    /**
     * @return The index of the end of the match or -1 if there is no match.
     */
    fun match(text:CharSequence, start:Int = 0) : Int
}