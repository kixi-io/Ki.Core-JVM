package ki.text

import ki.log
import ki.test.Test

class TextTest : Test() {

    companion object { val IS_ID = "is KiID" }

    override fun run() {
        log("  -- .isKiIdentifier() --")
        isFalse("\"\" $IS_ID", "".isKiIdentifier())
        isFalse("_ $IS_ID", "_".isKiIdentifier(), "Reserved for open ended " +
            "ranges")
        isTrue("Foo $IS_ID", "Foo".isKiIdentifier())
        isFalse("5Foo $IS_ID", "5Foo".isKiIdentifier())
        isFalse("f-oo $IS_ID", "f-oo".isKiIdentifier())
        isTrue("foo5 $IS_ID","foo5".isKiIdentifier())
        isTrue("😀 $IS_ID","😀".isKiIdentifier())
        isTrue("👽alien $IS_ID","👽alien".isKiIdentifier())
        // isTrue("👨‍👩‍👦‍👦 $IS_ID", "👨‍👩‍👦‍👦".isKiIdentifier()) // TODO: Broken. Allow non-BMP emoji.
    }
}
