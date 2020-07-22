package ki.test

import ki.log
import ki.err

open class Test {

    var testCount = 0
    var errors = 0

    fun eq(test:String, o1:Any?, o2:Any?, note:String = "") {
        if (o1 == o2) success(test, "== $o2", note) else fail(test, "$o1 != $o2 (expecting ==)", note)
    }

    fun notEq(test:String, o1:Any?, o2:Any?, note:String = "") {
        if (o1 != o2) success(test, "$o1 != $o2", note) else fail(test, "== $o2 (expecting !=)", note)
    }

    fun isTrue(test:String, bool:Boolean, note:String = "") {
        if(bool) success(test, "$bool", note) else fail(test, "$bool (expecting true)", note)
    }

    fun isFalse(test:String, bool:Boolean, note:String = "") {
        if(!bool) success(test, "$bool", note) else fail(test, "$bool (expecting false)\"", note)
    }

    fun banner(text:String) = log("  -- $text --")

    private fun success(test:String, output:String, note:String = "") {
        var message = if(test.isEmpty()) output else "$test: $output"
        log("  Success: $message" + if(note.isEmpty()) "" else " # $note")
        testCount++
    }

    private fun fail(test:String, output:String, note:String = "") {
        var message = if(test.isEmpty()) output else "$test: $output"
        err("  Fail: $message" + if(note.isEmpty()) "" else " # $note")
        testCount++
        errors++
    }

    open fun run() {
    }
}

fun main() {
    val tests = Reflect.getTests("ki.text")

    var totalErrors = 0;
    var totalTests = 0;

    for(test in tests) {
        log("==== ${test::class.qualifiedName} ====")
        test.run()
        totalTests+=test.testCount
        if(test.errors > 0) {
            err("  ${test.errors} " + if(test.errors == 1) "error" else "errors")
            totalErrors+=test.errors
        } else {
            log("  Success! No errors.")
        }
    }

    log("========")
    log("Summary:")
    log("  ${tests.size} " + if(tests.size==1) "class tested" else "classes tested")
    log("  $totalTests " + if(totalTests==1) "test" else "tests")

    if(totalErrors>0) {
        err("  Total: $totalErrors " + if(totalErrors == 1) "error" else "errors")
    } else {
        log("  All tests succeeded!")
    }
}
